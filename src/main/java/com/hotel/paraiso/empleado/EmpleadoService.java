package com.hotel.paraiso.empleado;

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
public class EmpleadoService extends AbstractCrudService<Empleado, EmpleadoRequest, EmpleadoResponse> {

    private static final Set<String> ORDENABLES =
            Set.of("id", "nombre", "apellido", "cargo", "fechaContratacion", "creadoEn");

    private final EmpleadoRepository empleadoRepository;

    public EmpleadoService(EmpleadoRepository repository, EmpleadoMapper mapper) {
        super(repository, mapper);
        this.empleadoRepository = repository;
    }

    @Transactional(readOnly = true)
    public Page<EmpleadoResponse> buscar(String q, boolean incluirInactivos, Pageable pageable) {
        Specification<Empleado> spec = Specification.allOf(Stream.of(
                incluirInactivos ? null : soloActivos(),
                EmpleadoSpecs.texto(q)
        ).filter(Objects::nonNull).toList());
        return buscar(spec, SortWhitelist.sanitize(pageable, ORDENABLES, Sort.by(Sort.Direction.DESC, "id")));
    }

    @Override
    protected void beforeCreate(EmpleadoRequest request) {
        validarUnicidad(request, null);
    }

    @Override
    protected void beforeUpdate(Long id, EmpleadoRequest request, Empleado entity) {
        validarUnicidad(request, id);
    }

    @Override
    protected EmpleadoResponse toDetalle(Empleado entity) {
        return mapper.toResponse(entity).toBuilder()
                .totalReservasGestionadas(empleadoRepository.countReservasGestionadas(entity.getId()))
                .build();
    }

    @Override
    protected String entityName() {
        return "Empleado";
    }

    private void validarUnicidad(EmpleadoRequest request, Long idActual) {
        empleadoRepository.findByNumeroDocumento(request.getNumeroDocumento()).ifPresent(existente -> {
            if (!existente.getId().equals(idActual)) {
                throw new BadRequestException("Ya existe un empleado con el documento: " + request.getNumeroDocumento());
            }
        });
    }
}
