package com.hotel.paraiso.security;

import com.hotel.paraiso.common.web.Peticiones;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Acceso denegado a un usuario YA autenticado. Antes no había ninguno:
 * el 403 llegaba a la plantilla de error solo por la convención de Boot y
 * una petición AJAX recibía HTML donde esperaba JSON.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PortalAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException, ServletException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.warn("Acceso denegado a '{}' para '{}'", request.getRequestURI(),
                auth != null ? auth.getName() : "anónimo");

        if (Peticiones.esAjax(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(Collections.singletonMap(
                    "mensaje", "No tienes permisos para realizar esta operación.")));
            return;
        }
        // sendError y no un forward a /error: solo el despacho de error del
        // contenedor rellena los atributos jakarta.servlet.error.*, sin los
        // cuales el ErrorController de Boot no sabe qué status resolver
        // (devolvía 999/500 en vez de la plantilla error/403).
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
