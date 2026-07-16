package com.hotel.paraiso.security;

import com.hotel.paraiso.common.web.Peticiones;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Fallo de login: el POST clásico conserva la redirección histórica a
 * /login?error; el login AJAX del modal del portal recibe 401 con un
 * mensaje para mostrar dentro del propio modal.
 */
@Component
@RequiredArgsConstructor
public class RolAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    private final AuthenticationFailureHandler porDefecto =
            new SimpleUrlAuthenticationFailureHandler("/login?error");

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        if (!Peticiones.esAjax(request)) {
            porDefecto.onAuthenticationFailure(request, response, exception);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                Collections.singletonMap("mensaje", mensajeDe(exception))));
    }

    private String mensajeDe(AuthenticationException exception) {
        if (exception instanceof DisabledException) {
            return "Tu cuenta aún no está activa: verifica tu email o contacta al hotel.";
        }
        if (exception instanceof LockedException) {
            return "Tu cuenta está bloqueada. Contacta al hotel.";
        }
        return "Usuario o contraseña incorrectos.";
    }
}
