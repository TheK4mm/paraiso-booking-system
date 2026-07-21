package com.hotel.paraiso.common.crud;

import com.hotel.paraiso.common.audit.AuditableEntity;
import com.hotel.paraiso.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD genérico para las entidades de catálogo (Cliente, Empleado,
 * Habitación, TipoHabitación, Servicio). Centraliza el esqueleto
 * repetido; la lógica específica se inyecta por hooks:
 *
 * <ul>
 *   <li>{@code beforeCreate}/{@code beforeUpdate}: validaciones (unicidad, reglas)</li>
 *   <li>{@code applyRelations}: resolución de asociaciones a partir de IDs del request</li>
 *   <li>{@code toDetalle}: enriquecer la respuesta de detalle (conteos, derivados)</li>
 * </ul>
 *
 * Reserva, Pago y Factura NO usan esta base: su lógica de dominio
 * (máquina de estados, saldos, locks) exige servicios dedicados.
 */
@Transactional(readOnly = true)
public abstract class AbstractCrudService<E extends AuditableEntity & Activable, REQ, RES> {

    protected final BaseRepository<E> repository;
    protected final CrudMapper<E, REQ, RES> mapper;

    protected AbstractCrudService(BaseRepository<E> repository, CrudMapper<E, REQ, RES> mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Page<RES> buscar(Specification<E> spec, Pageable pageable) {
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    /** Listado simple de registros activos (compatibilidad API). */
    public List<RES> findAll() {
        return repository.findAll(soloActivos()).stream().map(mapper::toResponse).toList();
    }

    public RES findById(Long id) {
        return toDetalle(getOrThrow(id));
    }

    public E getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(entityName(), "id", id));
    }

    @Transactional
    public RES create(REQ request) {
        beforeCreate(request);
        E entity = mapper.toEntity(request);
        applyRelations(entity, request);
        E saved = repository.save(entity);
        return toDetalle(saved);
    }

    @Transactional
    public RES update(Long id, REQ request) {
        E entity = getOrThrow(id);
        beforeUpdate(id, request, entity);
        mapper.updateEntity(request, entity);
        applyRelations(entity, request);
        return toDetalle(repository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void softDelete(Long id) {
        E entity = getOrThrow(id);
        beforeSoftDelete(entity);
        entity.setActivo(false);
        repository.save(entity);
    }

    // ─── Hooks ────────────────────────────────────────────────────────

    protected void beforeCreate(REQ request) {
    }

    protected void beforeUpdate(Long id, REQ request, E entity) {
    }

    protected void applyRelations(E entity, REQ request) {
    }

    protected void beforeSoftDelete(E entity) {
    }

    protected RES toDetalle(E entity) {
        return mapper.toResponse(entity);
    }

    protected Specification<E> soloActivos() {
        return (root, query, cb) -> cb.isTrue(root.get("activo"));
    }

    protected abstract String entityName();
}
