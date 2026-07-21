package com.hotel.paraiso.security;

import com.hotel.paraiso.common.web.Peticiones;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Destino tras el login según el contexto:
 * 1. Una reserva a medias tiene prioridad: el huésped que inició sesión
 *    en mitad del proceso vuelve directo al paso de datos.
 * 2. Si Spring Security interceptó una URL protegida, se respeta —
 *    siempre que el rol autenticado pueda alcanzarla (ver
 *    {@link DestinoPorRol}).
 * 3. Sin destino explícito: en el login clásico cada rol aterriza en su
 *    zona (CLIENTE en su cuenta, el personal en el panel); en el login
 *    AJAX del modal del portal se responde {"redirect": null} y el JS
 *    recarga la página actual — el usuario nunca abandona donde estaba.
 */
@Component
@RequiredArgsConstructor
public class RolAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    /** Atributo de sesión donde el flujo público guarda la reserva en curso. */
    public static final String SESION_RESERVA_EN_CURSO = "reservaEnCurso";

    private final ObjectMapper objectMapper;
    private final RequestCache requestCache;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String destino = resolverDestinoExplicito(request, response, authentication);

        if (Peticiones.esAjax(request)) {
            Map<String, String> cuerpo = Collections.singletonMap("redirect", destino);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(cuerpo));
            return;
        }

        if (destino == null) {
            destino = DestinoPorRol.porDefecto(authentication);
        }
        redirectStrategy.sendRedirect(request, response, destino);
    }

    /** Reserva en curso o URL interceptada; null si no hay destino forzoso. */
    private String resolverDestinoExplicito(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authentication) {
        HttpSession session = request.getSession(false);
        if (esCliente(authentication)
                && session != null && session.getAttribute(SESION_RESERVA_EN_CURSO) != null) {
            requestCache.removeRequest(request, response);
            return "/reservar/datos";
        }

        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved == null) {
            return null;
        }
        requestCache.removeRequest(request, response);

        // Un destino fuera de la zona del rol daría un 403 inmediatamente
        // después de un login correcto: se descarta y manda el destino por
        // defecto. Pasa sobre todo con el CLIENTE, cuyo navegador pudo
        // pedir antes una URL de back-office.
        String url = saved.getRedirectUrl();
        String path = DestinoPorRol.pathDe(url, request.getContextPath());
        return path != null && DestinoPorRol.alcanzable(authentication, path) ? url : null;
    }

    private boolean esCliente(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority()));
    }
}
