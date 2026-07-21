package com.hotel.paraiso.servicio;

import com.hotel.paraiso.common.crud.AbstractCrudService;
import com.hotel.paraiso.common.exception.BadRequestException;
import com.hotel.paraiso.common.web.SortWhitelist;
import com.hotel.paraiso.servicio.Servicio.CategoriaServicio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Slf4j
public class ServicioService extends AbstractCrudService<Servicio, ServicioRequest, ServicioResponse> {

    private static final Set<String> ORDENABLES = Set.of("id", "nombre", "precio", "categoria", "creadoEn");

    private final ServicioRepository servicioRepository;

    public ServicioService(ServicioRepository repository, ServicioMapper mapper) {
        super(repository, mapper);
        this.servicioRepository = repository;
    }

    @Transactional(readOnly = true)
    public Page<ServicioResponse> buscar(String q, CategoriaServicio categoria,
                                         boolean incluirInactivos, Pageable pageable) {
        Specification<Servicio> spec = Specification.allOf(Stream.of(
                incluirInactivos ? null : soloActivos(),
                ServicioSpecs.texto(q),
                ServicioSpecs.conCategoria(categoria)
        ).filter(Objects::nonNull).toList());
        return buscar(spec, SortWhitelist.sanitize(pageable, ORDENABLES, Sort.by(Sort.Direction.ASC, "nombre")));
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> findByCategoria(CategoriaServicio categoria) {
        return buscar(null, categoria, false, Pageable.ofSize(200)).getContent();
    }

    @Override
    protected void beforeCreate(ServicioRequest request) {
        validarUnicidad(request, null);
    }

    @Override
    protected void beforeUpdate(Long id, ServicioRequest request, Servicio entity) {
        validarUnicidad(request, id);
    }

    @Override
    protected String entityName() {
        return "Servicio";
    }

    private void validarUnicidad(ServicioRequest request, Long idActual) {
        servicioRepository.findByNombreIgnoreCase(request.getNombre()).ifPresent(existente -> {
            if (!existente.getId().equals(idActual)) {
                throw new BadRequestException("Ya existe un servicio con el nombre: " + request.getNombre());
            }
        });
    }
}
