package com.hotel.paraiso.controller.view;

import com.hotel.paraiso.dto.HabitacionDTO;
import com.hotel.paraiso.dto.TipoHabitacionDTO;
import com.hotel.paraiso.model.Habitacion.EstadoHabitacion;
import com.hotel.paraiso.service.HabitacionService;
import com.hotel.paraiso.service.TipoHabitacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.hotel.paraiso.controller.view.ViewSupport.*;

@Controller
@RequestMapping("/habitaciones")
@RequiredArgsConstructor
public class HabitacionViewController {

    private static final String ENTITY_NAME = "Habitación";
    private static final String ENTITY_PATH = "/habitaciones";

    private final HabitacionService service;
    private final TipoHabitacionService tipoHabitacionService;

    @GetMapping
    public String list(Model model) {
        List<Map<String, String>> columns = List.of(
                column("id", "ID"),
                column("numero", "Número"),
                column("piso", "Piso"),
                column("tipoHabitacionNombre", "Tipo"),
                column("capacidadMaxima", "Capacidad"),
                column("precioBaseNoche", "Precio/Noche"),
                column("estado", "Estado"),
                column("activo", "Activo")
        );
        model.addAttribute("title", "Habitaciones");
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
    public String create(@Valid @ModelAttribute("entity") HabitacionDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, null), false);
            return "pages/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Habitación creada correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("entity") HabitacionDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, id), true);
            return "pages/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Habitación actualizada correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        service.delete(id);
        redirect.addFlashAttribute("success", "Habitación desactivada.");
        return "redirect:" + ENTITY_PATH;
    }

    private void prepareFormModel(Model model, Map<String, Object> entity, boolean isEdit) {
        List<Map<String, String>> tipos = tipoHabitacionService.findAll().stream()
                .map(t -> option(String.valueOf(t.getId()), t.getNombre()))
                .toList();
        List<Map<String, String>> estados = Arrays.stream(EstadoHabitacion.values())
                .map(e -> option(e.name(), e.name()))
                .toList();

        List<Map<String, Object>> fields = List.of(
                field("numero", "Número", "text", true),
                field("piso", "Piso", "number", true),
                field("descripcion", "Descripción", "textarea", false),
                select("tipoHabitacionId", "Tipo de Habitación", true, tipos),
                select("estado", "Estado", false, estados),
                field("activo", "Activo", "checkbox", false)
        );
        model.addAttribute("title", (isEdit ? "Editar " : "Nueva ") + ENTITY_NAME);
        model.addAttribute("entityName", ENTITY_NAME);
        model.addAttribute("entityPath", ENTITY_PATH);
        model.addAttribute("fields", fields);
        model.addAttribute("entity", entity);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("action", isEdit && entity != null
                ? ENTITY_PATH + "/" + entity.get("id")
                : ENTITY_PATH);
    }

    private Map<String, Object> requestToMap(HabitacionDTO.Request r, Long id) {
        TipoHabitacionDTO.Response tipo = r.getTipoHabitacionId() != null
                ? tipoHabitacionService.findById(r.getTipoHabitacionId())
                : null;
        return HabitacionDTO.Response.builder()
                .id(id)
                .numero(r.getNumero())
                .piso(r.getPiso())
                .descripcion(r.getDescripcion())
                .estado(r.getEstado())
                .activo(r.getActivo())
                .tipoHabitacionId(r.getTipoHabitacionId())
                .tipoHabitacionNombre(tipo != null ? tipo.getNombre() : null)
                .precioBaseNoche(tipo != null ? tipo.getPrecioBaseNoche() : null)
                .capacidadMaxima(tipo != null ? tipo.getCapacidadMaxima() : null)
                .build()
                .toMap();
    }
}
