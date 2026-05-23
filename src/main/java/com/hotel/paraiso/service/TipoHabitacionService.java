package com.hotel.paraiso.service;

import com.hotel.paraiso.dto.TipoHabitacionDTO;
import com.hotel.paraiso.exception.BadRequestException;
import com.hotel.paraiso.exception.ResourceNotFoundException;
import com.hotel.paraiso.model.TipoHabitacion;
import com.hotel.paraiso.repository.TipoHabitacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TipoHabitacionService implements ICategoryService {

    private final TipoHabitacionRepository tipoHabitacionRepository;

    public List<TipoHabitacionDTO.Response> findAll() {
        log.debug("Obteniendo todos los tipos de habitación");
        return tipoHabitacionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TipoHabitacionDTO.Response findById(Long id) {
        TipoHabitacion tipo = tipoHabitacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoHabitacion", "id", id));
        return toResponse(tipo);
    }

    @Override
    public List<Map<String, Object>> findAllAsMap() {
        return findAll().stream()
                .map(TipoHabitacionDTO.Response::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> findByIdAsMap(Long id) {
        return findById(id).toMap();
    }

    @Transactional
    public TipoHabitacionDTO.Response create(TipoHabitacionDTO.Request request) {
        if (tipoHabitacionRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new BadRequestException("Ya existe un tipo de habitación con el nombre: " + request.getNombre());
        }
        TipoHabitacion tipo = TipoHabitacion.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .capacidadMaxima(request.getCapacidadMaxima())
                .precioBaseNoche(request.getPrecioBaseNoche())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .build();

        TipoHabitacion guardado = tipoHabitacionRepository.save(tipo);
        log.info("TipoHabitacion creado con id={}", guardado.getId());
        return toResponse(guardado);
    }

    @Transactional
    public TipoHabitacionDTO.Response update(Long id, TipoHabitacionDTO.Request request) {
        TipoHabitacion tipo = tipoHabitacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoHabitacion", "id", id));

        tipoHabitacionRepository.findByNombreIgnoreCase(request.getNombre())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BadRequestException("Ya existe otro tipo de habitación con el nombre: " + request.getNombre());
                    }
                });

        tipo.setNombre(request.getNombre());
        tipo.setDescripcion(request.getDescripcion());
        tipo.setCapacidadMaxima(request.getCapacidadMaxima());
        tipo.setPrecioBaseNoche(request.getPrecioBaseNoche());
        if (request.getActivo() != null) tipo.setActivo(request.getActivo());

        TipoHabitacion actualizado = tipoHabitacionRepository.save(tipo);
        log.info("TipoHabitacion actualizado id={}", actualizado.getId());
        return toResponse(actualizado);
    }

    @Transactional
    public void delete(Long id) {
        TipoHabitacion tipo = tipoHabitacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoHabitacion", "id", id));
        tipo.setActivo(false);
        tipoHabitacionRepository.save(tipo);
        log.info("TipoHabitacion desactivado id={}", id);
    }

    public TipoHabitacionDTO.Response toResponse(TipoHabitacion tipo) {
        int totalHabitaciones = tipo.getHabitaciones() != null ? tipo.getHabitaciones().size() : 0;
        return TipoHabitacionDTO.Response.builder()
                .id(tipo.getId())
                .nombre(tipo.getNombre())
                .descripcion(tipo.getDescripcion())
                .capacidadMaxima(tipo.getCapacidadMaxima())
                .precioBaseNoche(tipo.getPrecioBaseNoche())
                .activo(tipo.getActivo())
                .totalHabitaciones(totalHabitaciones)
                .build();
    }
}
