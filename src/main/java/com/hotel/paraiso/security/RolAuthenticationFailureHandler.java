package com.hotel.paraiso.security;

import com.hotel.paraiso.common.web.Peticiones;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Fallo de login: el login AJAX del modal recibe 401 con un mensaje para
 * mostrar dentro del propio modal; el POST clásico (sin JavaScript) vuelve
 * al portal con el modal abierto y el error visible.
 */
@Component
@RequiredArgsConstructor
public class RolAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    private final AuthenticationFailureHandler porDefecto =
            new SimpleUrlAuthenticationFailureHandler(RutasAuth.PORTAL_LOGIN + "&error");

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
            return "Tu cuenta está desactivada. Contacta al hotel.";
        }
        return "Usuario o contraseña incorrectos.";
    }
}
