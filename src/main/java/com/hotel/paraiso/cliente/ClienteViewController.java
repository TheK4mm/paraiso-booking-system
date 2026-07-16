package com.hotel.paraiso.cliente;

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
import java.util.Map;

@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteViewController {

    private final ClienteService service;

    @ModelAttribute("tiposDocumento")
    public List<Map<String, String>> tiposDocumento() {
        return List.of(
                Map.of("value", "CC", "label", "Cédula de Ciudadanía"),
                Map.of("value", "CE", "label", "Cédula de Extranjería"),
                Map.of("value", "PASAPORTE", "label", "Pasaporte"),
                Map.of("value", "NIT", "label", "NIT"));
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "false") boolean incluirInactivos,
                        @PageableDefault(size = 15) Pageable pageable, Model model) {
        model.addAttribute("title", "Clientes");
        model.addAttribute("page", service.buscar(q, incluirInactivos, pageable));
        model.addAttribute("q", q);
        model.addAttribute("incluirInactivos", incluirInactivos);
        return "clientes/lista";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        List<ClienteResponse> filas = service.buscar(q, incluirInactivos, PageRequest.of(0, 10_000)).getContent();
        return CsvExporter.exportar("clientes.csv",
                List.of("ID", "Nombre", "Apellido", "Tipo Doc.", "Documento", "Email", "Teléfono", "País", "Activo"),
                filas, c -> List.of(c.getId(), c.getNombre(), c.getApellido(), c.getTipoDocumento(),
                        c.getNumeroDocumento(), c.getEmail(), c.getTelefono(), c.getPais(),
                        Boolean.TRUE.equals(c.getActivo()) ? "Sí" : "No"));
    }

    @GetMapping("/new")
    public String crearForm(Model model) {
        model.addAttribute("title", "Nuevo cliente");
        model.addAttribute("request", new ClienteRequest());
        return "clientes/form";
    }

    @GetMapping("/{id}/edit")
    public String editarForm(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Editar cliente");
        model.addAttribute("request", toRequest(service.findById(id)));
        model.addAttribute("editId", id);
        return "clientes/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("request") ClienteRequest request,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Nuevo cliente");
            return "clientes/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Cliente creado correctamente.");
        return "redirect:/clientes";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("request") ClienteRequest request,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Editar cliente");
            model.addAttribute("editId", id);
            return "clientes/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Cliente actualizado correctamente.");
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/delete")
    public String desactivar(@PathVariable Long id, RedirectAttributes redirect) {
        service.softDelete(id);
        redirect.addFlashAttribute("success", "Cliente desactivado.");
        return "redirect:/clientes";
    }

    private ClienteRequest toRequest(ClienteResponse c) {
        ClienteRequest r = new ClienteRequest();
        r.setNombre(c.getNombre());
        r.setApellido(c.getApellido());
        r.setTipoDocumento(c.getTipoDocumento());
        r.setNumeroDocumento(c.getNumeroDocumento());
        r.setEmail(c.getEmail());
        r.setTelefono(c.getTelefono());
        r.setDireccion(c.getDireccion());
        r.setPais(c.getPais());
        return r;
    }
}
