package com.hotel.paraiso.reserva;

import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

public final class ReservaSpecs {

    private ReservaSpecs() {
    }

    /** Búsqueda libre por código o datos del cliente. */
    public static Specification<Reserva> texto(String q) {
        if (!StringUtils.hasText(q)) {
            return null;
        }
        String patron = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("codigoReserva")), patron),
                cb.like(cb.lower(root.get("cliente").get("nombre")), patron),
                cb.like(cb.lower(root.get("cliente").get("apellido")), patron),
                cb.like(cb.lower(root.get("cliente").get("numeroDocumento")), patron)
        );
    }

    public static Specification<Reserva> conEstado(EstadoReserva estado) {
        if (estado == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Reserva> deCliente(Long clienteId) {
        if (clienteId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("cliente").get("id"), clienteId);
    }

    public static Specification<Reserva> entradaDesde(LocalDate desde) {
        if (desde == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaEntrada"), desde);
    }

    public static Specification<Reserva> entradaHasta(LocalDate hasta) {
        if (hasta == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaEntrada"), hasta);
    }
}
