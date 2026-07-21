package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.web.CsvExporter;
import com.hotel.paraiso.facturacion.Pago.EstadoPago;
import com.hotel.paraiso.facturacion.Pago.MetodoPago;
import com.hotel.paraiso.reserva.ReservaService;
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
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoViewController {

    private final PagoService service;
    private final ReservaService reservaService;
    private final FacturaService facturaService;

    @ModelAttribute("metodoLabels")
    public Map<String, String> metodoLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("EFECTIVO", "Efectivo");
        labels.put("TARJETA_CREDITO", "Tarjeta de crédito");
        labels.put("TARJETA_DEBITO", "Tarjeta de débito");
        labels.put("TRANSFERENCIA", "Transferencia");
        labels.put("PSE", "PSE");
        labels.put("NEQUI", "Nequi");
        labels.put("DAVIPLATA", "Daviplata");
        return labels;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) EstadoPago estado,
                        @RequestParam(required = false) Long reservaId,
                        @PageableDefault(size = 15) Pageable pageable, Model model) {
        model.addAttribute("title", "Pagos");
        model.addAttribute("page", service.buscar(estado, reservaId, pageable));
        model.addAttribute("estado", estado);
        model.addAttribute("reservaId", reservaId);
        model.addAttribute("estados", EstadoPago.values());
        return "pagos/lista";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) EstadoPago estado,
                                           @RequestParam(required = false) Long reservaId) {
        List<PagoResponse> filas = service.buscar(estado, reservaId, PageRequest.of(0, 10_000)).getContent();
        return CsvExporter.exportar("pagos.csv",
                List.of("ID", "Reserva", "Monto", "Método", "Estado", "Referencia", "Fecha"),
                filas, p -> List.of(p.getId(), p.getCodigoReserva(), p.getMonto(), p.getMetodoPago(),
                        p.getEstadoPago(), p.getReferenciaTransaccion(), p.getFechaPago()));
    }

    @GetMapping("/new")
    public String crearForm(@RequestParam(required = false) Long reservaId, Model model) {
        PagoRequest request = new PagoRequest();
        request.setReservaId(reservaId);
        model.addAttribute("title", "Registrar pago");
        model.addAttribute("request", request);
        addFormData(model);
        return "pagos/form";
    }

    @GetMapping("/{id}/edit")
    public String editarForm(@PathVariable Long id, Model model) {
        PagoResponse p = service.findById(id);
        PagoRequest r = new PagoRequest();
        r.setMonto(p.getMonto());
        r.setMetodoPago(p.getMetodoPago());
        r.setReferenciaTransaccion(p.getReferenciaTransaccion());
        r.setDescripcion(p.getDescripcion());
        r.setReservaId(p.getReservaId());
        r.setFacturaId(p.getFacturaId());
        model.addAttribute("title", "Editar pago");
        model.addAttribute("request", r);
        model.addAttribute("editId", id);
        addFormData(model);
        return "pagos/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("request") PagoRequest request,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Registrar pago");
            addFormData(model);
            return "pagos/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Pago registrado correctamente.");
        return "redirect:/reservas/" + request.getReservaId();
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("request") PagoRequest request,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Editar pago");
            model.addAttribute("editId", id);
            addFormData(model);
            return "pagos/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Pago actualizado correctamente.");
        return "redirect:/pagos";
    }

    @PostMapping("/{id}/delete")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirect) {
        service.cancelar(id);
        redirect.addFlashAttribute("success", "Pago cancelado.");
        return "redirect:/pagos";
    }

    private void addFormData(Model model) {
        model.addAttribute("reservas",
                reservaService.buscar(null, null, null, null, null, PageRequest.of(0, 200)).getContent());
        model.addAttribute("facturas", facturaService.buscar(null, null, PageRequest.of(0, 200)).getContent());
        model.addAttribute("metodos", MetodoPago.values());
    }
}
