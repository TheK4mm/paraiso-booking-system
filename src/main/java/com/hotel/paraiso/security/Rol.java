package com.hotel.paraiso.security;

/**
 * Roles fijos del sistema. Se mantienen como enum (no tabla) porque el
 * código los referencia estáticamente en reglas de autorización.
 */
public enum Rol {
    ADMIN,
    RECEPCIONISTA,
    CLIENTE;

    /**
     * Roles asignables desde la administración. Las cuentas CLIENTE nacen
     * únicamente por el registro público (garantiza el vínculo con la
     * ficha de cliente vía verificación de email).
     */
    public static Rol[] internos() {
        return new Rol[] { ADMIN, RECEPCIONISTA };
    }
}
