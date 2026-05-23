package com.hotel.paraiso.service;

import com.hotel.paraiso.dto.ServicioDTO;
import com.hotel.paraiso.exception.BadRequestException;
import com.hotel.paraiso.exception.ResourceNotFoundException;
import com.hotel.paraiso.model.Servicio;
import com.hotel.paraiso.repository.ServicioRepository;
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
public class ServicioService implements IViewMapService<ServicioDTO.Response> {

    private final ServicioRepository servicioRepository;

    public List<ServicioDTO.Response> findAll() {
        return servicioRepository.findByActivoTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ServicioDTO.Response findById(Long id) {
        return toResponse(getServicioOrThrow(id));
    }

    @Override
    public List<Map<String, Object>> findAllAsMap() {
        return findAll().stream()
                .map(ServicioDTO.Response::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> findByIdAsMap(Long id) {
        return findById(id).toMap();
    }

    public List<ServicioDTO.Response> findByCategoria(Servicio.CategoriaServicio categoria) {
        return servicioRepository.findByCategoria(categoria)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ServicioDTO.Response create(ServicioDTO.Request request) {
        if (servicioRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new BadRequestException("Ya existe un servicio con el nombre: " + request.getNombre());
        }

        Servicio servicio = Servicio.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .categoria(request.getCategoria())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .build();

        Servicio guardado = servicioRepository.save(servicio);
        log.info("Servicio creado id={}", guardado.getId());
        return toResponse(guardado);
    }

    @Transactional
    public ServicioDTO.Response update(Long id, ServicioDTO.Request request) {
        Servicio servicio = getServicioOrThrow(id);

        servicioRepository.findByNombreIgnoreCase(request.getNombre())
                .ifPresent(s -> {
                    if (!s.getId().equals(id))
                        throw new BadRequestException("Nombre de servicio ya registrado");
                });

        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setPrecio(request.getPrecio());
        servicio.setCategoria(request.getCategoria());
        if (request.getActivo() != null) servicio.setActivo(request.getActivo());

        return toResponse(servicioRepository.save(servicio));
    }

    @Transactional
    public void delete(Long id) {
        Servicio servicio = getServicioOrThrow(id);
        servicio.setActivo(false);
        servicioRepository.save(servicio);
    }

    // ─── helpers ─────────────────────────────────────────────
    public Servicio getServicioOrThrow(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio", "id", id));
    }

    public ServicioDTO.Response toResponse(Servicio s) {
        return ServicioDTO.Response.builder()
                .id(s.getId())
                .nombre(s.getNombre())
                .descripcion(s.getDescripcion())
                .precio(s.getPrecio())
                .categoria(s.getCategoria())
                .activo(s.getActivo())
                .build();
    }
}
