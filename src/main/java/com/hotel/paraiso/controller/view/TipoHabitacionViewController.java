package com.hotel.paraiso.controller.view;

import com.hotel.paraiso.dto.TipoHabitacionDTO;
import com.hotel.paraiso.service.TipoHabitacionService;
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
@RequestMapping("/tipos-habitacion")
@RequiredArgsConstructor
public class TipoHabitacionViewController {

    private static final String ENTITY_NAME = "Tipo de Habitación";
    private static final String ENTITY_PATH = "/tipos-habitacion";

    private final TipoHabitacionService service;

    @GetMapping
    public String list(Model model) {
        List<Map<String, String>> columns = List.of(
                column("id", "ID"),
                column("nombre", "Nombre"),
                column("descripcion", "Descripción"),
                column("capacidadMaxima", "Capacidad"),
                column("precioBaseNoche", "Precio/Noche"),
                column("totalHabitaciones", "Habitaciones"),
                column("activo", "Activo")
        );

        model.addAttribute("title", "Tipos de Habitación");
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
        Map<String, Object> entity = service.findByIdAsMap(id);
        prepareFormModel(model, entity, true);
        return "pages/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("entity") TipoHabitacionDTO.Request request,
                         BindingResult result, Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, null), false);
            return "pages/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Tipo de habitación creado correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("entity") TipoHabitacionDTO.Request request,
                         BindingResult result, Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, id), true);
            return "pages/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Tipo de habitación actualizado correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        service.delete(id);
        redirect.addFlashAttribute("success", "Tipo de habitación desactivado.");
        return "redirect:" + ENTITY_PATH;
    }

    private void prepareFormModel(Model model, Map<String, Object> entity, boolean isEdit) {
        List<Map<String, Object>> fields = List.of(
                field("nombre", "Nombre", "text", true),
                field("descripcion", "Descripción", "textarea", false),
                field("capacidadMaxima", "Capacidad Máxima", "number", true),
                field("precioBaseNoche", "Precio Base por Noche", "number", true),
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

    private Map<String, Object> requestToMap(TipoHabitacionDTO.Request r, Long id) {
        return TipoHabitacionDTO.Response.builder()
                .id(id)
                .nombre(r.getNombre())
                .descripcion(r.getDescripcion())
                .capacidadMaxima(r.getCapacidadMaxima())
                .precioBaseNoche(r.getPrecioBaseNoche())
                .activo(r.getActivo())
                .build()
                .toMap();
    }
}
