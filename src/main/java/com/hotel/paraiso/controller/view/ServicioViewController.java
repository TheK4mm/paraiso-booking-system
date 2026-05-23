package com.hotel.paraiso.controller.view;

import com.hotel.paraiso.dto.ServicioDTO;
import com.hotel.paraiso.model.Servicio.CategoriaServicio;
import com.hotel.paraiso.service.ServicioService;
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
@RequestMapping("/servicios")
@RequiredArgsConstructor
public class ServicioViewController {

    private static final String ENTITY_NAME = "Servicio";
    private static final String ENTITY_PATH = "/servicios";

    private final ServicioService service;

    @GetMapping
    public String list(Model model) {
        List<Map<String, String>> columns = List.of(
                column("id", "ID"),
                column("nombre", "Nombre"),
                column("descripcion", "Descripción"),
                column("precio", "Precio"),
                column("categoria", "Categoría"),
                column("activo", "Activo")
        );
        model.addAttribute("title", "Servicios");
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
    public String create(@Valid @ModelAttribute("entity") ServicioDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, null), false);
            return "pages/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Servicio creado correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("entity") ServicioDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, id), true);
            return "pages/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Servicio actualizado correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        service.delete(id);
        redirect.addFlashAttribute("success", "Servicio desactivado.");
        return "redirect:" + ENTITY_PATH;
    }

    private void prepareFormModel(Model model, Map<String, Object> entity, boolean isEdit) {
        List<Map<String, String>> categorias = Arrays.stream(CategoriaServicio.values())
                .map(c -> option(c.name(), c.name()))
                .toList();

        List<Map<String, Object>> fields = List.of(
                field("nombre", "Nombre", "text", true),
                field("descripcion", "Descripción", "textarea", false),
                field("precio", "Precio", "number", true),
                select("categoria", "Categoría", true, categorias),
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

    private Map<String, Object> requestToMap(ServicioDTO.Request r, Long id) {
        return ServicioDTO.Response.builder()
                .id(id)
                .nombre(r.getNombre())
                .descripcion(r.getDescripcion())
                .precio(r.getPrecio())
                .categoria(r.getCategoria())
                .activo(r.getActivo())
                .build()
                .toMap();
    }
}
