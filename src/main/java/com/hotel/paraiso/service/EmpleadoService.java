package com.hotel.paraiso.service;

import com.hotel.paraiso.dto.EmpleadoDTO;
import com.hotel.paraiso.exception.BadRequestException;
import com.hotel.paraiso.exception.ResourceNotFoundException;
import com.hotel.paraiso.model.Empleado;
import com.hotel.paraiso.repository.EmpleadoRepository;
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
public class EmpleadoService implements IViewMapService<EmpleadoDTO.Response> {

    private final EmpleadoRepository empleadoRepository;

    public List<EmpleadoDTO.Response> findAll() {
        return empleadoRepository.findByActivoTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public EmpleadoDTO.Response findById(Long id) {
        return toResponse(getEmpleadoOrThrow(id));
    }

    @Override
    public List<Map<String, Object>> findAllAsMap() {
        return findAll().stream()
                .map(EmpleadoDTO.Response::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> findByIdAsMap(Long id) {
        return findById(id).toMap();
    }

    @Transactional
    public EmpleadoDTO.Response create(EmpleadoDTO.Request request) {
        if (empleadoRepository.existsByNumeroDocumento(request.getNumeroDocumento())) {
            throw new BadRequestException("Ya existe un empleado con el documento: " + request.getNumeroDocumento());
        }

        Empleado empleado = Empleado.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .numeroDocumento(request.getNumeroDocumento())
                .cargo(request.getCargo())
                .emailCorporativo(request.getEmailCorporativo())
                .telefonoExtension(request.getTelefonoExtension())
                .fechaContratacion(request.getFechaContratacion())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .build();

        Empleado guardado = empleadoRepository.save(empleado);
        log.info("Empleado creado id={}", guardado.getId());
        return toResponse(guardado);
    }

    @Transactional
    public EmpleadoDTO.Response update(Long id, EmpleadoDTO.Request request) {
        Empleado empleado = getEmpleadoOrThrow(id);

        empleadoRepository.findByNumeroDocumento(request.getNumeroDocumento())
                .ifPresent(e -> {
                    if (!e.getId().equals(id))
                        throw new BadRequestException("Documento ya registrado por otro empleado");
                });

        empleado.setNombre(request.getNombre());
        empleado.setApellido(request.getApellido());
        empleado.setNumeroDocumento(request.getNumeroDocumento());
        empleado.setCargo(request.getCargo());
        empleado.setEmailCorporativo(request.getEmailCorporativo());
        empleado.setTelefonoExtension(request.getTelefonoExtension());
        empleado.setFechaContratacion(request.getFechaContratacion());
        if (request.getActivo() != null) empleado.setActivo(request.getActivo());

        return toResponse(empleadoRepository.save(empleado));
    }

    @Transactional
    public void delete(Long id) {
        Empleado empleado = getEmpleadoOrThrow(id);
        empleado.setActivo(false);
        empleadoRepository.save(empleado);
        log.info("Empleado desactivado id={}", id);
    }

    // ─── helpers ─────────────────────────────────────────────
    public Empleado getEmpleadoOrThrow(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", id));
    }

    public EmpleadoDTO.Response toResponse(Empleado e) {
        int totalReservas = e.getReservas() != null ? e.getReservas().size() : 0;
        return EmpleadoDTO.Response.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .apellido(e.getApellido())
                .nombreCompleto(e.getNombre() + " " + e.getApellido())
                .numeroDocumento(e.getNumeroDocumento())
                .cargo(e.getCargo())
                .emailCorporativo(e.getEmailCorporativo())
                .telefonoExtension(e.getTelefonoExtension())
                .fechaContratacion(e.getFechaContratacion())
                .activo(e.getActivo())
                .fechaRegistro(e.getFechaRegistro())
                .totalReservasGestionadas(totalReservas)
                .build();
    }
}
