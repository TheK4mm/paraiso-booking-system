package com.hotel.paraiso.service;

import com.hotel.paraiso.dto.ClienteDTO;
import com.hotel.paraiso.exception.BadRequestException;
import com.hotel.paraiso.exception.ResourceNotFoundException;
import com.hotel.paraiso.model.Cliente;
import com.hotel.paraiso.repository.ClienteRepository;
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
public class ClienteService implements IViewMapService<ClienteDTO.Response> {

    private final ClienteRepository clienteRepository;

    public List<ClienteDTO.Response> findAll() {
        return clienteRepository.findByActivoTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ClienteDTO.Response findById(Long id) {
        return toResponse(getClienteOrThrow(id));
    }

    @Override
    public List<Map<String, Object>> findAllAsMap() {
        return findAll().stream()
                .map(ClienteDTO.Response::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> findByIdAsMap(Long id) {
        return findById(id).toMap();
    }

    public List<ClienteDTO.Response> search(String termino) {
        return clienteRepository
                .findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(termino, termino)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ClienteDTO.Response create(ClienteDTO.Request request) {
        if (clienteRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Ya existe un cliente con el email: " + request.getEmail());
        }
        if (clienteRepository.existsByNumeroDocumento(request.getNumeroDocumento())) {
            throw new BadRequestException("Ya existe un cliente con el documento: " + request.getNumeroDocumento());
        }

        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .tipoDocumento(request.getTipoDocumento())
                .numeroDocumento(request.getNumeroDocumento())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .pais(request.getPais())
                .activo(true)
                .build();

        Cliente guardado = clienteRepository.save(cliente);
        log.info("Cliente creado id={}, email={}", guardado.getId(), guardado.getEmail());
        return toResponse(guardado);
    }

    @Transactional
    public ClienteDTO.Response update(Long id, ClienteDTO.Request request) {
        Cliente cliente = getClienteOrThrow(id);

        // Verificar duplicados excluyendo al propio cliente
        clienteRepository.findByEmail(request.getEmail())
                .ifPresent(e -> { if (!e.getId().equals(id)) throw new BadRequestException("Email ya registrado por otro cliente"); });
        clienteRepository.findByNumeroDocumento(request.getNumeroDocumento())
                .ifPresent(e -> { if (!e.getId().equals(id)) throw new BadRequestException("Documento ya registrado por otro cliente"); });

        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setTipoDocumento(request.getTipoDocumento());
        cliente.setNumeroDocumento(request.getNumeroDocumento());
        cliente.setEmail(request.getEmail());
        cliente.setTelefono(request.getTelefono());
        cliente.setDireccion(request.getDireccion());
        cliente.setPais(request.getPais());

        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void delete(Long id) {
        Cliente cliente = getClienteOrThrow(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
        log.info("Cliente desactivado id={}", id);
    }

    // ─── helpers ────────────────────────────────────────────
    private Cliente getClienteOrThrow(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", id));
    }

    public ClienteDTO.Response toResponse(Cliente c) {
        int totalReservas = c.getReservas() != null ? c.getReservas().size() : 0;
        return ClienteDTO.Response.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .apellido(c.getApellido())
                .nombreCompleto(c.getNombre() + " " + c.getApellido())
                .tipoDocumento(c.getTipoDocumento())
                .numeroDocumento(c.getNumeroDocumento())
                .email(c.getEmail())
                .telefono(c.getTelefono())
                .direccion(c.getDireccion())
                .pais(c.getPais())
                .activo(c.getActivo())
                .fechaRegistro(c.getFechaRegistro())
                .totalReservas(totalReservas)
                .build();

    }
}


