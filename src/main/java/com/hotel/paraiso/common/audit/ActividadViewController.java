package com.hotel.paraiso.common.audit;

import com.hotel.paraiso.common.web.SortWhitelist;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Registro de actividad del sistema (solo ADMIN, protegido por ruta).
 */
@Controller
@RequestMapping("/actividad")
@RequiredArgsConstructor
public class ActividadViewController {

    private static final Set<String> ORDENABLES = Set.of("id", "username", "accion", "creadoEn");

    private final ActivityLogRepository repository;

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        @PageableDefault(size = 25) Pageable pageable, Model model) {
        Specification<ActivityLog> texto = !StringUtils.hasText(q) ? null
                : (root, query, cb) -> {
                    String patron = "%" + q.trim().toLowerCase() + "%";
                    return cb.or(
                            cb.like(cb.lower(root.get("username")), patron),
                            cb.like(cb.lower(root.get("accion")), patron),
                            cb.like(cb.lower(root.get("detalle")), patron));
                };
        Specification<ActivityLog> despues = desde == null ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("creadoEn"), desde.atStartOfDay());
        Specification<ActivityLog> antes = hasta == null ? null
                : (root, query, cb) -> cb.lessThan(root.get("creadoEn"), hasta.plusDays(1).atStartOfDay());

        Specification<ActivityLog> spec = Specification.allOf(
                Stream.of(texto, despues, antes).filter(Objects::nonNull).toList());
        Pageable saneado = SortWhitelist.sanitize(pageable, ORDENABLES, Sort.by(Sort.Direction.DESC, "id"));

        model.addAttribute("title", "Registro de actividad");
        model.addAttribute("page", repository.findAll(spec, saneado));
        model.addAttribute("q", q);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "actividad/lista";
    }
}
