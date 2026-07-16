package com.hotel.paraiso.portal;

import com.hotel.paraiso.habitacion.TipoHabitacionService;
import com.hotel.paraiso.portal.reserva.BusquedaDisponibilidadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Landing page pública del hotel. Sustituye al antiguo redirect de "/"
 * al dashboard: el personal llega al panel vía el success handler del
 * login o el enlace "Panel" de la barra pública.
 */
@Controller
@RequiredArgsConstructor
public class PortalController {

    private final TipoHabitacionService tipoHabitacionService;

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("tipos", tipoHabitacionService.findAll());
        model.addAttribute("busqueda", new BusquedaDisponibilidadRequest());
        return "portal/landing";
    }
}
