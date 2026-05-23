package com.hotel.paraiso.service;

import com.hotel.paraiso.dto.HabitacionDTO;
import com.hotel.paraiso.exception.BadRequestException;
import com.hotel.paraiso.exception.ResourceNotFoundException;
import com.hotel.paraiso.model.Habitacion;
import com.hotel.paraiso.model.TipoHabitacion;
import com.hotel.paraiso.repository.HabitacionRepository;
import com.hotel.paraiso.repository.TipoHabitacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HabitacionService implements IViewMapService<HabitacionDTO.Response> {

    private final HabitacionRepository habitacionRepository;
    private final TipoHabitacionRepository tipoHabitacionRepository;

    public List<HabitacionDTO.Response> findAll() {
        return habitacionRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public HabitacionDTO.Response findById(Long id) {
        return toResponse(getHabitacionOrThrow(id));
    }

    @Override
    public List<Map<String, Object>> findAllAsMap() {
        return findAll().stream()
                .map(HabitacionDTO.Response::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> findByIdAsMap(Long id) {
        return findById(id).toMap();
    }

    public List<HabitacionDTO.Response> findDisponibles(LocalDate entrada, LocalDate salida) {
        if (!entrada.isBefore(salida)) {
            throw new BadRequestException("La fecha de entrada debe ser anterior a la fecha de salida");
        }
        return habitacionRepository.findHabitacionesDisponibles(entrada, salida)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public HabitacionDTO.Response create(HabitacionDTO.Request request) {
        if (habitacionRepository.existsByNumero(request.getNumero())) {
            throw new BadRequestException("Ya existe una habitación con el número: " + request.getNumero());
        }
        TipoHabitacion tipo = tipoHabitacionRepository.findById(request.getTipoHabitacionId())
                .orElseThrow(() -> new ResourceNotFoundException("TipoHabitacion", "id", request.getTipoHabitacionId()));

        Habitacion habitacion = Habitacion.builder()
                .numero(request.getNumero())
                .piso(request.getPiso())
                .descripcion(request.getDescripcion())
                .estado(request.getEstado() != null ? request.getEstado() : Habitacion.EstadoHabitacion.DISPONIBLE)
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .tipoHabitacion(tipo)
                .build();

        Habitacion guardada = habitacionRepository.save(habitacion);
        log.info("Habitacion creada id={}, numero={}", guardada.getId(), guardada.getNumero());
        return toResponse(guardada);
    }

    @Transactional
    public HabitacionDTO.Response update(Long id, HabitacionDTO.Request request) {
        Habitacion habitacion = getHabitacionOrThrow(id);

        habitacionRepository.findByNumero(request.getNumero())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BadRequestException("Ya existe otra habitación con el número: " + request.getNumero());
                    }
                });

        TipoHabitacion tipo = tipoHabitacionRepository.findById(request.getTipoHabitacionId())
                .orElseThrow(() -> new ResourceNotFoundException("TipoHabitacion", "id", request.getTipoHabitacionId()));

        habitacion.setNumero(request.getNumero());
        habitacion.setPiso(request.getPiso());
        habitacion.setDescripcion(request.getDescripcion());
        if (request.getEstado() != null) habitacion.setEstado(request.getEstado());
        if (request.getActivo() != null) habitacion.setActivo(request.getActivo());
        habitacion.setTipoHabitacion(tipo);

        return toResponse(habitacionRepository.save(habitacion));
    }

    @Transactional
    public void delete(Long id) {
        Habitacion habitacion = getHabitacionOrThrow(id);
        habitacion.setActivo(false);
        habitacion.setEstado(Habitacion.EstadoHabitacion.BLOQUEADA);
        habitacionRepository.save(habitacion);
        log.info("Habitacion desactivada id={}", id);
    }

    // ─── helpers ────────────────────────────────────────────
    private Habitacion getHabitacionOrThrow(Long id) {
        return habitacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habitacion", "id", id));
    }

    public HabitacionDTO.Response toResponse(Habitacion h) {
        return HabitacionDTO.Response.builder()
                .id(h.getId())
                .numero(h.getNumero())
                .piso(h.getPiso())
                .descripcion(h.getDescripcion())
                .estado(h.getEstado())
                .activo(h.getActivo())
                .tipoHabitacionId(h.getTipoHabitacion().getId())
                .tipoHabitacionNombre(h.getTipoHabitacion().getNombre())
                .precioBaseNoche(h.getTipoHabitacion().getPrecioBaseNoche())
                .capacidadMaxima(h.getTipoHabitacion().getCapacidadMaxima())
                .build();
    }
}
