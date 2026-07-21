package com.hotel.paraiso.security;

import org.springframework.security.core.Authentication;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A dónde puede ir cada rol una vez autenticado.
 *
 * <p>Existe para impedir que el destino guardado por el {@code RequestCache}
 * mande a un usuario a una zona que su rol no alcanza: honrarlo a ciegas
 * producía un 403 justo después de un login correcto (típicamente un
 * CLIENTE arrastrado a una URL de back-office que el navegador había
 * pedido antes de autenticarse).
 */
final class DestinoPorRol {

    private DestinoPorRol() {
    }

    /** Destino cuando no hay ninguno explícito. */
    static String porDefecto(Authentication authentication) {
        return esCliente(authentication) ? "/mi-cuenta" : "/dashboard";
    }

    /**
     * ¿El rol autenticado puede abrir esa ruta? El personal alcanza todo
     * el back-office; el CLIENTE se queda en el portal y su área privada.
     * Debe mantenerse en sintonía con la zona pública de
     * {@link SecurityConfig} si se añaden rutas de cliente.
     */
    static boolean alcanzable(Authentication authentication, String path) {
        if (!esCliente(authentication)) {
            return true;
        }
        return path.equals("/")
                || path.equals("/consulta-reserva")
                || path.equals("/mi-cuenta") || path.startsWith("/mi-cuenta/")
                || path.equals("/reservar") || path.startsWith("/reservar/");
    }

    /**
     * Ruta de una URL absoluta como la que devuelve
     * {@code SavedRequest.getRedirectUrl()}, sin el context path.
     * Devuelve null si la URL no es interpretable.
     */
    static String pathDe(String url, String contextPath) {
        try {
            String path = new URI(url).getPath();
            if (path == null) {
                return null;
            }
            if (!contextPath.isEmpty() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
            return path.isEmpty() ? "/" : path;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean esCliente(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority()));
    }
}
