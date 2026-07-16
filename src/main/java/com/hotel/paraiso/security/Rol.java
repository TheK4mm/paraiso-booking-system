package com.hotel.paraiso.security;

/**
 * Roles fijos del sistema. Se mantienen como enum (no tabla) porque el
 * código los referencia estáticamente en reglas de autorización.
 */
public enum Rol {
    ADMIN,
    RECEPCIONISTA
}
