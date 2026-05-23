package com.hotel.paraiso.service;

import java.util.List;
import java.util.Map;

/**
 * Contrato común para servicios que exponen sus datos como Map<String, Object>
 * para alimentar vistas Thymeleaf de forma genérica.
 *
 * @param <R> el tipo de DTO Response asociado al servicio
 */
public interface IViewMapService<R> {

    /** Devuelve todos los registros del recurso transformados en mapas listos para vista. */
    List<Map<String, Object>> findAllAsMap();

    /** Devuelve un registro por id transformado en mapa. */
    Map<String, Object> findByIdAsMap(Long id);
}
