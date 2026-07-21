package com.hotel.paraiso.reserva;

import com.hotel.paraiso.common.crud.BaseRepository;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends BaseRepository<Reserva> {

    /** Listado con cliente y empleado cargados en la misma consulta (evita N+1). */
    @Override
    @EntityGraph(attributePaths = {"cliente", "empleado"})
    Page<Reserva> findAll(Specification<Reserva> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "empleado"})
    Optional<Reserva> findByCodigoReserva(String codigoReserva);

    @EntityGraph(attributePaths = {"cliente", "empleado"})
    List<Reserva> findByClienteId(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "empleado"})
    List<Reserva> findByEstado(EstadoReserva estado);

    /** Bloqueo pesimista para la validación de saldo de pagos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserva r WHERE r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);

    /**
     * Reservas vigentes que solapan el rango de fechas para una habitación,
     * excluyendo una reserva concreta (para ediciones; usar -1 al crear).
     */
    @Query("""
            SELECT COUNT(r) FROM Reserva r
            JOIN r.habitaciones h
            WHERE h.id = :habitacionId
              AND r.id <> :excluirReservaId
              AND r.estado NOT IN ('CANCELADA', 'NO_SHOW', 'CHECKOUT')
              AND r.fechaEntrada < :fechaSalida
              AND r.fechaSalida > :fechaEntrada
            """)
    long countReservasActivasParaHabitacion(
            @Param("habitacionId") Long habitacionId,
            @Param("fechaEntrada") LocalDate fechaEntrada,
            @Param("fechaSalida") LocalDate fechaSalida,
            @Param("excluirReservaId") Long excluirReservaId
    );

    // ─── Carga de detalle en 3 consultas constantes (colecciones List no
    //     permiten fetch simultáneo; el contexto de persistencia unifica) ───

    @Query("""
            SELECT DISTINCT r FROM Reserva r
            LEFT JOIN FETCH r.habitaciones h
            LEFT JOIN FETCH h.tipoHabitacion
            LEFT JOIN FETCH r.cliente
            LEFT JOIN FETCH r.empleado
            WHERE r.id = :id
            """)
    Optional<Reserva> findByIdConHabitaciones(@Param("id") Long id);

    @Query("SELECT DISTINCT r FROM Reserva r LEFT JOIN FETCH r.servicios WHERE r.id = :id")
    Optional<Reserva> findByIdConServicios(@Param("id") Long id);

    @Query("SELECT DISTINCT r FROM Reserva r LEFT JOIN FETCH r.pagos LEFT JOIN FETCH r.factura WHERE r.id = :id")
    Optional<Reserva> findByIdConPagosYFactura(@Param("id") Long id);

    /** Reservas visibles en el calendario que solapan el rango de la grilla. */
    @EntityGraph(attributePaths = "cliente")
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.estado NOT IN ('CANCELADA', 'NO_SHOW')
              AND r.fechaEntrada <= :hasta
              AND r.fechaSalida > :desde
            """)
    List<Reserva> findParaCalendario(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Siguiente valor de la secuencia de códigos de reserva (atómico). */
    @Query(value = "SELECT nextval('seq_codigo_reserva')", nativeQuery = true)
    long nextCodigoSeq();
}
