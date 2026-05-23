package com.hotel.paraiso.controller.view;

import com.hotel.paraiso.dto.EmpleadoDTO;
import com.hotel.paraiso.service.EmpleadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

import static com.hotel.paraiso.controller.view.ViewSupport.*;

@Controller
@RequestMapping("/empleados")
@RequiredArgsConstructor
public class EmpleadoViewController {

    private static final String ENTITY_NAME = "Empleado";
    private static final String ENTITY_PATH = "/empleados";

    private final EmpleadoService service;

    @GetMapping
    public String list(Model model) {
        List<Map<String, String>> columns = List.of(
                column("id", "ID"),
                column("nombreCompleto", "Nombre Completo"),
                column("numeroDocumento", "Documento"),
                column("cargo", "Cargo"),
                column("emailCorporativo", "Email"),
                column("fechaContratacion", "Contratación"),
                column("totalReservasGestionadas", "Reservas"),
                column("activo", "Activo")
        );
        model.addAttribute("title", "Empleados");
        model.addAttribute("entityName", ENTITY_NAME);
        model.addAttribute("entityPath", ENTITY_PATH);
        model.addAttribute("columns", columns);
        model.addAttribute("rows", service.findAllAsMap());
        return "pages/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        prepareFormModel(model, null, false);
        return "pages/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        prepareFormModel(model, service.findByIdAsMap(id), true);
        return "pages/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("entity") EmpleadoDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, null), false);
            return "pages/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Empleado creado correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("entity") EmpleadoDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, id), true);
            return "pages/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Empleado actualizado correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        service.delete(id);
        redirect.addFlashAttribute("success", "Empleado desactivado.");
        return "redirect:" + ENTITY_PATH;
    }

    private void prepareFormModel(Model model, Map<String, Object> entity, boolean isEdit) {
        List<Map<String, Object>> fields = List.of(
                field("nombre", "Nombre", "text", true),
                field("apellido", "Apellido", "text", true),
                field("numeroDocumento", "Número de Documento", "text", true),
                field("cargo", "Cargo", "text", true),
                field("emailCorporativo", "Email Corporativo", "email", false),
                field("telefonoExtension", "Extensión Telefónica", "text", false),
                field("fechaContratacion", "Fecha de Contratación", "date", true),
                field("activo", "Activo", "checkbox", false)
        );
        model.addAttribute("title", (isEdit ? "Editar " : "Nuevo ") + ENTITY_NAME);
        model.addAttribute("entityName", ENTITY_NAME);
        model.addAttribute("entityPath", ENTITY_PATH);
        model.addAttribute("fields", fields);
        model.addAttribute("entity", entity);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("action", isEdit && entity != null
                ? ENTITY_PATH + "/" + entity.get("id")
                : ENTITY_PATH);
    }

    private Map<String, Object> requestToMap(EmpleadoDTO.Request r, Long id) {
        return EmpleadoDTO.Response.builder()
                .id(id)
                .nombre(r.getNombre())
                .apellido(r.getApellido())
                .numeroDocumento(r.getNumeroDocumento())
                .cargo(r.getCargo())
                .emailCorporativo(r.getEmailCorporativo())
                .telefonoExtension(r.getTelefonoExtension())
                .fechaContratacion(r.getFechaContratacion())
                .activo(r.getActivo())
                .build()
                .toMap();
    }
}
