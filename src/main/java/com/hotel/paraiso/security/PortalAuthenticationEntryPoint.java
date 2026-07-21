package com.hotel.paraiso.security;

import com.hotel.paraiso.common.web.Peticiones;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Qué hacer cuando un anónimo pide algo protegido. En vez de llevarlo a
 * una página de login independiente, aterriza en el portal con el modal
 * de autenticación abierto; el {@code RequestCache} conserva su destino.
 *
 * <p>Las peticiones AJAX reciben el mismo JSON de error que
 * {@link RolAuthenticationFailureHandler}, para que el modal no tenga que
 * distinguir "credenciales inválidas" de "sesión caducada".
 */
@Component
@RequiredArgsConstructor
public class PortalAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (Peticiones.esAjax(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(Collections.singletonMap(
                    "mensaje", "Tu sesión expiró. Inicia sesión para continuar.")));
            return;
        }
        redirectStrategy.sendRedirect(request, response, RutasAuth.PORTAL_LOGIN);
    }
}
