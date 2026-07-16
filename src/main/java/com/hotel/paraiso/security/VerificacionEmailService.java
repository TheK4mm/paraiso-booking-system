package com.hotel.paraiso.security;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.cliente.ClienteRepository;
import com.hotel.paraiso.common.audit.ActividadEvent;
import com.hotel.paraiso.common.exception.BadRequestException;
import com.hotel.paraiso.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * Registro público de huéspedes con verificación de email por token de
 * un solo uso (24 horas). La ficha de {@link Cliente} se crea o vincula
 * ÚNICAMENTE tras verificar: así nadie puede ver el historial de reservas
 * de un email que no controla. Sin SMTP configurado, el enlace se emite
 * por el log de la aplicación (mismo patrón que {@link PasswordResetService}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificacionEmailService {

    private static final int HORAS_VIGENCIA = 24;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TokenVerificacionEmailRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Crea la cuenta CLIENTE sin verificar (no puede iniciar sesión aún)
     * y emite el enlace de verificación.
     */
    @Transactional
    public void registrarCliente(RegistroClienteRequest request) {
        if (!request.getPassword().equals(request.getConfirmarPassword())) {
            throw new BadRequestException("Las contraseñas no coinciden");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException(
                    "El email ya está registrado. Si aún no verificaste tu cuenta, solicita un nuevo enlace.");
        }
        clienteRepository.findByNumeroDocumento(request.getNumeroDocumento()).ifPresent(existente -> {
            if (!existente.getEmail().equalsIgnoreCase(request.getEmail())) {
                throw new BadRequestException(
                        "Ese documento ya está asociado a otro email. Usa ese email o contacta a recepción.");
            }
        });

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                .nombreCompleto(request.getNombre() + " " + request.getApellido())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.CLIENTE)
                .emailVerificado(false)
                .build());

        emitirToken(usuario, request.getNombre(), request.getApellido(),
                request.getTipoDocumento(), request.getNumeroDocumento(), request.getTelefono());

        eventPublisher.publishEvent(new ActividadEvent(
                "CLIENTE_REGISTRO_SOLICITADO", "Usuario", usuario.getId(), usuario.getEmail()));
        log.info("Registro de cliente solicitado: {}", usuario.getEmail());
    }

    /**
     * Verifica el email y materializa el vínculo con la ficha de cliente:
     * si ya existe una con ese email (creada por recepción o por una
     * reserva de invitado) se vincula sin sobrescribir sus datos; si no,
     * se crea desde el payload del token.
     */
    @Transactional
    public void verificar(String token) {
        TokenVerificacionEmail tve = tokenRepository.findByTokenHash(sha256(token))
                .filter(TokenVerificacionEmail::estaVigente)
                .orElseThrow(() -> new BadRequestException("El enlace de verificación es inválido o expiró"));

        Usuario usuario = tve.getUsuario();
        usuario.setEmailVerificado(true);
        tve.setUsadoEn(LocalDateTime.now());

        Cliente cliente = clienteRepository.findByEmailIgnoreCase(usuario.getEmail())
                .orElseGet(() -> crearClienteDesde(tve, usuario.getEmail()));
        if (usuarioRepository.existsByClienteId(cliente.getId())) {
            throw new BusinessException(
                    "Esa ficha de cliente ya está vinculada a otra cuenta. Contacta a recepción.");
        }
        usuario.setCliente(cliente);
        usuarioRepository.save(usuario);

        eventPublisher.publishEvent(new ActividadEvent(
                "EMAIL_VERIFICADO", "Usuario", usuario.getId(), usuario.getEmail()));
        log.info("Email verificado y cliente vinculado: {}", usuario.getEmail());
    }

    /**
     * Reemite el enlace de verificación. La respuesta al usuario es
     * idéntica exista o no la cuenta (evita enumeración).
     */
    @Transactional
    public void reenviar(String email) {
        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(u -> u.getRol() == Rol.CLIENTE && !u.getEmailVerificado())
                .flatMap(u -> tokenRepository.findTopByUsuarioOrderByCreadoEnDesc(u))
                .ifPresent(previo -> emitirToken(previo.getUsuario(), previo.getNombre(),
                        previo.getApellido(), previo.getTipoDocumento(),
                        previo.getNumeroDocumento(), previo.getTelefono()));
    }

    private void emitirToken(Usuario usuario, String nombre, String apellido,
                             String tipoDocumento, String numeroDocumento, String telefono) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokenRepository.save(TokenVerificacionEmail.builder()
                .tokenHash(sha256(token))
                .expiraEn(LocalDateTime.now().plusHours(HORAS_VIGENCIA))
                .usuario(usuario)
                .nombre(nombre)
                .apellido(apellido)
                .tipoDocumento(tipoDocumento)
                .numeroDocumento(numeroDocumento)
                .telefono(telefono)
                .build());

        // Punto de extensión SMTP: en dev el enlace se emite por log
        log.warn("Enlace de verificación para '{}': {}/verificar-email?token={}",
                usuario.getEmail(), baseUrl, token);
    }

    private Cliente crearClienteDesde(TokenVerificacionEmail tve, String email) {
        // Carrera registro↔recepción: el documento pudo aparecer con otro
        // email entre el registro y la verificación
        clienteRepository.findByNumeroDocumento(tve.getNumeroDocumento()).ifPresent(otro -> {
            throw new BusinessException(
                    "Tu documento quedó registrado con otro email. Contacta a recepción para unificar tus datos.");
        });
        return clienteRepository.save(Cliente.builder()
                .nombre(tve.getNombre())
                .apellido(tve.getApellido())
                .tipoDocumento(tve.getTipoDocumento())
                .numeroDocumento(tve.getNumeroDocumento())
                .telefono(tve.getTelefono())
                .email(email)
                .activo(true)
                .build());
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
