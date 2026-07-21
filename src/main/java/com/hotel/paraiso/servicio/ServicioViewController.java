package com.hotel.paraiso.servicio;

import com.hotel.paraiso.common.web.CsvExporter;
import com.hotel.paraiso.servicio.Servicio.CategoriaServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/servicios")
@RequiredArgsConstructor
public class ServicioViewController {

    private final ServicioService service;

    @ModelAttribute("categorias")
    public CategoriaServicio[] categorias() {
        return CategoriaServicio.values();
    }

    @ModelAttribute("categoriaLabels")
    public Map<String, String> categoriaLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("ALIMENTACION", "Alimentación");
        labels.put("SPA_BIENESTAR", "Spa y bienestar");
        labels.put("TRANSPORTE", "Transporte");
        labels.put("ENTRETENIMIENTO", "Entretenimiento");
        labels.put("LAVANDERIA", "Lavandería");
        labels.put("NEGOCIOS", "Negocios");
        labels.put("OTROS", "Otros");
        return labels;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(required = false) CategoriaServicio categoria,
                        @RequestParam(defaultValue = "false") boolean incluirInactivos,
                        @PageableDefault(size = 15) Pageable pageable, Model model) {
        model.addAttribute("title", "Servicios");
        model.addAttribute("page", service.buscar(q, categoria, incluirInactivos, pageable));
        model.addAttribute("q", q);
        model.addAttribute("categoria", categoria);
        model.addAttribute("incluirInactivos", incluirInactivos);
        return "servicios/lista";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) CategoriaServicio categoria,
                                           @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        List<ServicioResponse> filas = service.buscar(q, categoria, incluirInactivos, PageRequest.of(0, 10_000)).getContent();
        return CsvExporter.exportar("servicios.csv",
                List.of("ID", "Nombre", "Descripción", "Precio", "Categoría", "Activo"),
                filas, s -> List.of(s.getId(), s.getNombre(), s.getDescripcion(), s.getPrecio(),
                        s.getCategoria(), Boolean.TRUE.equals(s.getActivo()) ? "Sí" : "No"));
    }

    @GetMapping("/new")
    public String crearForm(Model model) {
        model.addAttribute("title", "Nuevo servicio");
        model.addAttribute("request", new ServicioRequest());
        return "servicios/form";
    }

    @GetMapping("/{id}/edit")
    public String editarForm(@PathVariable Long id, Model model) {
        ServicioResponse s = service.findById(id);
        ServicioRequest r = new ServicioRequest();
        r.setNombre(s.getNombre());
        r.setDescripcion(s.getDescripcion());
        r.setPrecio(s.getPrecio());
        r.setCategoria(s.getCategoria());
        model.addAttribute("title", "Editar servicio");
        model.addAttribute("request", r);
        model.addAttribute("editId", id);
        return "servicios/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("request") ServicioRequest request,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Nuevo servicio");
            return "servicios/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Servicio creado correctamente.");
        return "redirect:/servicios";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("request") ServicioRequest request,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Editar servicio");
            model.addAttribute("editId", id);
            return "servicios/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Servicio actualizado correctamente.");
        return "redirect:/servicios";
    }

    @PostMapping("/{id}/delete")
    public String desactivar(@PathVariable Long id, RedirectAttributes redirect) {
        service.softDelete(id);
        redirect.addFlashAttribute("success", "Servicio desactivado.");
        return "redirect:/servicios";
    }
}
