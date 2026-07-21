package com.hotel.paraiso.empleado;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class EmpleadoSpecs {

    private EmpleadoSpecs() {
    }

    /** Búsqueda libre por nombre, apellido, documento o cargo. */
    public static Specification<Empleado> texto(String q) {
        if (!StringUtils.hasText(q)) {
            return null;
        }
        String patron = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nombre")), patron),
                cb.like(cb.lower(root.get("apellido")), patron),
                cb.like(cb.lower(cb.concat(cb.concat(root.get("nombre"), " "), root.get("apellido"))), patron),
                cb.like(cb.lower(root.get("numeroDocumento")), patron),
                cb.like(cb.lower(root.get("cargo")), patron)
        );
    }
}
