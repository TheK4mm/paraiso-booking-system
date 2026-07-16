package com.hotel.paraiso.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Atributos disponibles en todas las vistas Thymeleaf:
 *  - currentPath: resalta el ítem activo del sidebar.
 *  - queryBase / queryBaseSort: query string vigente sin page (y sin sort)
 *    para que paginación y ordenamiento conserven los filtros aplicados.
 */
@ControllerAdvice
public class GlobalViewAttributes {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("queryBase")
    public String queryBase(HttpServletRequest request) {
        return sinParametros(request, "page");
    }

    @ModelAttribute("queryBaseSort")
    public String queryBaseSort(HttpServletRequest request) {
        return sinParametros(request, "page", "sort");
    }

    private String sinParametros(HttpServletRequest request, String... excluidos) {
        String qs = request.getQueryString();
        if (qs == null || qs.isBlank()) {
            return "?";
        }
        String filtrado = Arrays.stream(qs.split("&"))
                .filter(par -> Arrays.stream(excluidos).noneMatch(ex -> par.startsWith(ex + "=")))
                .collect(Collectors.joining("&"));
        return filtrado.isBlank() ? "?" : "?" + filtrado + "&";
    }
}
