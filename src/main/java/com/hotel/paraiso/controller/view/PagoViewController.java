package com.hotel.paraiso.controller.view;

import com.hotel.paraiso.dto.PagoDTO;
import com.hotel.paraiso.model.Pago.MetodoPago;
import com.hotel.paraiso.service.PagoService;
import com.hotel.paraiso.service.ReservaService;
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
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoViewController {

    private static final String ENTITY_NAME = "Pago";
    private static final String ENTITY_PATH = "/pagos";

    private final PagoService service;
    private final ReservaService reservaService;

    @GetMapping
    public String list(Model model) {
        List<Map<String, String>> columns = List.of(
                column("id", "ID"),
                column("monto", "Monto"),
                column("metodoPago", "Método"),
                column("estadoPago", "Estado"),
                column("codigoReserva", "Reserva"),
                column("referenciaTransaccion", "Referencia"),
                column("fechaPago", "Fecha")
        );
        model.addAttribute("title", "Pagos");
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
    public String create(@Valid @ModelAttribute("entity") PagoDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, null), false);
            return "pages/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Pago registrado correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("entity") PagoDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, id), true);
            return "pages/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Pago actualizado correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        service.delete(id);
        redirect.addFlashAttribute("success", "Pago cancelado.");
        return "redirect:" + ENTITY_PATH;
    }

    private void prepareFormModel(Model model, Map<String, Object> entity, boolean isEdit) {
        List<Map<String, String>> reservas = reservaService.findAll().stream()
                .map(r -> option(String.valueOf(r.getId()), r.getCodigoReserva()))
                .toList();
        List<Map<String, String>> metodos = Arrays.stream(MetodoPago.values())
                .map(m -> option(m.name(), m.name()))
                .toList();

        List<Map<String, Object>> fields = List.of(
                field("monto", "Monto", "number", true),
                select("metodoPago", "Método de Pago", true, metodos),
                field("referenciaTransaccion", "Referencia", "text", false),
                field("descripcion", "Descripción", "textarea", false),
                select("reservaId", "Reserva", true, reservas)
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

    private Map<String, Object> requestToMap(PagoDTO.Request r, Long id) {
        return PagoDTO.Response.builder()
                .id(id)
                .monto(r.getMonto())
                .metodoPago(r.getMetodoPago())
                .referenciaTransaccion(r.getReferenciaTransaccion())
                .descripcion(r.getDescripcion())
                .reservaId(r.getReservaId())
                .facturaId(r.getFacturaId())
                .build()
                .toMap();
    }
}
