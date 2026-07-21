package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.crud.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface FacturaRepository extends BaseRepository<Factura> {

    @Override
    @EntityGraph(attributePaths = "reserva")
    Page<Factura> findAll(Specification<Factura> spec, Pageable pageable);

    @EntityGraph(attributePaths = "reserva")
    Optional<Factura> findByReservaId(Long reservaId);

    boolean existsByReservaId(Long reservaId);

    /** Siguiente valor de la secuencia de numeración de facturas (atómico). */
    @Query(value = "SELECT nextval('seq_numero_factura')", nativeQuery = true)
    long nextNumeroSeq();
}
