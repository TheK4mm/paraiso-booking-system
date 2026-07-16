package com.hotel.paraiso.common.crud;

import org.mapstruct.MappingTarget;

/**
 * Contrato que implementan los mappers MapStruct de cada módulo.
 * AbstractCrudService delega en él todo el mapeo entidad ↔ DTO.
 */
public interface CrudMapper<E, REQ, RES> {

    E toEntity(REQ request);

    RES toResponse(E entity);

    void updateEntity(REQ request, @MappingTarget E entity);
}
