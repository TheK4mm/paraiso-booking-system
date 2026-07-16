package com.hotel.paraiso.security;

import com.hotel.paraiso.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Recuperación de contraseña por token de un solo uso (30 minutos).
 * Sin SMTP configurado, el enlace se emite por el log de la aplicación;
 * en producción se sustituye por un envío de correo real.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int MINUTOS_VIGENCIA = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    /**
     * Genera y "envía" el enlace de restablecimiento. La respuesta al
     * usuario es idéntica exista o no el email (evita enumeración de cuentas).
     */
    @Transactional
    public void solicitar(String email) {
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuario -> {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            tokenRepository.save(PasswordResetToken.builder()
                    .tokenHash(sha256(token))
                    .expiraEn(LocalDateTime.now().plusMinutes(MINUTOS_VIGENCIA))
                    .usuario(usuario)
                    .build());

            // Punto de extensión SMTP: en dev el enlace se emite por log
            log.warn("Enlace de restablecimiento para '{}': http://localhost:8080/restablecer?token={}",
                    usuario.getUsername(), token);
        });
    }

    @Transactional
    public void restablecer(String token, String nuevaPassword, String confirmarPassword) {
        if (nuevaPassword == null || nuevaPassword.length() < 8) {
            throw new BadRequestException("La contraseña debe tener al menos 8 caracteres");
        }
        if (!nuevaPassword.equals(confirmarPassword)) {
            throw new BadRequestException("Las contraseñas no coinciden");
        }
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(sha256(token))
                .filter(PasswordResetToken::estaVigente)
                .orElseThrow(() -> new BadRequestException("El enlace de restablecimiento es inválido o expiró"));

        usuarioService.cambiarPassword(resetToken.getUsuario().getId(), nuevaPassword);
        resetToken.setUsadoEn(LocalDateTime.now());
        tokenRepository.save(resetToken);
    }

    private String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
