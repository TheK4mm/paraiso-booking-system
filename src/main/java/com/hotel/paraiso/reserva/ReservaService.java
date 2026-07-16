package com.hotel.paraiso.reserva;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.cliente.ClienteRepository;
import com.hotel.paraiso.common.audit.ActividadEvent;
import com.hotel.paraiso.common.exception.BadRequestException;
import com.hotel.paraiso.common.exception.BusinessException;
import com.hotel.paraiso.common.exception.ResourceNotFoundException;
import com.hotel.paraiso.common.web.SortWhitelist;
import com.hotel.paraiso.empleado.Empleado;
import com.hotel.paraiso.empleado.EmpleadoRepository;
import com.hotel.paraiso.facturacion.Factura.EstadoFactura;
import com.hotel.paraiso.facturacion.PagoRepository;
import com.hotel.paraiso.habitacion.Habitacion;
import com.hotel.paraiso.habitacion.Habitacion.EstadoHabitacion;
import com.hotel.paraiso.habitacion.HabitacionRepository;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import com.hotel.paraiso.servicio.Servicio;
import com.hotel.paraiso.servicio.ServicioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Lógica de negocio de reservas: máquina de estados, disponibilidad bajo
 * lock pesimista, cálculo de precios y generación atómica de códigos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReservaService {

    private static final long SIN_EXCLUSION = -1L;
    private static final Set<String> ORDENABLES =
            Set.of("id", "codigoReserva", "fechaEntrada", "fechaSalida", "estado", "precioTotal", "creadoEn");

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final HabitacionRepository habitacionRepository;
    private final ServicioRepository servicioRepository;
    private final PagoRepository pagoRepository;
    private final ReservaMapper reservaMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ─── Consultas ────────────────────────────────────────────────────

    public Page<ReservaResponse> buscar(String q, EstadoReserva estado, Long clienteId,
                                        LocalDate desde, LocalDate hasta, Pageable pageable) {
        Specification<Reserva> spec = Specification.allOf(Stream.of(
                ReservaSpecs.texto(q),
                ReservaSpecs.conEstado(estado),
                ReservaSpecs.deCliente(clienteId),
                ReservaSpecs.entradaDesde(desde),
                ReservaSpecs.entradaHasta(hasta)
        ).filter(Objects::nonNull).toList());
        Pageable saneado = SortWhitelist.sanitize(pageable, ORDENABLES, Sort.by(Sort.Direction.DESC, "id"));
        return reservaRepository.findAll(spec, saneado).map(reservaMapper::toResponse);
    }

    public ReservaResponse findById(Long id) {
        return reservaMapper.toResponse(getReservaOrThrow(id));
    }

    /** Detalle completo: 3 consultas fetch constantes + cálculo de saldos. */
    public ReservaDetalleResponse findDetalle(Long id) {
        Reserva reserva = reservaRepository.findByIdConHabitaciones(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));
        reservaRepository.findByIdConServicios(id);
        reservaRepository.findByIdConPagosYFactura(id);

        BigDecimal totalPagado = pagoRepository.sumPagosAprobadosByReservaId(id);
        BigDecimal limite = reserva.getFactura() != null
                && reserva.getFactura().getEstadoFactura() != EstadoFactura.ANULADA
                ? reserva.getFactura().getTotal()
                : reserva.getPrecioTotal();

        return reservaMapper.toDetalle(reserva).toBuilder()
                .totalPagado(totalPagado)
                .saldoPendiente(limite.subtract(totalPagado))
                .build();
    }

    public ReservaDetalleResponse findByCodigo(String codigo) {
        Reserva reserva = reservaRepository.findByCodigoReserva(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "codigoReserva", codigo));
        return findDetalle(reserva.getId());
    }

    public List<ReservaResponse> findByCliente(Long clienteId) {
        return reservaRepository.findByClienteId(clienteId)
                .stream().map(reservaMapper::toResponse).toList();
    }

    public List<ReservaResponse> findByEstado(EstadoReserva estado) {
        return reservaRepository.findByEstado(estado)
                .stream().map(reservaMapper::toResponse).toList();
    }

    // ─── Escritura ────────────────────────────────────────────────────

    @Transactional
    public ReservaDetalleResponse create(ReservaRequest request) {
        if (request.getFechaEntrada().isBefore(LocalDate.now())) {
            throw new BadRequestException("La fecha de entrada no puede ser en el pasado");
        }

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.getClienteId()));
        if (!cliente.getActivo()) {
            throw new BusinessException("El cliente está inactivo y no puede realizar reservas");
        }

        Empleado empleado = resolverEmpleado(request.getEmpleadoId());
        List<Habitacion> habitaciones = resolverHabitacionesConLock(
                request.getHabitacionIds(), request.getFechaEntrada(), request.getFechaSalida(), SIN_EXCLUSION);
        validarCapacidad(habitaciones, request.getNumeroHuespedes());
        List<Servicio> servicios = resolverServicios(request.getServicioIds());

        long noches = ChronoUnit.DAYS.between(request.getFechaEntrada(), request.getFechaSalida());
        Reserva reserva = Reserva.builder()
                .codigoReserva(generarCodigoReserva())
                .fechaEntrada(request.getFechaEntrada())
                .fechaSalida(request.getFechaSalida())
                .numeroHuespedes(request.getNumeroHuespedes())
                .totalNoches((int) noches)
                .precioTotal(calcularPrecio(habitaciones, servicios, noches))
                .observaciones(request.getObservaciones())
                .estado(EstadoReserva.PENDIENTE)
                .cliente(cliente)
                .empleado(empleado)
                .habitaciones(habitaciones)
                .servicios(servicios)
                .build();

        Reserva guardada = reservaRepository.save(reserva);
        eventPublisher.publishEvent(new ActividadEvent("RESERVA_CREADA", "Reserva", guardada.getId(),
                guardada.getCodigoReserva() + " para " + cliente.getNombre() + " " + cliente.getApellido()));
        log.info("Reserva creada: codigo={}, cliente={}", guardada.getCodigoReserva(), cliente.getEmail());
        return findDetalle(guardada.getId());
    }

    @Transactional
    public ReservaDetalleResponse update(Long id, ReservaRequest request) {
        Reserva reserva = getReservaOrThrow(id);

        if (reserva.getEstado() == EstadoReserva.CANCELADA
                || reserva.getEstado() == EstadoReserva.CHECKOUT
                || reserva.getEstado() == EstadoReserva.NO_SHOW) {
            throw new BusinessException("No se puede modificar una reserva en estado: " + reserva.getEstado());
        }

        List<Habitacion> habitaciones = resolverHabitacionesConLock(
                request.getHabitacionIds(), request.getFechaEntrada(), request.getFechaSalida(), id);
        validarCapacidad(habitaciones, request.getNumeroHuespedes());
        List<Servicio> servicios = resolverServicios(request.getServicioIds());

        long noches = ChronoUnit.DAYS.between(request.getFechaEntrada(), request.getFechaSalida());
        reserva.setFechaEntrada(request.getFechaEntrada());
        reserva.setFechaSalida(request.getFechaSalida());
        reserva.setNumeroHuespedes(request.getNumeroHuespedes());
        reserva.setTotalNoches((int) noches);
        reserva.setPrecioTotal(calcularPrecio(habitaciones, servicios, noches));
        reserva.setObservaciones(request.getObservaciones());
        reserva.setEmpleado(resolverEmpleado(request.getEmpleadoId()));
        reserva.setHabitaciones(habitaciones);
        reserva.setServicios(servicios);

        reservaRepository.save(reserva);
        return findDetalle(id);
    }

    @Transactional
    public ReservaDetalleResponse cambiarEstado(Long id, EstadoReserva nuevoEstado) {
        Reserva reserva = getReservaOrThrow(id);
        EstadoReserva actual = reserva.getEstado();
        validarTransicionEstado(actual, nuevoEstado);
        reserva.setEstado(nuevoEstado);
        aplicarEfectoSobreHabitaciones(reserva, actual, nuevoEstado);
        reservaRepository.save(reserva);
        eventPublisher.publishEvent(new ActividadEvent("RESERVA_" + nuevoEstado, "Reserva", id,
                reserva.getCodigoReserva() + ": " + actual + " a " + nuevoEstado));
        log.info("Reserva {} cambió de estado: {} → {}", reserva.getCodigoReserva(), actual, nuevoEstado);
        return findDetalle(id);
    }

    @Transactional
    public void cancelar(Long id) {
        Reserva reserva = getReservaOrThrow(id);
        if (reserva.getEstado() == EstadoReserva.CHECKOUT) {
            throw new BusinessException("No se puede cancelar una reserva con checkout realizado");
        }
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            return;
        }
        EstadoReserva anterior = reserva.getEstado();
        reserva.setEstado(EstadoReserva.CANCELADA);
        aplicarEfectoSobreHabitaciones(reserva, anterior, EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);
        eventPublisher.publishEvent(new ActividadEvent("RESERVA_CANCELADA", "Reserva", id,
                reserva.getCodigoReserva()));
        log.info("Reserva cancelada id={}", id);
    }

    // ─── Reglas internas ──────────────────────────────────────────────

    /** Transiciones válidas de la máquina de estados. */
    private void validarTransicionEstado(EstadoReserva actual, EstadoReserva nuevo) {
        boolean valida = switch (actual) {
            case PENDIENTE  -> nuevo == EstadoReserva.CONFIRMADA || nuevo == EstadoReserva.CANCELADA;
            case CONFIRMADA -> nuevo == EstadoReserva.CHECKIN    || nuevo == EstadoReserva.CANCELADA;
            case CHECKIN    -> nuevo == EstadoReserva.CHECKOUT   || nuevo == EstadoReserva.NO_SHOW;
            default         -> false;
        };
        if (!valida) {
            throw new BusinessException("Transición de estado no permitida: " + actual + " → " + nuevo);
        }
    }

    /** El check-in ocupa las habitaciones; salir de check-in las libera. */
    private void aplicarEfectoSobreHabitaciones(Reserva reserva, EstadoReserva anterior, EstadoReserva nuevo) {
        if (nuevo == EstadoReserva.CHECKIN) {
            reserva.getHabitaciones().forEach(h -> h.setEstado(EstadoHabitacion.OCUPADA));
        } else if (anterior == EstadoReserva.CHECKIN) {
            reserva.getHabitaciones().stream()
                    .filter(h -> h.getEstado() == EstadoHabitacion.OCUPADA)
                    .forEach(h -> h.setEstado(EstadoHabitacion.DISPONIBLE));
        }
    }

    /**
     * Carga las habitaciones con lock pesimista y re-verifica disponibilidad
     * dentro de la misma transacción: dos reservas concurrentes sobre la
     * misma habitación quedan serializadas y la segunda es rechazada.
     */
    private List<Habitacion> resolverHabitacionesConLock(List<Long> ids, LocalDate entrada,
                                                         LocalDate salida, long excluirReservaId) {
        List<Habitacion> habitaciones = habitacionRepository.findAllByIdForUpdate(ids);
        if (habitaciones.size() != ids.size()) {
            throw new ResourceNotFoundException("Habitacion", "ids", ids);
        }
        for (Habitacion h : habitaciones) {
            if (!h.getActivo() || h.getEstado() == EstadoHabitacion.MANTENIMIENTO
                    || h.getEstado() == EstadoHabitacion.BLOQUEADA) {
                throw new BusinessException("La habitación " + h.getNumero() + " no está disponible");
            }
            long conflictos = reservaRepository.countReservasActivasParaHabitacion(
                    h.getId(), entrada, salida, excluirReservaId);
            if (conflictos > 0) {
                throw new BusinessException("La habitación " + h.getNumero()
                        + " ya está reservada para las fechas seleccionadas");
            }
        }
        return habitaciones;
    }

    private void validarCapacidad(List<Habitacion> habitaciones, Integer numeroHuespedes) {
        int capacidadTotal = habitaciones.stream()
                .mapToInt(h -> h.getTipoHabitacion().getCapacidadMaxima())
                .sum();
        if (numeroHuespedes > capacidadTotal) {
            throw new BusinessException(String.format(
                    "El número de huéspedes (%d) supera la capacidad de las habitaciones seleccionadas (%d)",
                    numeroHuespedes, capacidadTotal));
        }
    }

    private Empleado resolverEmpleado(Long empleadoId) {
        if (empleadoId == null) {
            return null;
        }
        return empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", empleadoId));
    }

    private List<Servicio> resolverServicios(List<Long> servicioIds) {
        if (servicioIds == null || servicioIds.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(servicioRepository.findAllById(servicioIds));
    }

    private BigDecimal calcularPrecio(List<Habitacion> habitaciones, List<Servicio> servicios, long noches) {
        BigDecimal totalHabitaciones = habitaciones.stream()
                .map(h -> h.getTipoHabitacion().getPrecioBaseNoche().multiply(BigDecimal.valueOf(noches)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalServicios = servicios.stream()
                .map(Servicio::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalHabitaciones.add(totalServicios);
    }

    /** Código con secuencia de BD: atómico, sin colisiones bajo concurrencia. */
    private String generarCodigoReserva() {
        return String.format("RES-%d-%06d", Year.now().getValue(), reservaRepository.nextCodigoSeq());
    }

    private Reserva getReservaOrThrow(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));
    }
}
