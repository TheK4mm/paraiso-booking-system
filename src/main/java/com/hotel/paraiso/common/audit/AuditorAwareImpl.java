package com.hotel.paraiso.common.audit;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Resuelve el auditor actual para las columnas creado_por / actualizado_por
 * a partir del usuario autenticado; "sistema" para procesos sin sesión.
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(ActividadEventListener.usuarioActual());
    }
}
