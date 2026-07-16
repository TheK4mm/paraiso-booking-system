package com.hotel.paraiso.habitacion;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class HabitacionSpecs {

    private HabitacionSpecs() {
    }

    /** Búsqueda libre por número, descripción o nombre del tipo. */
    public static Specification<Habitacion> texto(String q) {
        if (!StringUtils.hasText(q)) {
            return null;
        }
        String patron = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("numero")), patron),
                cb.like(cb.lower(root.get("descripcion")), patron),
                cb.like(cb.lower(root.get("tipoHabitacion").get("nombre")), patron)
        );
    }

    public static Specification<Habitacion> conEstado(Habitacion.EstadoHabitacion estado) {
        if (estado == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Habitacion> deTipo(Long tipoHabitacionId) {
        if (tipoHabitacionId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("tipoHabitacion").get("id"), tipoHabitacionId);
    }
}
