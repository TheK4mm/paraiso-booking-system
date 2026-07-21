package com.hotel.paraiso.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Persiste los eventos de actividad DESPUÉS del commit de la transacción
 * de negocio: las operaciones que hacen rollback no dejan rastro falso.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActividadEventListener {

    private final ActivityLogRepository activityLogRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(ActividadEvent event) {
        activityLogRepository.save(ActivityLog.builder()
                .username(usuarioActual())
                .accion(event.accion())
                .tipoEntidad(event.tipoEntidad())
                .entidadId(event.entidadId())
                .detalle(event.detalle())
                .build());
    }

    static String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "sistema";
        }
        return auth.getName();
    }
}
