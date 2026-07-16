package com.hotel.paraiso.habitacion;

import com.hotel.paraiso.common.crud.BaseRepository;
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

public interface HabitacionRepository extends BaseRepository<Habitacion> {

    Optional<Habitacion> findByNumero(String numero);

    /** Listado con el tipo cargado en la misma consulta (evita N+1). */
    @Override
    @EntityGraph(attributePaths = "tipoHabitacion")
    Page<Habitacion> findAll(Specification<Habitacion> spec, Pageable pageable);

    /**
     * Bloqueo pesimista de las habitaciones a reservar: serializa las
     * creaciones concurrentes de reservas sobre las mismas habitaciones
     * mientras se re-verifica la disponibilidad.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Habitacion h WHERE h.id IN :ids")
    List<Habitacion> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    /**
     * Habitaciones disponibles para un rango de fechas: activas, en estado
     * DISPONIBLE y sin reservas vigentes solapadas.
     */
    @EntityGraph(attributePaths = "tipoHabitacion")
    @Query("""
            SELECT h FROM Habitacion h
            WHERE h.activo = true
              AND h.estado = 'DISPONIBLE'
              AND h.id NOT IN (
                  SELECT hab.id FROM Reserva r
                  JOIN r.habitaciones hab
                  WHERE r.estado NOT IN ('CANCELADA', 'NO_SHOW', 'CHECKOUT')
                    AND r.fechaEntrada < :fechaSalida
                    AND r.fechaSalida > :fechaEntrada
              )
            """)
    List<Habitacion> findHabitacionesDisponibles(
            @Param("fechaEntrada") LocalDate fechaEntrada,
            @Param("fechaSalida") LocalDate fechaSalida
    );
}
