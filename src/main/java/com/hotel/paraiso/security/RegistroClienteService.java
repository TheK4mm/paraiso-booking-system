package com.hotel.paraiso.security;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.cliente.ClienteRepository;
import com.hotel.paraiso.common.audit.ActividadEvent;
import com.hotel.paraiso.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Registro público de huéspedes. La cuenta nace ACTIVA y puede iniciar
 * sesión de inmediato: la verificación del email es opcional y solo
 * desbloquea beneficios (ver {@link VerificacionEmailService}).
 *
 * <p>La ficha de {@link Cliente} se crea y vincula aquí mismo cuando el
 * email no tiene ninguna. Si el email YA tiene ficha —un huésped que
 * reservó sin cuenta— la cuenta se crea igualmente activa pero SIN
 * vincular: el historial ajeno solo se vincula tras verificar el email,
 * de modo que registrar el correo de otra persona no expone su documento
 * ni sus reservas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistroClienteService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final VerificacionEmailService verificacionEmailService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void registrar(RegistroClienteRequest request) {
        if (!request.getPassword().equals(request.getConfirmarPassword())) {
            throw new BadRequestException("Las contraseñas no coinciden");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException(
                    "El email ya está registrado. Inicia sesión o recupera tu contraseña.");
        }
        clienteRepository.findByNumeroDocumento(request.getNumeroDocumento()).ifPresent(existente -> {
            if (!existente.getEmail().equalsIgnoreCase(request.getEmail())) {
                throw new BadRequestException(
                        "Ese documento ya está asociado a otro email. Usa ese email o contacta a recepción.");
            }
        });

        Optional<Cliente> fichaDelEmail = clienteRepository.findByEmailIgnoreCase(request.getEmail());

        Usuario usuario = Usuario.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                .nombreCompleto(request.getNombre() + " " + request.getApellido())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.CLIENTE)
                .activo(true)
                .emailVerificado(false)
                // Ficha propia desde el minuto cero; la ajena espera a la verificación
                .cliente(fichaDelEmail.isEmpty() ? crearFicha(request) : null)
                .build();
        usuarioRepository.save(usuario);

        verificacionEmailService.emitirEnlace(usuario);

        eventPublisher.publishEvent(new ActividadEvent(
                "CLIENTE_REGISTRADO", "Usuario", usuario.getId(), usuario.getEmail()));
        log.info("Cliente registrado: {} (ficha vinculada: {})",
                usuario.getEmail(), usuario.getCliente() != null);
    }

    private Cliente crearFicha(RegistroClienteRequest request) {
        return clienteRepository.save(Cliente.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .tipoDocumento(request.getTipoDocumento())
                .numeroDocumento(request.getNumeroDocumento())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .activo(true)
                .build());
    }
}
