package com.hotel.paraiso.empleado;

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
@RequestMapping("/empleados")
@RequiredArgsConstructor
public class EmpleadoViewController {

    private final EmpleadoService service;

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "false") boolean incluirInactivos,
                        @PageableDefault(size = 15) Pageable pageable, Model model) {
        model.addAttribute("title", "Empleados");
        model.addAttribute("page", service.buscar(q, incluirInactivos, pageable));
        model.addAttribute("q", q);
        model.addAttribute("incluirInactivos", incluirInactivos);
        return "empleados/lista";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        List<EmpleadoResponse> filas = service.buscar(q, incluirInactivos, PageRequest.of(0, 10_000)).getContent();
        return CsvExporter.exportar("empleados.csv",
                List.of("ID", "Nombre", "Apellido", "Documento", "Cargo", "Email", "Extensión", "Contratación", "Activo"),
                filas, e -> List.of(e.getId(), e.getNombre(), e.getApellido(), e.getNumeroDocumento(), e.getCargo(),
                        e.getEmailCorporativo(), e.getTelefonoExtension(), e.getFechaContratacion(),
                        Boolean.TRUE.equals(e.getActivo()) ? "Sí" : "No"));
    }

    @GetMapping("/new")
    public String crearForm(Model model) {
        model.addAttribute("title", "Nuevo empleado");
        model.addAttribute("request", new EmpleadoRequest());
        return "empleados/form";
    }

    @GetMapping("/{id}/edit")
    public String editarForm(@PathVariable Long id, Model model) {
        EmpleadoResponse e = service.findById(id);
        EmpleadoRequest r = new EmpleadoRequest();
        r.setNombre(e.getNombre());
        r.setApellido(e.getApellido());
        r.setNumeroDocumento(e.getNumeroDocumento());
        r.setCargo(e.getCargo());
        r.setEmailCorporativo(e.getEmailCorporativo());
        r.setTelefonoExtension(e.getTelefonoExtension());
        r.setFechaContratacion(e.getFechaContratacion());
        model.addAttribute("title", "Editar empleado");
        model.addAttribute("request", r);
        model.addAttribute("editId", id);
        return "empleados/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("request") EmpleadoRequest request,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Nuevo empleado");
            return "empleados/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Empleado creado correctamente.");
        return "redirect:/empleados";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("request") EmpleadoRequest request,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Editar empleado");
            model.addAttribute("editId", id);
            return "empleados/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Empleado actualizado correctamente.");
        return "redirect:/empleados";
    }

    @PostMapping("/{id}/delete")
    public String desactivar(@PathVariable Long id, RedirectAttributes redirect) {
        service.softDelete(id);
        redirect.addFlashAttribute("success", "Empleado desactivado.");
        return "redirect:/empleados";
    }
}
