package com.hotel.paraiso.service;

import com.hotel.paraiso.dto.TipoHabitacionDTO;

/**
 * Interfaz "categoría" del proyecto, asociada a TipoHabitacion
 * (la entidad que actúa como categoría de las habitaciones).
 *
 * Define el método de exposición en formato Map para la vista
 * y unifica las operaciones CRUD principales del servicio.
 */
public interface ICategoryService extends IViewMapService<TipoHabitacionDTO.Response> {

    /** Alias semántico de {@link #findAllAsMap()} para entidades de categoría. */
    default java.util.List<java.util.Map<String, Object>> getAllCategoriesAsMap() {
        return findAllAsMap();
    }
}
