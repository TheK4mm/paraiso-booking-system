package com.hotel.paraiso.controller.view;

import com.hotel.paraiso.dto.FacturaDTO;
import com.hotel.paraiso.service.FacturaService;
import com.hotel.paraiso.service.ReservaService;
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
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturaViewController {

    private static final String ENTITY_NAME = "Factura";
    private static final String ENTITY_PATH = "/facturas";

    private final FacturaService service;
    private final ReservaService reservaService;

    @GetMapping
    public String list(Model model) {
        List<Map<String, String>> columns = List.of(
                column("id", "ID"),
                column("numeroFactura", "N° Factura"),
                column("codigoReserva", "Reserva"),
                column("subtotal", "Subtotal"),
                column("descuento", "Descuento"),
                column("impuestoValor", "Impuestos"),
                column("total", "Total"),
                column("estadoFactura", "Estado"),
                column("fechaEmision", "Emisión")
        );
        model.addAttribute("title", "Facturas");
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
    public String create(@Valid @ModelAttribute("entity") FacturaDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, null), false);
            return "pages/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Factura creada correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("entity") FacturaDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, id), true);
            return "pages/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Factura actualizada correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        service.delete(id);
        redirect.addFlashAttribute("success", "Factura anulada.");
        return "redirect:" + ENTITY_PATH;
    }

    private void prepareFormModel(Model model, Map<String, Object> entity, boolean isEdit) {
        List<Map<String, String>> reservas = reservaService.findAll().stream()
                .map(r -> option(String.valueOf(r.getId()), r.getCodigoReserva()))
                .toList();

        List<Map<String, Object>> fields = List.of(
                select("reservaId", "Reserva", true, reservas),
                field("descuento", "Descuento", "number", false),
                field("impuestoPorcentaje", "Impuesto (%)", "number", false),
                field("notas", "Notas", "textarea", false)
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

    private Map<String, Object> requestToMap(FacturaDTO.Request r, Long id) {
        return FacturaDTO.Response.builder()
                .id(id)
                .reservaId(r.getReservaId())
                .descuento(r.getDescuento())
                .impuestoPorcentaje(r.getImpuestoPorcentaje())
                .notas(r.getNotas())
                .build()
                .toMap();
    }
}
