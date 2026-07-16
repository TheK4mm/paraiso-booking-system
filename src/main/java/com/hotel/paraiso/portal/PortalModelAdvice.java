package com.hotel.paraiso.portal;

import com.hotel.paraiso.portal.reserva.ConsultaReservaController;
import com.hotel.paraiso.portal.reserva.ReservaPublicaController;
import com.hotel.paraiso.security.RegistroClienteRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Atributos que necesitan todas las páginas públicas: el modal de
 * autenticación de layout/public renderiza el formulario de registro
 * (th:object="${registro}") en cada vista para visitantes anónimos.
 * Acotado a los controllers públicos: el back-office y /mi-cuenta no
 * cargan el modal y no pagan este atributo.
 */
@ControllerAdvice(assignableTypes = {
        PortalController.class,
        ReservaPublicaController.class,
        ConsultaReservaController.class
})
public class PortalModelAdvice {

    @ModelAttribute("registro")
    public RegistroClienteRequest registro() {
        return new RegistroClienteRequest();
    }
}
