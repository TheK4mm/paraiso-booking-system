package com.hotel.paraiso.portal.reserva;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Consulta de reserva para invitados por código + email. El resultado se
 * renderiza en la misma vista (el código nunca viaja en la URL) y el
 * mensaje de error es único: no distingue "código inexistente" de "email
 * ajeno" para no permitir enumerar reservas.
 */
@Controller
@RequiredArgsConstructor
public class ConsultaReservaController {

    private final ReservaPublicaService service;

    @GetMapping("/consulta-reserva")
    public String formulario() {
        return "portal/consulta";
    }

    @PostMapping("/consulta-reserva")
    public String consultar(@RequestParam String codigo, @RequestParam String email, Model model) {
        service.consultar(codigo, email).ifPresentOrElse(
                reserva -> model.addAttribute("reserva", reserva),
                () -> model.addAttribute("noEncontrada",
                        "No encontramos una reserva con esos datos. Revisa el código y el email."));
        model.addAttribute("codigo", codigo);
        model.addAttribute("email", email);
        return "portal/consulta";
    }
}
