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

import java.security.Principal;

/**
 * Endpoints de autenticación del portal público. No sirve NINGUNA página:
 * toda la experiencia vive en el modal de {@code fragments/auth.html}.
 *
 * <p>Los GET sobreviven solo como redirecciones al portal con el panel
 * correspondiente abierto — marcadores antiguos, enlaces de los correos y
 * el {@code loginPage} que exige el filtro de Spring Security.
 *
 * <p>Los POST responden en dos modos:
 * <ul>
 *   <li><b>AJAX</b> (modal): devuelven el fragment del panel re-renderizado
 *       con sus errores (422) o el de éxito (200); el JS lo intercambia sin
 *       recargar.</li>
 *   <li><b>Clásico</b> (sin JavaScript): redirigen al portal reenviando el
 *       BindingResult por flash, de modo que el modal se abre ya con los
 *       errores pintados.</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
public class PortalAuthController {

    private final RegistroClienteService registroClienteService;
    private final VerificacionEmailService verificacionEmailService;
    private final PasswordResetService passwordResetService;

    // ─── El módulo aislado ya no existe: solo redirecciones al portal ───

    @GetMapping("/login")
    public String login() {
        return "redirect:" + RutasAuth.PORTAL_LOGIN;
    }

    @GetMapping("/registro")
    public String registroForm() {
        return "redirect:" + RutasAuth.PORTAL_REGISTRO;
    }

    @GetMapping("/recuperar")
    public String recuperarForm() {
        return "redirect:" + RutasAuth.PORTAL_RECUPERAR;
    }

    // ─── Registro de huéspedes ───

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registro") RegistroClienteRequest request,
                            BindingResult result, Model model, RedirectAttributes redirect,
                            HttpServletRequest http, HttpServletResponse response) {
        if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmarPassword")
                && !request.getPassword().equals(request.getConfirmarPassword())) {
            result.rejectValue("confirmarPassword", "no-coincide", "Las contraseñas no coinciden");
        }
        if (!result.hasErrors()) {
            try {
                registroClienteService.registrar(request);
            } catch (BadRequestException e) {
                result.reject("registro-fallido", e.getMessage());
            }
        }
        if (result.hasErrors()) {
            return conErrores(http, response, redirect, "registro", request, result,
                    "fragments/auth :: registroPane", RutasAuth.PORTAL_REGISTRO);
        }
        if (Peticiones.esAjax(http)) {
            model.addAttribute("email", request.getEmail());
            return "fragments/auth :: registroExito";
        }
        redirect.addFlashAttribute("success", "Tu cuenta está lista. Ya puedes iniciar sesión.");
        return "redirect:" + RutasAuth.PORTAL_LOGIN;
    }

    // ─── Recuperación de contraseña ───

    @PostMapping("/recuperar")
    public String solicitarRecuperacion(@Valid @ModelAttribute("recuperar") RecuperarPasswordRequest request,
                                        BindingResult result, Model model, RedirectAttributes redirect,
                                        HttpServletRequest http, HttpServletResponse response,
                                        Principal principal) {
        if (result.hasErrors()) {
            return conErrores(http, response, redirect, "recuperar", request, result,
                    "fragments/auth :: recuperarPane", RutasAuth.PORTAL_RECUPERAR);
        }
        passwordResetService.solicitar(request.getEmail());
        if (Peticiones.esAjax(http)) {
            model.addAttribute("email", request.getEmail());
            return "fragments/auth :: recuperarExito";
        }
        redirect.addFlashAttribute("success",
                "Si el email está registrado, recibirás un enlace de restablecimiento.");
        // El cliente que la pide desde su perfil no debe acabar en el modal de login
        return "redirect:" + (principal != null ? "/mi-cuenta/perfil" : RutasAuth.PORTAL_LOGIN);
    }

    /** Destino del enlace del correo: el token pasa por flash y no queda en la URL. */
    @GetMapping("/restablecer")
    public String restablecerForm(@RequestParam String token, RedirectAttributes redirect) {
        redirect.addFlashAttribute("tokenRestablecer", token);
        return "redirect:" + RutasAuth.PORTAL_RESTABLECER;
    }

    @PostMapping("/restablecer")
    public String restablecer(@Valid @ModelAttribute("restablecer") RestablecerPasswordRequest request,
                              BindingResult result, RedirectAttributes redirect,
                              HttpServletRequest http, HttpServletResponse response) {
        if (!result.hasErrors()) {
            try {
                passwordResetService.restablecer(request.getToken(), request.getPassword(),
                        request.getConfirmarPassword());
            } catch (BadRequestException e) {
                result.reject("restablecer-fallido", e.getMessage());
            }
        }
        if (result.hasErrors()) {
            return conErrores(http, response, redirect, "restablecer", request, result,
                    "fragments/auth :: restablecerPane", RutasAuth.PORTAL_RESTABLECER);
        }
        if (Peticiones.esAjax(http)) {
            return "fragments/auth :: restablecerExito";
        }
        redirect.addFlashAttribute("success", "Contraseña restablecida. Ya puedes iniciar sesión.");
        return "redirect:" + RutasAuth.PORTAL_LOGIN;
    }

    // ─── Verificación de email (opcional: desbloquea beneficios) ───

    @GetMapping("/verificar-email")
    public String verificarEmail(@RequestParam String token, RedirectAttributes redirect) {
        try {
            verificacionEmailService.verificar(token);
        } catch (BadRequestException | BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:" + RutasAuth.PORTAL;
        }
        redirect.addFlashAttribute("success", "Email verificado. ¡Gracias!");
        return "redirect:" + RutasAuth.PORTAL;
    }

    @PostMapping("/verificar-email/reenviar")
    public String reenviarVerificacion(@RequestParam String email, RedirectAttributes redirect) {
        verificacionEmailService.reenviar(email);
        redirect.addFlashAttribute("success",
                "Si tu email tiene una verificación pendiente, te enviamos un nuevo enlace.");
        return "redirect:/mi-cuenta";
    }

    /**
     * Renderiza los errores donde corresponda: dentro del modal si la
     * petición vino por AJAX, o de vuelta en el portal por flash si el
     * usuario navega sin JavaScript.
     */
    private String conErrores(HttpServletRequest http, HttpServletResponse response,
                              RedirectAttributes redirect, String nombre, Object request,
                              BindingResult result, String fragment, String destino) {
        if (Peticiones.esAjax(http)) {
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            return fragment;
        }
        redirect.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + nombre, result);
        redirect.addFlashAttribute(nombre, request);
        return "redirect:" + destino;
    }
}
