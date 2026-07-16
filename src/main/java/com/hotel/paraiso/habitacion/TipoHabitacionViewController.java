package com.hotel.paraiso.habitacion;

import com.hotel.paraiso.common.web.CsvExporter;
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

import java.util.List;

@Controller
@RequestMapping("/tipos-habitacion")
@RequiredArgsConstructor
public class TipoHabitacionViewController {

    private final TipoHabitacionService service;

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "false") boolean incluirInactivos,
                        @PageableDefault(size = 15) Pageable pageable, Model model) {
        model.addAttribute("title", "Tipos de habitación");
        model.addAttribute("page", service.buscar(q, incluirInactivos, pageable));
        model.addAttribute("q", q);
        model.addAttribute("incluirInactivos", incluirInactivos);
        return "tipos-habitacion/lista";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        List<TipoHabitacionResponse> filas = service.buscar(q, incluirInactivos, PageRequest.of(0, 10_000)).getContent();
        return CsvExporter.exportar("tipos-habitacion.csv",
                List.of("ID", "Nombre", "Descripción", "Capacidad", "Precio/Noche", "Activo"),
                filas, t -> List.of(t.getId(), t.getNombre(), t.getDescripcion(), t.getCapacidadMaxima(),
                        t.getPrecioBaseNoche(), Boolean.TRUE.equals(t.getActivo()) ? "Sí" : "No"));
    }

    @GetMapping("/new")
    public String crearForm(Model model) {
        model.addAttribute("title", "Nuevo tipo de habitación");
        model.addAttribute("request", new TipoHabitacionRequest());
        return "tipos-habitacion/form";
    }

    @GetMapping("/{id}/edit")
    public String editarForm(@PathVariable Long id, Model model) {
        TipoHabitacionResponse t = service.findById(id);
        TipoHabitacionRequest r = new TipoHabitacionRequest();
        r.setNombre(t.getNombre());
        r.setDescripcion(t.getDescripcion());
        r.setCapacidadMaxima(t.getCapacidadMaxima());
        r.setPrecioBaseNoche(t.getPrecioBaseNoche());
        model.addAttribute("title", "Editar tipo de habitación");
        model.addAttribute("request", r);
        model.addAttribute("editId", id);
        return "tipos-habitacion/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("request") TipoHabitacionRequest request,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Nuevo tipo de habitación");
            return "tipos-habitacion/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Tipo de habitación creado correctamente.");
        return "redirect:/tipos-habitacion";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("request") TipoHabitacionRequest request,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Editar tipo de habitación");
            model.addAttribute("editId", id);
            return "tipos-habitacion/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Tipo de habitación actualizado correctamente.");
        return "redirect:/tipos-habitacion";
    }

    @PostMapping("/{id}/delete")
    public String desactivar(@PathVariable Long id, RedirectAttributes redirect) {
        service.softDelete(id);
        redirect.addFlashAttribute("success", "Tipo de habitación desactivado.");
        return "redirect:/tipos-habitacion";
    }
}
