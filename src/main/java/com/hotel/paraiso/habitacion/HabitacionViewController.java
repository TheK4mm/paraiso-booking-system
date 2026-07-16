package com.hotel.paraiso.habitacion;

import com.hotel.paraiso.common.web.CsvExporter;
import com.hotel.paraiso.habitacion.Habitacion.EstadoHabitacion;
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
@RequestMapping("/habitaciones")
@RequiredArgsConstructor
public class HabitacionViewController {

    private final HabitacionService service;
    private final TipoHabitacionService tipoHabitacionService;

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(required = false) EstadoHabitacion estado,
                        @RequestParam(required = false) Long tipoHabitacionId,
                        @RequestParam(defaultValue = "false") boolean incluirInactivos,
                        @PageableDefault(size = 15) Pageable pageable, Model model) {
        model.addAttribute("title", "Habitaciones");
        model.addAttribute("page", service.buscar(q, estado, tipoHabitacionId, incluirInactivos, pageable));
        model.addAttribute("q", q);
        model.addAttribute("estado", estado);
        model.addAttribute("tipoHabitacionId", tipoHabitacionId);
        model.addAttribute("incluirInactivos", incluirInactivos);
        model.addAttribute("estados", EstadoHabitacion.values());
        model.addAttribute("tipos", tipoHabitacionService.findAll());
        return "habitaciones/lista";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) EstadoHabitacion estado,
                                           @RequestParam(required = false) Long tipoHabitacionId,
                                           @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        List<HabitacionResponse> filas = service
                .buscar(q, estado, tipoHabitacionId, incluirInactivos, PageRequest.of(0, 10_000)).getContent();
        return CsvExporter.exportar("habitaciones.csv",
                List.of("ID", "Número", "Piso", "Tipo", "Capacidad", "Precio/Noche", "Estado", "Activa"),
                filas, h -> List.of(h.getId(), h.getNumero(), h.getPiso(), h.getTipoHabitacionNombre(),
                        h.getCapacidadMaxima(), h.getPrecioBaseNoche(), h.getEstado(),
                        Boolean.TRUE.equals(h.getActivo()) ? "Sí" : "No"));
    }

    @GetMapping("/new")
    public String crearForm(Model model) {
        model.addAttribute("title", "Nueva habitación");
        model.addAttribute("request", new HabitacionRequest());
        addFormData(model);
        return "habitaciones/form";
    }

    @GetMapping("/{id}/edit")
    public String editarForm(@PathVariable Long id, Model model) {
        HabitacionResponse h = service.findById(id);
        HabitacionRequest r = new HabitacionRequest();
        r.setNumero(h.getNumero());
        r.setPiso(h.getPiso());
        r.setDescripcion(h.getDescripcion());
        r.setEstado(h.getEstado());
        r.setTipoHabitacionId(h.getTipoHabitacionId());
        model.addAttribute("title", "Editar habitación");
        model.addAttribute("request", r);
        model.addAttribute("editId", id);
        addFormData(model);
        return "habitaciones/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("request") HabitacionRequest request,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Nueva habitación");
            addFormData(model);
            return "habitaciones/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Habitación creada correctamente.");
        return "redirect:/habitaciones";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("request") HabitacionRequest request,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Editar habitación");
            model.addAttribute("editId", id);
            addFormData(model);
            return "habitaciones/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Habitación actualizada correctamente.");
        return "redirect:/habitaciones";
    }

    @PostMapping("/{id}/delete")
    public String desactivar(@PathVariable Long id, RedirectAttributes redirect) {
        service.softDelete(id);
        redirect.addFlashAttribute("success", "Habitación desactivada y bloqueada.");
        return "redirect:/habitaciones";
    }

    private void addFormData(Model model) {
        model.addAttribute("tipos", tipoHabitacionService.findAll());
        model.addAttribute("estados", EstadoHabitacion.values());
    }
}
