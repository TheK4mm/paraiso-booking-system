package com.hotel.paraiso.portal.reserva;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.reserva.ReservaDetalleResponse;
import com.hotel.paraiso.security.RolAuthenticationSuccessHandler;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Wizard público de reservas: fechas → disponibilidad → datos → éxito.
 * La búsqueda viaja por query params; desde la selección el estado vive
 * en la sesión (sobrevive a un desvío por login/registro — ver
 * {@link RolAuthenticationSuccessHandler}).
 */
@Controller
@RequestMapping("/reservar")
@RequiredArgsConstructor
public class ReservaPublicaController {

    private static final String SESION_RESERVA = RolAuthenticationSuccessHandler.SESION_RESERVA_EN_CURSO;

    private final ReservaPublicaService service;

    @GetMapping
    public String fechas(@ModelAttribute("busqueda") BusquedaDisponibilidadRequest busqueda,
                         Model model) {
        // El binding sin @Valid conserva la búsqueda al "cambiar fechas"
        model.addAttribute("paso", 1);
        return "portal/reservar/fechas";
    }

    @GetMapping("/disponibilidad")
    public String disponibilidad(@Valid @ModelAttribute("busqueda") BusquedaDisponibilidadRequest busqueda,
                                 BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("paso", 1);
            return "portal/reservar/fechas";
        }
        model.addAttribute("habitaciones", service.buscarDisponibles(
                busqueda.getFechaEntrada(), busqueda.getFechaSalida(), busqueda.getHuespedes()));
        model.addAttribute("paso", 2);
        return "portal/reservar/disponibilidad";
    }

    @PostMapping("/seleccion")
    public String seleccionar(@RequestParam Long habitacionId,
                              @Valid @ModelAttribute("busqueda") BusquedaDisponibilidadRequest busqueda,
                              BindingResult result, HttpSession session,
                              RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", "La búsqueda ya no es válida. Elige las fechas de nuevo.");
            return "redirect:/reservar";
        }
        ReservaEnCurso enCurso = service.prepararSeleccion(habitacionId,
                busqueda.getFechaEntrada(), busqueda.getFechaSalida(), busqueda.getHuespedes());
        session.setAttribute(SESION_RESERVA, enCurso);
        return "redirect:/reservar/datos";
    }

    @GetMapping("/datos")
    public String datos(HttpSession session, Authentication auth, Model model,
                        RedirectAttributes redirect) {
        ReservaEnCurso enCurso = (ReservaEnCurso) session.getAttribute(SESION_RESERVA);
        if (enCurso == null) {
            redirect.addFlashAttribute("error", "Tu selección expiró. Empecemos de nuevo.");
            return "redirect:/reservar";
        }
        Cliente cliente = service.clienteAutenticado(auth != null ? auth.getName() : null);
        if (!model.containsAttribute("datos")) {
            DatosHuespedRequest datos = new DatosHuespedRequest();
            if (cliente != null) {
                prefijar(datos, cliente);
            }
            model.addAttribute("datos", datos);
        }
        model.addAttribute("enCurso", enCurso);
        model.addAttribute("clienteAutenticado", cliente != null);
        model.addAttribute("anonimo", auth == null);
        model.addAttribute("paso", 3);
        return "portal/reservar/datos";
    }

    @PostMapping("/confirmar")
    public String confirmar(@Valid @ModelAttribute("datos") DatosHuespedRequest datos,
                            BindingResult result, HttpSession session, Authentication auth,
                            Model model, RedirectAttributes redirect) {
        ReservaEnCurso enCurso = (ReservaEnCurso) session.getAttribute(SESION_RESERVA);
        if (enCurso == null) {
            redirect.addFlashAttribute("error", "Tu selección expiró. Empecemos de nuevo.");
            return "redirect:/reservar";
        }
        Cliente cliente = service.clienteAutenticado(auth != null ? auth.getName() : null);
        if (result.hasErrors()) {
            model.addAttribute("enCurso", enCurso);
            model.addAttribute("clienteAutenticado", cliente != null);
            model.addAttribute("anonimo", auth == null);
            model.addAttribute("paso", 3);
            return "portal/reservar/datos";
        }
        ReservaDetalleResponse reserva = service.confirmar(enCurso, datos,
                auth != null ? auth.getName() : null);
        session.removeAttribute(SESION_RESERVA);
        redirect.addFlashAttribute("reserva", reserva);
        return "redirect:/reservar/exito";
    }

    @GetMapping("/exito")
    public String exito(Model model) {
        if (!model.containsAttribute("reserva")) {
            return "redirect:/";
        }
        model.addAttribute("paso", 4);
        return "portal/reservar/exito";
    }

    private void prefijar(DatosHuespedRequest datos, Cliente cliente) {
        datos.setNombre(cliente.getNombre());
        datos.setApellido(cliente.getApellido());
        datos.setTipoDocumento(cliente.getTipoDocumento());
        datos.setNumeroDocumento(cliente.getNumeroDocumento());
        datos.setEmail(cliente.getEmail());
        datos.setTelefono(cliente.getTelefono());
    }
}
