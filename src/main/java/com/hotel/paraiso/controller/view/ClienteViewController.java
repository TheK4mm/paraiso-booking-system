package com.hotel.paraiso.controller.view;

import com.hotel.paraiso.dto.ClienteDTO;
import com.hotel.paraiso.service.ClienteService;
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
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteViewController {

    private static final String ENTITY_NAME = "Cliente";
    private static final String ENTITY_PATH = "/clientes";

    private final ClienteService service;

    @GetMapping
    public String list(Model model) {
        List<Map<String, String>> columns = List.of(
                column("id", "ID"),
                column("nombreCompleto", "Nombre Completo"),
                column("tipoDocumento", "Tipo Doc."),
                column("numeroDocumento", "N° Documento"),
                column("email", "Email"),
                column("telefono", "Teléfono"),
                column("pais", "País"),
                column("activo", "Activo")
        );
        model.addAttribute("title", "Clientes");
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
    public String create(@Valid @ModelAttribute("entity") ClienteDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, null), false);
            return "pages/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Cliente creado correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("entity") ClienteDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, id), true);
            return "pages/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Cliente actualizado correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        service.delete(id);
        redirect.addFlashAttribute("success", "Cliente desactivado.");
        return "redirect:" + ENTITY_PATH;
    }

    private void prepareFormModel(Model model, Map<String, Object> entity, boolean isEdit) {
        List<Map<String, String>> tiposDoc = List.of(
                option("CC", "Cédula de Ciudadanía"),
                option("CE", "Cédula de Extranjería"),
                option("PASAPORTE", "Pasaporte"),
                option("NIT", "NIT")
        );
        List<Map<String, Object>> fields = List.of(
                field("nombre", "Nombre", "text", true),
                field("apellido", "Apellido", "text", true),
                select("tipoDocumento", "Tipo de Documento", true, tiposDoc),
                field("numeroDocumento", "Número de Documento", "text", true),
                field("email", "Email", "email", true),
                field("telefono", "Teléfono", "text", false),
                field("direccion", "Dirección", "text", false),
                field("pais", "País", "text", false)
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

    private Map<String, Object> requestToMap(ClienteDTO.Request r, Long id) {
        return ClienteDTO.Response.builder()
                .id(id)
                .nombre(r.getNombre())
                .apellido(r.getApellido())
                .tipoDocumento(r.getTipoDocumento())
                .numeroDocumento(r.getNumeroDocumento())
                .email(r.getEmail())
                .telefono(r.getTelefono())
                .direccion(r.getDireccion())
                .pais(r.getPais())
                .build()
                .toMap();
    }
}
