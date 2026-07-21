package com.hotel.paraiso.servicio;

import com.hotel.paraiso.common.crud.BaseRepository;

import java.util.Optional;

public interface ServicioRepository extends BaseRepository<Servicio> {

    Optional<Servicio> findByNombreIgnoreCase(String nombre);
}
