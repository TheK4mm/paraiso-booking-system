package com.hotel.paraiso.portal.cuenta;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.cliente.ClienteRepository;
import com.hotel.paraiso.common.audit.ActividadEvent;
import com.hotel.paraiso.common.exception.BusinessException;
import com.hotel.paraiso.common.exception.ResourceNotFoundException;
import com.hotel.paraiso.reserva.Reserva;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import com.hotel.paraiso.reserva.ReservaDetalleResponse;
import com.hotel.paraiso.reserva.ReservaRepository;
import com.hotel.paraiso.reserva.ReservaResponse;
import com.hotel.paraiso.reserva.ReservaService;
import com.hotel.paraiso.security.Usuario;
import com.hotel.paraiso.security.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Portal del huésped autenticado. La propiedad de una reserva se
 * verifica SIEMPRE y su ausencia se reporta como 404 (no 403): un
 * cliente no debe poder confirmar que un código ajeno existe.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CuentaClienteService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final ReservaService reservaService;
    private final ApplicationEventPublisher eventPublisher;

    /** Ficha vinculada a la cuenta; vacío si la verificación quedó a medias. */
    public Optional<Cliente> clienteDe(String username) {
        // Fetch-join: la ficha se usa fuera de esta transacción (vistas)
        return usuarioRepository.findConClienteByUsernameIgnoreCase(username)
                .map(Usuario::getCliente);
    }

    public List<ReservaResponse> misReservas(String username) {
        return clienteDe(username)
                .map(cliente -> reservaService.findByCliente(cliente.getId()))
                .orElse(List.of());
    }

    public ReservaDetalleResponse detalle(String username, String codigo) {
        return reservaPropia(username, codigo)
                .map(reserva -> reservaService.findDetalle(reserva.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "codigo", codigo));
    }

    /**
     * El huésped solo cancela reservas PENDIENTE o CONFIRMADA: con un
     * check-in en curso la cancelación es operación de recepción
     * (implica liberar la habitación y resolver la facturación).
     */
    @Transactional
    public void cancelar(String username, String codigo) {
        Reserva reserva = reservaPropia(username, codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "codigo", codigo));
        if (reserva.getEstado() != EstadoReserva.PENDIENTE
                && reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new BusinessException(
                    "Solo puedes cancelar reservas pendientes o confirmadas. Para esta, contacta a recepción.");
        }
        reservaService.cancelar(reserva.getId());
        log.info("Reserva {} cancelada por el cliente {}", codigo, username);
    }

    @Transactional
    public void actualizarPerfil(String username, PerfilClienteRequest request) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "username", username));
        Cliente cliente = usuario.getCliente();
        if (cliente == null) {
            throw new BusinessException("Tu cuenta aún no tiene ficha de cliente vinculada. Contacta a recepción.");
        }
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setTelefono(request.getTelefono());
        cliente.setDireccion(request.getDireccion());
        cliente.setPais(request.getPais());
        clienteRepository.save(cliente);

        usuario.setNombreCompleto(request.getNombre() + " " + request.getApellido());
        usuarioRepository.save(usuario);

        eventPublisher.publishEvent(new ActividadEvent(
                "PERFIL_ACTUALIZADO", "Cliente", cliente.getId(), usuario.getEmail()));
    }

    private Optional<Reserva> reservaPropia(String username, String codigo) {
        return clienteDe(username).flatMap(cliente ->
                reservaRepository.findByCodigoReserva(codigo)
                        .filter(r -> r.getCliente().getId().equals(cliente.getId())));
    }
}
