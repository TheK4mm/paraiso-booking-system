package com.hotel.paraiso.security;

import com.hotel.paraiso.common.audit.ActivityLog;
import com.hotel.paraiso.common.audit.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Registra inicios y cierres de sesión en el log de actividad.
 */
@Component
@RequiredArgsConstructor
public class AuthActivityListener {

    private final ActivityLogRepository activityLogRepository;

    @EventListener
    public void onLogin(AuthenticationSuccessEvent event) {
        registrar(event.getAuthentication().getName(), "LOGIN", "Inicio de sesión");
    }

    @EventListener
    public void onLoginFallido(AbstractAuthenticationFailureEvent event) {
        registrar(event.getAuthentication().getName(), "LOGIN_FALLIDO",
                event.getException().getClass().getSimpleName());
    }

    @EventListener
    public void onLogout(LogoutSuccessEvent event) {
        registrar(event.getAuthentication().getName(), "LOGOUT", "Cierre de sesión");
    }

    private void registrar(String username, String accion, String detalle) {
        activityLogRepository.save(ActivityLog.builder()
                .username(username)
                .accion(accion)
                .detalle(detalle)
                .build());
    }
}
