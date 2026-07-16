package com.hotel.paraiso.portal.reserva;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.cliente.ClienteRepository;
import com.hotel.paraiso.common.exception.BadRequestException;
import com.hotel.paraiso.common.exception.BusinessException;
import com.hotel.paraiso.common.exception.ResourceNotFoundException;
import com.hotel.paraiso.habitacion.Habitacion;
import com.hotel.paraiso.habitacion.Habitacion.EstadoHabitacion;
import com.hotel.paraiso.habitacion.HabitacionRepository;
import com.hotel.paraiso.habitacion.TipoHabitacion;
import com.hotel.paraiso.reserva.ReservaDetalleResponse;
import com.hotel.paraiso.reserva.ReservaRepository;
import com.hotel.paraiso.reserva.ReservaRequest;
import com.hotel.paraiso.reserva.ReservaService;
import com.hotel.paraiso.security.Usuario;
import com.hotel.paraiso.security.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Flujo público de reservas (huéspedes, con o sin cuenta). La creación
 * delega en {@link ReservaService#create}: hereda el lock pesimista, el
 * re-chequeo de solape, la capacidad, el precio y el código por secuencia.
 * Aquí solo vive lo propio del portal: la búsqueda de reservables, la
 * selección y el get-or-create de la ficha de cliente para invitados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReservaPublicaService {

    private static final Long SIN_EXCLUSION = -1L;

    private final HabitacionRepository habitacionRepository;
    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaService reservaService;

    public List<HabitacionCardView> buscarDisponibles(LocalDate entrada, LocalDate salida, int huespedes) {
        validarBusqueda(entrada, salida);
        long noches = ChronoUnit.DAYS.between(entrada, salida);
        return habitacionRepository.findHabitacionesReservables(entrada, salida).stream()
                .filter(h -> h.getTipoHabitacion().getCapacidadMaxima() >= huespedes)
                .map(h -> toCard(h, noches))
                .toList();
    }

    /**
     * Re-verifica la habitación elegida y congela el resumen en un
     * {@link ReservaEnCurso} (el precio final se recalcula al confirmar).
     */
    public ReservaEnCurso prepararSeleccion(Long habitacionId, LocalDate entrada,
                                            LocalDate salida, int huespedes) {
        validarBusqueda(entrada, salida);
        Habitacion habitacion = habitacionRepository.findById(habitacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Habitación", "id", habitacionId));
        if (!habitacion.getActivo()
                || habitacion.getEstado() == EstadoHabitacion.MANTENIMIENTO
                || habitacion.getEstado() == EstadoHabitacion.BLOQUEADA) {
            throw new BusinessException("La habitación ya no está disponible. Elige otra, por favor.");
        }
        TipoHabitacion tipo = habitacion.getTipoHabitacion();
        if (tipo.getCapacidadMaxima() < huespedes) {
            throw new BusinessException("La habitación no tiene capacidad para " + huespedes + " huéspedes.");
        }
        long conflictos = reservaRepository.countReservasActivasParaHabitacion(
                habitacionId, entrada, salida, SIN_EXCLUSION);
        if (conflictos > 0) {
            throw new BusinessException(
                    "La habitación acaba de ser reservada para esas fechas. Elige otra, por favor.");
        }
        long noches = ChronoUnit.DAYS.between(entrada, salida);
        BigDecimal precioNoche = tipo.getPrecioBaseNoche();
        return new ReservaEnCurso(habitacion.getId(), habitacion.getNumero(), tipo.getNombre(),
                tipo.getImagen(), entrada, salida, huespedes, noches, precioNoche,
                precioNoche.multiply(BigDecimal.valueOf(noches)));
    }

    /** Ficha de cliente vinculada a la cuenta autenticada, o null (invitado/personal). */
    public Cliente clienteAutenticado(String username) {
        if (username == null) {
            return null;
        }
        // Fetch-join: la ficha se usa fuera de esta transacción
        return usuarioRepository.findConClienteByUsernameIgnoreCase(username)
                .map(Usuario::getCliente)
                .orElse(null);
    }

    @Transactional
    public ReservaDetalleResponse confirmar(ReservaEnCurso enCurso, DatosHuespedRequest datos,
                                            String username) {
        Cliente cliente = resolverCliente(datos, username);

        ReservaRequest request = new ReservaRequest();
        request.setFechaEntrada(enCurso.fechaEntrada());
        request.setFechaSalida(enCurso.fechaSalida());
        request.setNumeroHuespedes(enCurso.huespedes());
        request.setClienteId(cliente.getId());
        request.setHabitacionIds(List.of(enCurso.habitacionId()));
        request.setObservaciones(datos.getObservaciones());

        ReservaDetalleResponse reserva = reservaService.create(request);
        log.info("Reserva pública creada: codigo={}, cliente={}, invitado={}",
                reserva.getCodigoReserva(), cliente.getEmail(), username == null);
        return reserva;
    }

    /** Consulta de invitado por código+email. Vacío tanto si el código no
     *  existe como si el email no coincide (anti-enumeración). */
    public Optional<ReservaDetalleResponse> consultar(String codigo, String email) {
        if (!StringUtils.hasText(codigo) || !StringUtils.hasText(email)) {
            return Optional.empty();
        }
        return reservaRepository.findByCodigoReserva(codigo.trim().toUpperCase())
                .filter(r -> r.getCliente().getEmail().equalsIgnoreCase(email.trim()))
                .map(r -> reservaService.findDetalle(r.getId()));
    }

    // ─── privados ───

    private Cliente resolverCliente(DatosHuespedRequest datos, String username) {
        Cliente vinculado = clienteAutenticado(username);
        if (vinculado != null) {
            // Cliente con cuenta: manda su ficha, los campos del form se ignoran
            return vinculado;
        }
        return clienteRepository.findByEmailIgnoreCase(datos.getEmail())
                .map(existente -> {
                    if (!existente.getActivo()) {
                        throw new BusinessException(
                                "No pudimos completar la reserva con ese email. Contacta a recepción, por favor.");
                    }
                    // Ficha existente (recepción o reserva previa): no se sobrescribe
                    return existente;
                })
                .orElseGet(() -> crearClienteInvitado(datos));
    }

    private Cliente crearClienteInvitado(DatosHuespedRequest datos) {
        clienteRepository.findByNumeroDocumento(datos.getNumeroDocumento()).ifPresent(otro -> {
            throw new BusinessException(
                    "Ese documento ya está registrado con otro email. Usa ese email o contacta a recepción.");
        });
        return clienteRepository.save(Cliente.builder()
                .nombre(datos.getNombre())
                .apellido(datos.getApellido())
                .tipoDocumento(datos.getTipoDocumento())
                .numeroDocumento(datos.getNumeroDocumento())
                .email(datos.getEmail())
                .telefono(datos.getTelefono())
                .activo(true)
                .build());
    }

    private void validarBusqueda(LocalDate entrada, LocalDate salida) {
        if (entrada == null || salida == null || !entrada.isBefore(salida)) {
            throw new BadRequestException("El rango de fechas es inválido: la llegada debe ser anterior a la salida");
        }
        if (entrada.isBefore(LocalDate.now())) {
            throw new BadRequestException("La fecha de llegada no puede ser en el pasado");
        }
    }

    private HabitacionCardView toCard(Habitacion h, long noches) {
        TipoHabitacion tipo = h.getTipoHabitacion();
        List<String> comodidades = !StringUtils.hasText(tipo.getComodidades())
                ? List.of()
                : Arrays.stream(tipo.getComodidades().split(",")).map(String::trim)
                        .filter(StringUtils::hasText).toList();
        BigDecimal precioNoche = tipo.getPrecioBaseNoche();
        return new HabitacionCardView(h.getId(), h.getNumero(), h.getPiso(), h.getDescripcion(),
                tipo.getNombre(), tipo.getDescripcion(), tipo.getCapacidadMaxima(),
                tipo.getImagen(), comodidades, precioNoche,
                precioNoche.multiply(BigDecimal.valueOf(noches)));
    }
}
