package com.hotel.paraiso.portal.cuenta;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import com.hotel.paraiso.reserva.ReservaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Área del huésped autenticado (rol CLIENTE, ver SecurityConfig).
 */
@Controller
@RequestMapping("/mi-cuenta")
@RequiredArgsConstructor
public class MiCuentaController {

    private static final EnumSet<EstadoReserva> ACTIVAS =
            EnumSet.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.CHECKIN);

    private final CuentaClienteService service;

    @GetMapping
    public String resumen(Authentication auth, Model model) {
        Optional<Cliente> cliente = service.clienteDe(auth.getName());
        List<ReservaResponse> reservas = service.misReservas(auth.getName());
        List<ReservaResponse> proximas = reservas.stream()
                .filter(r -> ACTIVAS.contains(r.getEstado())
                        && !r.getFechaSalida().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(ReservaResponse::getFechaEntrada))
                .toList();

        model.addAttribute("title", "Mi cuenta");
        model.addAttribute("cliente", cliente.orElse(null));
        model.addAttribute("totalReservas", reservas.size());
        model.addAttribute("proximas", proximas);
        return "mi-cuenta/index";
    }

    @GetMapping("/reservas")
    public String reservas(Authentication auth, Model model) {
        List<ReservaResponse> reservas = service.misReservas(auth.getName()).stream()
                .sorted(Comparator.comparing(ReservaResponse::getFechaEntrada).reversed())
                .toList();
        model.addAttribute("title", "Mis reservas");
        model.addAttribute("reservas", reservas);
        return "mi-cuenta/reservas";
    }

    @GetMapping("/reservas/{codigo}")
    public String detalle(@PathVariable String codigo, Authentication auth, Model model) {
        model.addAttribute("title", codigo);
        model.addAttribute("r", service.detalle(auth.getName(), codigo));
        return "mi-cuenta/detalle";
    }

    @PostMapping("/reservas/{codigo}/cancelar")
    public String cancelar(@PathVariable String codigo, Authentication auth,
                           RedirectAttributes redirect) {
        service.cancelar(auth.getName(), codigo);
        redirect.addFlashAttribute("success", "Tu reserva " + codigo + " fue cancelada.");
        return "redirect:/mi-cuenta/reservas";
    }

    @GetMapping("/perfil")
    public String perfil(Authentication auth, Model model) {
        Cliente cliente = service.clienteDe(auth.getName()).orElse(null);
        if (!model.containsAttribute("perfil")) {
            PerfilClienteRequest perfil = new PerfilClienteRequest();
            if (cliente != null) {
                perfil.setNombre(cliente.getNombre());
                perfil.setApellido(cliente.getApellido());
                perfil.setTelefono(cliente.getTelefono());
                perfil.setDireccion(cliente.getDireccion());
                perfil.setPais(cliente.getPais());
            }
            model.addAttribute("perfil", perfil);
        }
        model.addAttribute("title", "Mi perfil");
        model.addAttribute("cliente", cliente);
        return "mi-cuenta/perfil";
    }

    @PostMapping("/perfil")
    public String actualizarPerfil(@Valid @ModelAttribute("perfil") PerfilClienteRequest perfil,
                                   BindingResult result, Authentication auth, Model model,
                                   RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Mi perfil");
            model.addAttribute("cliente", service.clienteDe(auth.getName()).orElse(null));
            return "mi-cuenta/perfil";
        }
        service.actualizarPerfil(auth.getName(), perfil);
        redirect.addFlashAttribute("success", "Tu perfil fue actualizado.");
        return "redirect:/mi-cuenta/perfil";
    }
}
