package com.hotel.paraiso.service;

import com.hotel.paraiso.dto.*;
import com.hotel.paraiso.exception.BadRequestException;
import com.hotel.paraiso.exception.BusinessException;
import com.hotel.paraiso.exception.ResourceNotFoundException;
import com.hotel.paraiso.model.*;
import com.hotel.paraiso.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReservaService implements IViewMapService<ReservaDTO.Response> {

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final HabitacionRepository habitacionRepository;
    private final ServicioRepository servicioRepository;
    private final HabitacionService habitacionService;
    private final ServicioService servicioService;
    private final PagoService pagoService;
    private final FacturaService facturaService;

    public List<ReservaDTO.Response> findAll() {
        return reservaRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ReservaDTO.Response findById(Long id) {
        return toResponse(getReservaOrThrow(id));
    }

    public ReservaDTO.Response findByCodigo(String codigo) {
        Reserva reserva = reservaRepository.findByCodigoReserva(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "codigoReserva", codigo));
        return toResponse(reserva);
    }

    @Override
    public List<Map<String, Object>> findAllAsMap() {
        return findAll().stream()
                .map(ReservaDTO.Response::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> findByIdAsMap(Long id) {
        return findById(id).toMap();
    }

    public List<ReservaDTO.Response> findByCliente(Long clienteId) {
        return reservaRepository.findByClienteId(clienteId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ReservaDTO.Response> findByEstado(Reserva.EstadoReserva estado) {
        return reservaRepository.findByEstado(estado)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── CREATE ──────────────────────────────────────────────────
    @Transactional
    public ReservaDTO.Response create(ReservaDTO.Request request) {
        // 1. Validar fechas
        validarFechas(request.getFechaEntrada(), request.getFechaSalida());

        // 2. Obtener cliente
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.getClienteId()));

        if (!cliente.getActivo()) {
            throw new BusinessException("El cliente está inactivo y no puede realizar reservas");
        }

        // 3. Obtener empleado (opcional)
        Empleado empleado = null;
        if (request.getEmpleadoId() != null) {
            empleado = empleadoRepository.findById(request.getEmpleadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", request.getEmpleadoId()));
        }

        // 4. Verificar y obtener habitaciones
        List<Habitacion> habitaciones = resolverHabitaciones(request.getHabitacionIds(),
                request.getFechaEntrada(), request.getFechaSalida());

        // 5. Obtener servicios (opcional)
        List<Servicio> servicios = new ArrayList<>();
        if (request.getServicioIds() != null && !request.getServicioIds().isEmpty()) {
            servicios = servicioRepository.findAllById(request.getServicioIds());
        }

        // 6. Calcular precios
        long noches = request.getFechaEntrada().until(request.getFechaSalida()).getDays();
        BigDecimal totalHabitaciones = habitaciones.stream()
                .map(h -> h.getTipoHabitacion().getPrecioBaseNoche().multiply(BigDecimal.valueOf(noches)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalServicios = servicios.stream()
                .map(Servicio::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal precioTotal = totalHabitaciones.add(totalServicios);

        // 7. Construir y persistir la reserva
        Reserva reserva = Reserva.builder()
                .codigoReserva(generarCodigoReserva())
                .fechaEntrada(request.getFechaEntrada())
                .fechaSalida(request.getFechaSalida())
                .numeroHuespedes(request.getNumeroHuespedes())
                .totalNoches((int) noches)
                .precioTotal(precioTotal)
                .observaciones(request.getObservaciones())
                .estado(Reserva.EstadoReserva.PENDIENTE)
                .cliente(cliente)
                .empleado(empleado)
                .habitaciones(habitaciones)
                .servicios(servicios)
                .build();

        Reserva guardada = reservaRepository.save(reserva);
        log.info("Reserva creada: codigo={}, cliente={}", guardada.getCodigoReserva(), cliente.getEmail());
        return toResponse(guardada);
    }

    // ─── UPDATE ──────────────────────────────────────────────────
    @Transactional
    public ReservaDTO.Response update(Long id, ReservaDTO.Request request) {
        Reserva reserva = getReservaOrThrow(id);

        if (reserva.getEstado() == Reserva.EstadoReserva.CANCELADA
                || reserva.getEstado() == Reserva.EstadoReserva.CHECKOUT) {
            throw new BusinessException("No se puede modificar una reserva en estado: " + reserva.getEstado());
        }

        validarFechas(request.getFechaEntrada(), request.getFechaSalida());

        List<Habitacion> habitaciones = resolverHabitaciones(
                request.getHabitacionIds(), request.getFechaEntrada(), request.getFechaSalida()
        );

        List<Servicio> servicios = new ArrayList<>();
        if (request.getServicioIds() != null && !request.getServicioIds().isEmpty()) {
            servicios = servicioRepository.findAllById(request.getServicioIds());
        }

        long noches = request.getFechaEntrada().until(request.getFechaSalida()).getDays();
        BigDecimal totalHabitaciones = habitaciones.stream()
                .map(h -> h.getTipoHabitacion().getPrecioBaseNoche().multiply(BigDecimal.valueOf(noches)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalServicios = servicios.stream()
                .map(Servicio::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        reserva.setFechaEntrada(request.getFechaEntrada());
        reserva.setFechaSalida(request.getFechaSalida());
        reserva.setNumeroHuespedes(request.getNumeroHuespedes());
        reserva.setTotalNoches((int) noches);
        reserva.setPrecioTotal(totalHabitaciones.add(totalServicios));
        reserva.setObservaciones(request.getObservaciones());
        reserva.setHabitaciones(habitaciones);
        reserva.setServicios(servicios);

        return toResponse(reservaRepository.save(reserva));
    }

    // ─── CAMBIO DE ESTADO ────────────────────────────────────────
    @Transactional
    public ReservaDTO.Response cambiarEstado(Long id, ReservaDTO.EstadoRequest request) {
        Reserva reserva = getReservaOrThrow(id);
        validarTransicionEstado(reserva.getEstado(), request.getEstado());
        reserva.setEstado(request.getEstado());
        return toResponse(reservaRepository.save(reserva));
    }

    // ─── DELETE (cancelar) ───────────────────────────────────────
    @Transactional
    public void cancel(Long id) {
        Reserva reserva = getReservaOrThrow(id);
        if (reserva.getEstado() == Reserva.EstadoReserva.CHECKOUT) {
            throw new BusinessException("No se puede cancelar una reserva con checkout realizado");
        }
        reserva.setEstado(Reserva.EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);
        log.info("Reserva cancelada id={}", id);
    }

    // ─── PRIVADOS ────────────────────────────────────────────────

    private void validarFechas(LocalDate entrada, LocalDate salida) {
        if (!entrada.isBefore(salida)) {
            throw new BadRequestException("La fecha de entrada debe ser anterior a la fecha de salida");
        }
        if (entrada.isBefore(LocalDate.now())) {
            throw new BadRequestException("La fecha de entrada no puede ser en el pasado");
        }
    }

    private List<Habitacion> resolverHabitaciones(List<Long> ids, LocalDate entrada, LocalDate salida) {
        List<Habitacion> habitaciones = habitacionRepository.findAllById(ids);
        if (habitaciones.size() != ids.size()) {
            throw new ResourceNotFoundException("Habitacion", "ids", ids);
        }
        for (Habitacion h : habitaciones) {
            if (!h.getActivo() || h.getEstado() == Habitacion.EstadoHabitacion.MANTENIMIENTO) {
                throw new BusinessException("La habitación " + h.getNumero() + " no está disponible");
            }
            long conflictos = reservaRepository.countReservasActivasParaHabitacion(h.getId(), entrada, salida);
            if (conflictos > 0) {
                throw new BusinessException("La habitación " + h.getNumero()
                        + " ya está reservada para las fechas seleccionadas");
            }
        }
        return habitaciones;
    }

    private void validarTransicionEstado(Reserva.EstadoReserva actual, Reserva.EstadoReserva nuevo) {
        boolean valida = switch (actual) {
            case PENDIENTE   -> nuevo == Reserva.EstadoReserva.CONFIRMADA || nuevo == Reserva.EstadoReserva.CANCELADA;
            case CONFIRMADA  -> nuevo == Reserva.EstadoReserva.CHECKIN    || nuevo == Reserva.EstadoReserva.CANCELADA;
            case CHECKIN     -> nuevo == Reserva.EstadoReserva.CHECKOUT   || nuevo == Reserva.EstadoReserva.NO_SHOW;
            default          -> false;
        };
        if (!valida) {
            throw new BusinessException("Transición de estado no permitida: " + actual + " → " + nuevo);
        }
    }

    private String generarCodigoReserva() {
        String anio = DateTimeFormatter.ofPattern("yyyy").format(LocalDate.now());
        long count = reservaRepository.count() + 1;
        return String.format("RES-%s-%06d", anio, count);
    }

    private Reserva getReservaOrThrow(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));
    }

    public ReservaDTO.Response toResponse(Reserva r) {
        List<HabitacionDTO.Response> habitacionesDto = r.getHabitaciones() != null
                ? r.getHabitaciones().stream().map(habitacionService::toResponse).collect(Collectors.toList())
                : List.of();

        List<ServicioDTO.Response> serviciosDto = r.getServicios() != null
                ? r.getServicios().stream().map(servicioService::toResponse).collect(Collectors.toList())
                : List.of();

        List<PagoDTO.Response> pagosDto = r.getPagos() != null
                ? r.getPagos().stream().map(pagoService::toResponse).collect(Collectors.toList())
                : List.of();

        FacturaDTO.Response facturaDto = r.getFactura() != null
                ? facturaService.toResponse(r.getFactura())
                : null;

        return ReservaDTO.Response.builder()
                .id(r.getId())
                .codigoReserva(r.getCodigoReserva())
                .fechaEntrada(r.getFechaEntrada())
                .fechaSalida(r.getFechaSalida())
                .numeroHuespedes(r.getNumeroHuespedes())
                .totalNoches(r.getTotalNoches())
                .precioTotal(r.getPrecioTotal())
                .observaciones(r.getObservaciones())
                .estado(r.getEstado())
                .fechaCreacion(r.getFechaCreacion())
                .clienteId(r.getCliente().getId())
                .clienteNombreCompleto(r.getCliente().getNombre() + " " + r.getCliente().getApellido())
                .clienteDocumento(r.getCliente().getNumeroDocumento())
                .empleadoId(r.getEmpleado() != null ? r.getEmpleado().getId() : null)
                .empleadoNombreCompleto(r.getEmpleado() != null
                        ? r.getEmpleado().getNombre() + " " + r.getEmpleado().getApellido() : null)
                .habitaciones(habitacionesDto)
                .servicios(serviciosDto)
                .pagos(pagosDto)
                .factura(facturaDto)
                .build();
    }
}
