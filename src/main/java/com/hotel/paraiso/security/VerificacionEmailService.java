package com.hotel.paraiso.security;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.cliente.ClienteRepository;
import com.hotel.paraiso.common.audit.ActividadEvent;
import com.hotel.paraiso.common.email.EmailSender;
import com.hotel.paraiso.common.exception.BadRequestException;
import com.hotel.paraiso.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Verificación del email de una cuenta CLIENTE mediante token de un solo
 * uso (24 horas). Es OPCIONAL: no condiciona el inicio de sesión ni la
 * posibilidad de reservar. Lo que desbloquea es la confianza sobre el
 * correo y, con ella, la vinculación de la ficha de cliente preexistente
 * —y su historial de reservas— cuando el registro no pudo vincularla
 * (ver {@link RegistroClienteService}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificacionEmailService {

    private static final int HORAS_VIGENCIA = 24;

    private final TokenVerificacionEmailRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final EmailSender emailSender;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.base-url}")
    private String baseUrl;

    /** Emite un token nuevo y envía el enlace de verificación. */
    @Transactional
    public void emitirEnlace(Usuario usuario) {
        String token = TokensSeguros.generar();
        tokenRepository.save(TokenVerificacionEmail.builder()
                .tokenHash(TokensSeguros.hash(token))
                .expiraEn(LocalDateTime.now().plusHours(HORAS_VIGENCIA))
                .usuario(usuario)
                .build());

        emailSender.enviar(usuario.getEmail(), "Verifica tu email — Hotel Paraíso", """
                Hola, %s:

                Confirma que este correo es tuyo para activar los beneficios de tu
                cuenta (promociones, ofertas exclusivas y la recuperación avanzada
                de acceso). Ya puedes iniciar sesión y reservar sin verificarlo.

                %s/verificar-email?token=%s

                El enlace caduca en %d horas.

                Hotel Paraíso""".formatted(
                usuario.getNombreCompleto(), baseUrl, token, HORAS_VIGENCIA));
    }

    /**
     * Marca el email como verificado y, si la cuenta aún no tiene ficha,
     * la vincula por email sin sobrescribir sus datos.
     */
    @Transactional
    public void verificar(String token) {
        TokenVerificacionEmail tve = tokenRepository.findByTokenHash(TokensSeguros.hash(token))
                .filter(TokenVerificacionEmail::estaVigente)
                .orElseThrow(() -> new BadRequestException("El enlace de verificación es inválido o expiró"));

        Usuario usuario = tve.getUsuario();
        usuario.setEmailVerificado(true);
        tve.setUsadoEn(LocalDateTime.now());

        if (usuario.getCliente() == null) {
            vincularFicha(usuario);
        }
        usuarioRepository.save(usuario);

        eventPublisher.publishEvent(new ActividadEvent(
                "EMAIL_VERIFICADO", "Usuario", usuario.getId(), usuario.getEmail()));
        log.info("Email verificado: {}", usuario.getEmail());
    }

    /**
     * Reemite el enlace. La respuesta al usuario es idéntica exista o no
     * la cuenta y esté o no verificada (evita enumeración de cuentas).
     */
    @Transactional
    public void reenviar(String email) {
        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(u -> u.getRol() == Rol.CLIENTE && !u.getEmailVerificado())
                .ifPresent(this::emitirEnlace);
    }

    private void vincularFicha(Usuario usuario) {
        Cliente cliente = clienteRepository.findByEmailIgnoreCase(usuario.getEmail())
                .orElseThrow(() -> new BusinessException(
                        "No encontramos una ficha de cliente con tu email. Contacta a recepción."));
        if (usuarioRepository.existsByClienteId(cliente.getId())) {
            throw new BusinessException(
                    "Esa ficha de cliente ya está vinculada a otra cuenta. Contacta a recepción.");
        }
        usuario.setCliente(cliente);
        log.info("Ficha de cliente {} vinculada a la cuenta {}", cliente.getId(), usuario.getEmail());
    }
}
