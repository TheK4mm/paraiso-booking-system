package com.hotel.paraiso.habitacion;

import com.hotel.paraiso.common.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TipoHabitacionRepository extends BaseRepository<TipoHabitacion> {

    Optional<TipoHabitacion> findByNombreIgnoreCase(String nombre);

    @Query("SELECT COUNT(h) FROM Habitacion h WHERE h.tipoHabitacion.id = :tipoId")
    long countHabitaciones(@Param("tipoId") Long tipoId);
}
