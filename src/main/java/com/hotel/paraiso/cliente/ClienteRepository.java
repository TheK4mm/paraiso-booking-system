package com.hotel.paraiso.cliente;

import com.hotel.paraiso.common.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepository extends BaseRepository<Cliente> {

    Optional<Cliente> findByEmailIgnoreCase(String email);

    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.cliente.id = :clienteId")
    long countReservas(@Param("clienteId") Long clienteId);
}
