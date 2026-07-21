package com.hotel.paraiso.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manejo centralizado de errores para la API REST usando RFC 7807 (ProblemDetail).
 * Tiene precedencia sobre el advice MVC; solo aplica a @RestController.
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(1)
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), req);
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage(), req);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio violada", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        List<String> globales = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                errores.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                globales.add(error.getDefaultMessage());
            }
        });
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Error de validación",
                "Uno o más campos no superan las validaciones", req);
        if (!errores.isEmpty()) {
            problem.setProperty("errores", errores);
        }
        if (!globales.isEmpty()) {
            problem.setProperty("erroresGlobales", globales);
        }
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String detalle = String.format("El parámetro '%s' tiene un valor inválido: '%s'",
                ex.getName(), ex.getValue());
        return problem(HttpStatus.BAD_REQUEST, "Parámetro inválido", detalle, req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "Parámetro faltante",
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'", req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "Cuerpo de solicitud ilegible",
                "El cuerpo de la solicitud está malformado o contiene valores inválidos", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violación de integridad de datos en {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflicto de datos",
                "La operación viola una restricción de integridad (valor duplicado o referencia en uso)", req);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT, "Conflicto de concurrencia",
                "El registro fue modificado por otro usuario; recargue e intente de nuevo", req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return problem(HttpStatus.NOT_FOUND, "Ruta no encontrada",
                "No existe el recurso solicitado", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return problem(HttpStatus.FORBIDDEN, "Acceso denegado",
                "No tienes permisos para realizar esta operación", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido", ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado; contacte al administrador", req);
    }

    private ProblemDetail problem(HttpStatus status, String titulo, String detalle, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalle);
        problem.setTitle(titulo);
        problem.setInstance(URI.create(req.getRequestURI()));
        problem.setProperty("timestamp", LocalDateTime.now().toString());
        return problem;
    }
}
