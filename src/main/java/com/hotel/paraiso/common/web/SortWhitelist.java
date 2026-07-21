package com.hotel.paraiso.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Sanea el parámetro sort recibido del cliente contra una lista blanca de
 * propiedades ordenables por listado; evita exponer ordenamiento arbitrario
 * (y errores 500 por propiedades inexistentes) a JPA.
 */
public final class SortWhitelist {

    private static final int MAX_PAGE_SIZE = 200;

    private SortWhitelist() {
    }

    public static Pageable sanitize(Pageable pageable, Set<String> permitidas, Sort porDefecto) {
        Sort saneado = Sort.by(pageable.getSort().stream()
                .filter(order -> permitidas.contains(order.getProperty()))
                .toList());
        if (saneado.isEmpty()) {
            saneado = porDefecto;
        }
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, saneado);
    }
}
