package com.hotel.paraiso.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Redirección tras el login según el contexto:
 * 1. Una reserva a medias tiene prioridad: el huésped que inició sesión
 *    en mitad del proceso vuelve directo al paso de datos.
 * 2. Si Spring Security interceptó una URL protegida, se respeta.
 * 3. Por defecto, cada rol aterriza en su zona: CLIENTE en su cuenta,
 *    el personal en el panel.
 */
@Component
public class RolAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    /** Atributo de sesión donde el flujo público guarda la reserva en curso. */
    public static final String SESION_RESERVA_EN_CURSO = "reservaEnCurso";

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        boolean esCliente = tieneRol(authentication, "ROLE_CLIENTE");

        HttpSession session = request.getSession(false);
        if (esCliente && session != null && session.getAttribute(SESION_RESERVA_EN_CURSO) != null) {
            requestCache.removeRequest(request, response);
            redirectStrategy.sendRedirect(request, response, "/reservar/datos");
            return;
        }

        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            requestCache.removeRequest(request, response);
            redirectStrategy.sendRedirect(request, response, saved.getRedirectUrl());
            return;
        }

        redirectStrategy.sendRedirect(request, response, esCliente ? "/mi-cuenta" : "/dashboard");
    }

    private boolean tieneRol(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
