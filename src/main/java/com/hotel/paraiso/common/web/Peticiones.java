package com.hotel.paraiso.common.web;

import jakarta.servlet.http.HttpServletRequest;

/** Utilidades sobre la petición HTTP en curso. */
public final class Peticiones {

    private Peticiones() {
    }

    /**
     * Una petición hecha con fetch/XHR desde nuestro propio JS (el modal de
     * autenticación la marca con la cabecera estándar X-Requested-With).
     */
    public static boolean esAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }
}
