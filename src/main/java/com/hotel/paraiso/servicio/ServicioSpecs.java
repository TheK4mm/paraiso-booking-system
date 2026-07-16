package com.hotel.paraiso.servicio;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ServicioSpecs {

    private ServicioSpecs() {
    }

    /** Búsqueda libre por nombre o descripción. */
    public static Specification<Servicio> texto(String q) {
        if (!StringUtils.hasText(q)) {
            return null;
        }
        String patron = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nombre")), patron),
                cb.like(cb.lower(root.get("descripcion")), patron)
        );
    }

    public static Specification<Servicio> conCategoria(Servicio.CategoriaServicio categoria) {
        if (categoria == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("categoria"), categoria);
    }
}
