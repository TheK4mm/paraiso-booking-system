package com.hotel.paraiso.security;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String username,
        String email,
        String nombreCompleto,
        Rol rol,
        Boolean activo,
        LocalDateTime creadoEn
) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(),
                u.getNombreCompleto(), u.getRol(), u.getActivo(), u.getCreadoEn());
    }
}
