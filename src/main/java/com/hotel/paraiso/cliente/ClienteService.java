package com.hotel.paraiso.cliente;

import com.hotel.paraiso.common.crud.AbstractCrudService;
import com.hotel.paraiso.common.exception.BadRequestException;
import com.hotel.paraiso.common.web.SortWhitelist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Slf4j
public class ClienteService extends AbstractCrudService<Cliente, ClienteRequest, ClienteResponse> {

    private static final Set<String> ORDENABLES =
            Set.of("id", "nombre", "apellido", "email", "numeroDocumento", "pais", "creadoEn");

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository repository, ClienteMapper mapper) {
        super(repository, mapper);
        this.clienteRepository = repository;
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> buscar(String q, boolean incluirInactivos, Pageable pageable) {
        Specification<Cliente> spec = Specification.allOf(Stream.of(
                incluirInactivos ? null : soloActivos(),
                ClienteSpecs.texto(q)
        ).filter(Objects::nonNull).toList());
        Pageable saneado = SortWhitelist.sanitize(pageable, ORDENABLES, Sort.by(Sort.Direction.DESC, "id"));
        return buscar(spec, saneado);
    }

    @Override
    protected void beforeCreate(ClienteRequest request) {
        validarUnicidad(request, null);
    }

    @Override
    protected void beforeUpdate(Long id, ClienteRequest request, Cliente entity) {
        validarUnicidad(request, id);
    }

    @Override
    protected ClienteResponse toDetalle(Cliente entity) {
        return mapper.toResponse(entity).toBuilder()
                .totalReservas(clienteRepository.countReservas(entity.getId()))
                .build();
    }

    @Override
    protected String entityName() {
        return "Cliente";
    }

    private void validarUnicidad(ClienteRequest request, Long idActual) {
        clienteRepository.findByEmailIgnoreCase(request.getEmail()).ifPresent(existente -> {
            if (!existente.getId().equals(idActual)) {
                throw new BadRequestException("Ya existe un cliente con el email: " + request.getEmail());
            }
        });
        clienteRepository.findByNumeroDocumento(request.getNumeroDocumento()).ifPresent(existente -> {
            if (!existente.getId().equals(idActual)) {
                throw new BadRequestException("Ya existe un cliente con el documento: " + request.getNumeroDocumento());
            }
        });
    }
}
