package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.web.CsvExporter;
import com.hotel.paraiso.facturacion.Factura.EstadoFactura;
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

import java.util.List;

@Controller
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturaViewController {

    private final FacturaService service;
    private final PagoService pagoService;
    private final ReservaService reservaService;

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(required = false) EstadoFactura estado,
                        @PageableDefault(size = 15) Pageable pageable, Model model) {
        model.addAttribute("title", "Facturas");
        model.addAttribute("page", service.buscar(q, estado, pageable));
        model.addAttribute("q", q);
        model.addAttribute("estado", estado);
        model.addAttribute("estados", EstadoFactura.values());
        return "facturas/lista";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) EstadoFactura estado) {
        List<FacturaResponse> filas = service.buscar(q, estado, PageRequest.of(0, 10_000)).getContent();
        return CsvExporter.exportar("facturas.csv",
                List.of("ID", "Número", "Reserva", "Subtotal", "Descuento", "IVA %", "IVA", "Total", "Estado", "Emisión"),
                filas, f -> List.of(f.getId(), f.getNumeroFactura(), f.getCodigoReserva(), f.getSubtotal(),
                        f.getDescuento(), f.getImpuestoPorcentaje(), f.getImpuestoValor(), f.getTotal(),
                        f.getEstadoFactura(), f.getFechaEmision()));
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        FacturaResponse factura = service.findById(id);
        model.addAttribute("title", "Factura " + factura.getNumeroFactura());
        model.addAttribute("f", factura);
        model.addAttribute("pagos", pagoService.findByReserva(factura.getReservaId()));
        return "facturas/detalle";
    }

    @GetMapping("/new")
    public String crearForm(@RequestParam(required = false) Long reservaId, Model model) {
        FacturaRequest request = new FacturaRequest();
        request.setReservaId(reservaId);
        model.addAttribute("title", "Generar factura");
        model.addAttribute("request", request);
        addFormData(model);
        return "facturas/form";
    }

    @GetMapping("/{id}/edit")
    public String editarForm(@PathVariable Long id, Model model) {
        FacturaResponse f = service.findById(id);
        FacturaRequest r = new FacturaRequest();
        r.setReservaId(f.getReservaId());
        r.setDescuento(f.getDescuento());
        r.setImpuestoPorcentaje(f.getImpuestoPorcentaje());
        r.setNotas(f.getNotas());
        model.addAttribute("title", "Editar factura");
        model.addAttribute("request", r);
        model.addAttribute("editId", id);
        addFormData(model);
        return "facturas/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("request") FacturaRequest request,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Generar factura");
            addFormData(model);
            return "facturas/form";
        }
        FacturaResponse creada = service.create(request);
        redirect.addFlashAttribute("success", "Factura " + creada.getNumeroFactura() + " generada correctamente.");
        return "redirect:/facturas/" + creada.getId();
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("request") FacturaRequest request,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Editar factura");
            model.addAttribute("editId", id);
            addFormData(model);
            return "facturas/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Factura actualizada correctamente.");
        return "redirect:/facturas/" + id;
    }

    @PostMapping("/{id}/delete")
    public String anular(@PathVariable Long id, RedirectAttributes redirect) {
        service.anular(id);
        redirect.addFlashAttribute("success", "Factura anulada.");
        return "redirect:/facturas";
    }

    private void addFormData(Model model) {
        model.addAttribute("reservas",
                reservaService.buscar(null, null, null, null, null, PageRequest.of(0, 200)).getContent());
    }
}
