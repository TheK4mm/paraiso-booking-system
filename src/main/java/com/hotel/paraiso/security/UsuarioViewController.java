package com.hotel.paraiso.security;

import com.hotel.paraiso.common.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Administración de cuentas (solo ADMIN, protegido por ruta en SecurityConfig).
 */
@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioViewController {

    private final UsuarioService service;

    @ModelAttribute("roles")
    public Rol[] roles() {
        // Solo roles internos: las cuentas CLIENTE nacen del registro público
        return Rol.internos();
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @PageableDefault(size = 15) Pageable pageable, Model model) {
        model.addAttribute("title", "Usuarios");
        model.addAttribute("page", service.buscar(q, pageable));
        model.addAttribute("q", q);
        return "usuarios/lista";
    }

    @GetMapping("/new")
    public String crearForm(Model model) {
        model.addAttribute("title", "Nuevo usuario");
        model.addAttribute("request", new UsuarioAdminRequest());
        return "usuarios/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("request") UsuarioAdminRequest request,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Nuevo usuario");
            return "usuarios/form";
        }
        try {
            service.crearPorAdmin(request);
        } catch (BadRequestException e) {
            result.reject("usuario-duplicado", e.getMessage());
            model.addAttribute("title", "Nuevo usuario");
            return "usuarios/form";
        }
        redirect.addFlashAttribute("success", "Usuario creado correctamente.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/toggle")
    public String toggleActivo(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        boolean activo = service.toggleActivo(id, auth.getName());
        redirect.addFlashAttribute("success", activo ? "Usuario activado." : "Usuario desactivado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/rol")
    public String cambiarRol(@PathVariable Long id, @RequestParam Rol rol,
                             Authentication auth, RedirectAttributes redirect) {
        service.cambiarRol(id, rol, auth.getName());
        redirect.addFlashAttribute("success", "Rol actualizado a " + rol + ".");
        return "redirect:/usuarios";
    }
}
