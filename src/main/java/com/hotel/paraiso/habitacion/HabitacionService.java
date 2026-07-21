package com.hotel.paraiso.habitacion;

import com.hotel.paraiso.common.crud.AbstractCrudService;
import com.hotel.paraiso.common.exception.BadRequestException;
import com.hotel.paraiso.common.exception.ResourceNotFoundException;
import com.hotel.paraiso.common.web.SortWhitelist;
import com.hotel.paraiso.habitacion.Habitacion.EstadoHabitacion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Slf4j
public class HabitacionService extends AbstractCrudService<Habitacion, HabitacionRequest, HabitacionResponse> {

    private static final Set<String> ORDENABLES = Set.of("id", "numero", "piso", "estado", "creadoEn");

    private final HabitacionRepository habitacionRepository;
    private final TipoHabitacionRepository tipoHabitacionRepository;
    private final HabitacionMapper habitacionMapper;

    public HabitacionService(HabitacionRepository repository,
                             TipoHabitacionRepository tipoHabitacionRepository,
                             HabitacionMapper mapper) {
        super(repository, mapper);
        this.habitacionRepository = repository;
        this.tipoHabitacionRepository = tipoHabitacionRepository;
        this.habitacionMapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<HabitacionResponse> buscar(String q, EstadoHabitacion estado, Long tipoHabitacionId,
                                           boolean incluirInactivos, Pageable pageable) {
        Specification<Habitacion> spec = Specification.allOf(Stream.of(
                incluirInactivos ? null : soloActivos(),
                HabitacionSpecs.texto(q),
                HabitacionSpecs.conEstado(estado),
                HabitacionSpecs.deTipo(tipoHabitacionId)
        ).filter(Objects::nonNull).toList());
        return buscar(spec, SortWhitelist.sanitize(pageable, ORDENABLES, Sort.by(Sort.Direction.ASC, "numero")));
    }

    @Transactional(readOnly = true)
    public List<HabitacionResponse> findDisponibles(LocalDate entrada, LocalDate salida) {
        if (entrada == null || salida == null || !entrada.isBefore(salida)) {
            throw new BadRequestException("El rango de fechas es inválido: la entrada debe ser anterior a la salida");
        }
        return habitacionRepository.findHabitacionesDisponibles(entrada, salida)
                .stream().map(habitacionMapper::toResponse).toList();
    }

    @Override
    protected void beforeCreate(HabitacionRequest request) {
        validarUnicidad(request, null);
    }

    @Override
    protected void beforeUpdate(Long id, HabitacionRequest request, Habitacion entity) {
        validarUnicidad(request, id);
    }

    @Override
    protected void applyRelations(Habitacion entity, HabitacionRequest request) {
        TipoHabitacion tipo = tipoHabitacionRepository.findById(request.getTipoHabitacionId())
                .orElseThrow(() -> new ResourceNotFoundException("TipoHabitacion", "id", request.getTipoHabitacionId()));
        entity.setTipoHabitacion(tipo);
    }

    /** El soft-delete además bloquea la habitación para que no aparezca disponible. */
    @Override
    protected void beforeSoftDelete(Habitacion entity) {
        entity.setEstado(EstadoHabitacion.BLOQUEADA);
    }

    @Override
    protected String entityName() {
        return "Habitacion";
    }

    private void validarUnicidad(HabitacionRequest request, Long idActual) {
        habitacionRepository.findByNumero(request.getNumero()).ifPresent(existente -> {
            if (!existente.getId().equals(idActual)) {
                throw new BadRequestException("Ya existe una habitación con el número: " + request.getNumero());
            }
        });
    }
}
