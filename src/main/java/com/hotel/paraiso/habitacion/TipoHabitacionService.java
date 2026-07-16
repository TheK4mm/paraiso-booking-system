package com.hotel.paraiso.habitacion;

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
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Slf4j
public class TipoHabitacionService extends AbstractCrudService<TipoHabitacion, TipoHabitacionRequest, TipoHabitacionResponse> {

    private static final Set<String> ORDENABLES =
            Set.of("id", "nombre", "capacidadMaxima", "precioBaseNoche", "creadoEn");

    private final TipoHabitacionRepository tipoRepository;

    public TipoHabitacionService(TipoHabitacionRepository repository, TipoHabitacionMapper mapper) {
        super(repository, mapper);
        this.tipoRepository = repository;
    }

    @Transactional(readOnly = true)
    public Page<TipoHabitacionResponse> buscar(String q, boolean incluirInactivos, Pageable pageable) {
        Specification<TipoHabitacion> texto = !StringUtils.hasText(q) ? null
                : (root, query, cb) -> cb.or(
                        cb.like(cb.lower(root.get("nombre")), "%" + q.trim().toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("descripcion")), "%" + q.trim().toLowerCase() + "%"));
        Specification<TipoHabitacion> spec = Specification.allOf(Stream.of(
                incluirInactivos ? null : soloActivos(),
                texto
        ).filter(Objects::nonNull).toList());
        return buscar(spec, SortWhitelist.sanitize(pageable, ORDENABLES, Sort.by(Sort.Direction.ASC, "precioBaseNoche")));
    }

    @Override
    protected void beforeCreate(TipoHabitacionRequest request) {
        validarUnicidad(request, null);
    }

    @Override
    protected void beforeUpdate(Long id, TipoHabitacionRequest request, TipoHabitacion entity) {
        validarUnicidad(request, id);
    }

    @Override
    protected TipoHabitacionResponse toDetalle(TipoHabitacion entity) {
        return mapper.toResponse(entity).toBuilder()
                .totalHabitaciones(tipoRepository.countHabitaciones(entity.getId()))
                .build();
    }

    @Override
    protected String entityName() {
        return "TipoHabitacion";
    }

    private void validarUnicidad(TipoHabitacionRequest request, Long idActual) {
        tipoRepository.findByNombreIgnoreCase(request.getNombre()).ifPresent(existente -> {
            if (!existente.getId().equals(idActual)) {
                throw new BadRequestException("Ya existe un tipo de habitación con el nombre: " + request.getNombre());
            }
        });
    }
}
