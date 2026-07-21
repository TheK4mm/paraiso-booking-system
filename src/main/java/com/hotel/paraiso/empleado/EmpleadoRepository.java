package com.hotel.paraiso.empleado;

import com.hotel.paraiso.common.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmpleadoRepository extends BaseRepository<Empleado> {

    Optional<Empleado> findByNumeroDocumento(String numeroDocumento);

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.empleado.id = :empleadoId")
    long countReservasGestionadas(@Param("empleadoId") Long empleadoId);
}
