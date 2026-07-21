package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.crud.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PagoRepository extends BaseRepository<Pago> {

    @Override
    @EntityGraph(attributePaths = {"reserva", "factura"})
    Page<Pago> findAll(Specification<Pago> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"reserva", "factura"})
    List<Pago> findByReservaId(Long reservaId);

    /** Total aprobado de una reserva. */
    @Query("""
            SELECT COALESCE(SUM(p.monto), 0) FROM Pago p
            WHERE p.reserva.id = :reservaId AND p.estadoPago = 'APROBADO'
            """)
    BigDecimal sumPagosAprobadosByReservaId(@Param("reservaId") Long reservaId);

    /** Total aprobado excluyendo un pago concreto (para re-validar en ediciones). */
    @Query("""
            SELECT COALESCE(SUM(p.monto), 0) FROM Pago p
            WHERE p.reserva.id = :reservaId AND p.estadoPago = 'APROBADO' AND p.id <> :excluirPagoId
            """)
    BigDecimal sumPagosAprobadosByReservaIdExcluyendo(@Param("reservaId") Long reservaId,
                                                      @Param("excluirPagoId") Long excluirPagoId);
}
