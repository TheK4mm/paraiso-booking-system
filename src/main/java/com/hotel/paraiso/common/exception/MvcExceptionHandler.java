package com.hotel.paraiso.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Manejo de errores para los controladores de vistas Thymeleaf: las
 * violaciones de reglas de negocio vuelven a la página anterior con un
 * mensaje flash; los recursos inexistentes muestran la página 404.
 */
@ControllerAdvice
@Order(2)
@Slf4j
public class MvcExceptionHandler {

    @ExceptionHandler({BusinessException.class, BadRequestException.class})
    public String handleNegocio(RuntimeException ex, HttpServletRequest request) {
        return redirectConError(request, ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String handleOptimisticLock(HttpServletRequest request) {
        return redirectConError(request,
                "El registro fue modificado por otro usuario. Recargue la página e intente de nuevo.");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.debug("Recurso no encontrado en vista {}: {}", request.getRequestURI(), ex.getMessage());
        return "error/404";
    }

    private String redirectConError(HttpServletRequest request, String mensaje) {
        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        flashMap.put("error", mensaje);
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}
