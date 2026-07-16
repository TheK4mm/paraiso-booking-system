package com.hotel.paraiso.cliente;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Specifications componibles para búsqueda y filtrado de clientes.
 */
public final class ClienteSpecs {

    private ClienteSpecs() {
    }

    /** Búsqueda libre por nombre, apellido, email o número de documento. */
    public static Specification<Cliente> texto(String q) {
        if (!StringUtils.hasText(q)) {
            return null;
        }
        String patron = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nombre")), patron),
                cb.like(cb.lower(root.get("apellido")), patron),
                cb.like(cb.lower(cb.concat(cb.concat(root.get("nombre"), " "), root.get("apellido"))), patron),
                cb.like(cb.lower(root.get("email")), patron),
                cb.like(cb.lower(root.get("numeroDocumento")), patron)
        );
    }
}
