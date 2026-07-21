package com.hotel.paraiso.portal;

import com.hotel.paraiso.security.RecuperarPasswordRequest;
import com.hotel.paraiso.security.RegistroClienteRequest;
import com.hotel.paraiso.security.RestablecerPasswordRequest;
import com.hotel.paraiso.security.RutasAuth;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Atributos que necesita el modal de autenticación de {@code layout/public}:
 * los objetos a los que se enlazan sus formularios y el panel que debe
 * aparecer abierto.
 *
 * <p>Es global a propósito. El modal se abre desde cualquier página pública
 * y, sobre todo, es el destino de {@code /?auth=…} tras un fallo de login o
 * un enlace de correo; acotarlo a unos pocos controllers hacía que añadir
 * una página pública nueva reventara al resolver {@code ${registro}}.
 *
 * <p>Estos métodos NO pisan lo que llegue por flash: {@code ModelFactory}
 * omite el atributo si el modelo ya lo contiene, que es como el flujo sin
 * JavaScript devuelve los formularios con sus errores.
 */
@ControllerAdvice
public class PortalAuthModelAdvice {

    @ModelAttribute("registro")
    public RegistroClienteRequest registro() {
        return new RegistroClienteRequest();
    }

    @ModelAttribute("recuperar")
    public RecuperarPasswordRequest recuperar() {
        return new RecuperarPasswordRequest();
    }

    @ModelAttribute("restablecer")
    public RestablecerPasswordRequest restablecer() {
        return new RestablecerPasswordRequest();
    }

    /** Panel a mostrar abierto, o null para dejar el modal cerrado. */
    @ModelAttribute("panelAuth")
    public String panelAuth(@RequestParam(name = "auth", required = false) String auth) {
        return auth != null && RutasAuth.PANELES.contains(auth) ? auth : null;
    }
}
