package com.hotel.paraiso.security;

import com.hotel.paraiso.common.email.EmailSender;
import com.hotel.paraiso.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Recuperación de contraseña por token de un solo uso (30 minutos).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int MINUTOS_VIGENCIA = 30;

    private final PasswordResetTokenRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final EmailSender emailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Genera y envía el enlace de restablecimiento. La respuesta al
     * usuario es idéntica exista o no el email (evita enumeración de cuentas).
     */
    @Transactional
    public void solicitar(String email) {
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuario -> {
            String token = TokensSeguros.generar();

            tokenRepository.save(PasswordResetToken.builder()
                    .tokenHash(TokensSeguros.hash(token))
                    .expiraEn(LocalDateTime.now().plusMinutes(MINUTOS_VIGENCIA))
                    .usuario(usuario)
                    .build());

            emailSender.enviar(usuario.getEmail(), "Restablece tu contraseña — Hotel Paraíso", """
                    Hola, %s:

                    Recibimos una solicitud para restablecer tu contraseña. Abre este
                    enlace para elegir una nueva:

                    %s/restablecer?token=%s

                    El enlace caduca en %d minutos y solo puede usarse una vez. Si no
                    fuiste tú, ignora este mensaje: tu contraseña no ha cambiado.

                    Hotel Paraíso""".formatted(
                    usuario.getNombreCompleto(), baseUrl, token, MINUTOS_VIGENCIA));
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
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(TokensSeguros.hash(token))
                .filter(PasswordResetToken::estaVigente)
                .orElseThrow(() -> new BadRequestException("El enlace de restablecimiento es inválido o expiró"));

        usuarioService.cambiarPassword(resetToken.getUsuario().getId(), nuevaPassword);
        resetToken.setUsadoEn(LocalDateTime.now());
        tokenRepository.save(resetToken);
        log.info("Contraseña restablecida para {}", resetToken.getUsuario().getEmail());
    }
}
