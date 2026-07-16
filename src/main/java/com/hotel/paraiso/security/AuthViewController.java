package com.hotel.paraiso.security;

import com.hotel.paraiso.common.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Login, registro público y recuperación de contraseña.
 */
@Controller
@RequiredArgsConstructor
public class AuthViewController {

    private final UsuarioService usuarioService;
    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registroForm(Model model) {
        model.addAttribute("registro", new RegistroRequest());
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registro") RegistroRequest request,
                            BindingResult result, RedirectAttributes redirect) {
        if (!result.hasErrors() && !request.getPassword().equals(request.getConfirmarPassword())) {
            result.rejectValue("confirmarPassword", "no-coincide", "Las contraseñas no coinciden");
        }
        if (result.hasErrors()) {
            return "auth/registro";
        }
        try {
            usuarioService.registrar(request);
        } catch (BadRequestException e) {
            result.reject("registro-fallido", e.getMessage());
            return "auth/registro";
        }
        redirect.addFlashAttribute("success", "Cuenta creada correctamente. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    @GetMapping("/recuperar")
    public String recuperarForm() {
        return "auth/recuperar";
    }

    @PostMapping("/recuperar")
    public String solicitarRecuperacion(@RequestParam String email, RedirectAttributes redirect) {
        passwordResetService.solicitar(email);
        redirect.addFlashAttribute("success",
                "Si el email está registrado, recibirás un enlace de restablecimiento.");
        return "redirect:/login";
    }

    @GetMapping("/restablecer")
    public String restablecerForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/restablecer";
    }

    @PostMapping("/restablecer")
    public String restablecer(@RequestParam String token,
                              @RequestParam String password,
                              @RequestParam String confirmarPassword,
                              Model model, RedirectAttributes redirect) {
        try {
            passwordResetService.restablecer(token, password, confirmarPassword);
        } catch (BadRequestException e) {
            model.addAttribute("token", token);
            model.addAttribute("error", e.getMessage());
            return "auth/restablecer";
        }
        redirect.addFlashAttribute("success", "Contraseña restablecida. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }
}
