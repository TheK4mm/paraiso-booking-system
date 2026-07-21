package com.hotel.paraiso.security;

import com.hotel.paraiso.common.crud.BaseRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface UsuarioRepository extends BaseRepository<Usuario> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    /**
     * Carga la ficha de cliente en la misma consulta: el portal la usa
     * fuera de la transacción del servicio (controladores y vistas).
     */
    @EntityGraph(attributePaths = "cliente")
    Optional<Usuario> findConClienteByUsernameIgnoreCase(String username);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByClienteId(Long clienteId);

    Optional<Usuario> findByClienteId(Long clienteId);
}
