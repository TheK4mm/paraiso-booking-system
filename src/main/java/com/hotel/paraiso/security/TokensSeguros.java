package com.hotel.paraiso.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens de un solo uso para los enlaces enviados por correo (verificación
 * de email y restablecimiento de contraseña). El valor en claro viaja
 * únicamente en el enlace; en la base de datos se guarda solo su hash, de
 * modo que un volcado de las tablas de tokens no permite suplantar a nadie.
 */
final class TokensSeguros {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BYTES = 32;

    private TokensSeguros() {
    }

    /** Token en claro, seguro para viajar en una URL. */
    static String generar() {
        byte[] bytes = new byte[BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal: lo único que se persiste. */
    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
