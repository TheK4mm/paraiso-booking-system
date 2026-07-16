package com.hotel.paraiso.common.crud;

/**
 * Contrato de soft-delete: las entidades de catálogo exponen un flag
 * `activo` en lugar de eliminarse físicamente.
 */
public interface Activable {

    Boolean getActivo();

    void setActivo(Boolean activo);
}
