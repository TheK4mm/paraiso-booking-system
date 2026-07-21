package com.hotel.paraiso.common.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envoltorio estable de paginación para la API REST (evita exponer la
 * estructura interna de {@link Page}, cuya serialización no es contrato).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
