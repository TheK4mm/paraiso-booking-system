package com.hotel.paraiso.security;

import com.hotel.paraiso.common.exception.BadRequestException;
import com.hotel.paraiso.common.exception.BusinessException;
import com.hotel.paraiso.common.web.Peticiones;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Login, registro público de huéspedes (con verificación de email)
 * y recuperación de contraseña.
 */
@Controller
@RequiredArgsConstructor
public class AuthViewController {

    private final VerificacionEmailService verificacionEmailService;
    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    // ─── Registro de huéspedes ───

    @GetMapping("/registro")
    public String registroForm(Model model) {
        model.addAttribute("registro", new RegistroClienteRequest());
        return "auth/registro";
    }

    /**
     * Atiende el POST clásico de la página standalone y el envío AJAX del
     * modal del portal: en AJAX responde solo el fragment del pane (con los
     * errores de binding, status 422) o el mensaje de éxito, que el JS
     * intercambia dentro del modal sin recargar la página.
     */
    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registro") RegistroClienteRequest request,
                            BindingResult result, Model model, RedirectAttributes redirect,
                            HttpServletRequest http, HttpServletResponse response) {
        boolean ajax = Peticiones.esAjax(http);
        if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmarPassword")
                && !request.getPassword().equals(request.getConfirmarPassword())) {
            result.rejectValue("confirmarPassword", "no-coincide", "Las contraseñas no coinciden");
        }
        if (!result.hasErrors()) {
            try {
                verificacionEmailService.registrarCliente(request);
            } catch (BadRequestException e) {
                result.reject("registro-fallido", e.getMessage());
            }
        }
        if (result.hasErrors()) {
            if (ajax) {
                response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
                return "fragments/auth :: registroPane";
            }
            return "auth/registro";
        }
        if (ajax) {
            model.addAttribute("email", request.getEmail());
            return "fragments/auth :: registroExito";
        }
        redirect.addFlashAttribute("email", request.getEmail());
        return "redirect:/verificar-email/pendiente";
    }

    /** Pantalla "revisa tu correo", con opción de reenviar el enlace. */
    @GetMapping("/verificar-email/pendiente")
    public String verificacionPendiente() {
        return "auth/verificacion-pendiente";
    }

    @GetMapping("/verificar-email")
    public String verificarEmail(@RequestParam String token, RedirectAttributes redirect) {
        try {
            verificacionEmailService.verificar(token);
        } catch (BadRequestException | BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/verificar-email/pendiente";
        }
        redirect.addFlashAttribute("success", "Email verificado. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    @PostMapping("/verificar-email/reenviar")
    public String reenviarVerificacion(@RequestParam String email, RedirectAttributes redirect) {
        verificacionEmailService.reenviar(email);
        redirect.addFlashAttribute("success",
                "Si el email tiene una verificación pendiente, enviamos un nuevo enlace.");
        redirect.addFlashAttribute("email", email);
        return "redirect:/verificar-email/pendiente";
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
