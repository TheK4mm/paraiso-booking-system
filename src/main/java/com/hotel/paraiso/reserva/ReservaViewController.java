package com.hotel.paraiso.reserva;

import com.hotel.paraiso.cliente.ClienteService;
import com.hotel.paraiso.common.web.CsvExporter;
import com.hotel.paraiso.empleado.EmpleadoService;
import com.hotel.paraiso.habitacion.HabitacionResponse;
import com.hotel.paraiso.habitacion.HabitacionService;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import com.hotel.paraiso.servicio.ServicioResponse;
import com.hotel.paraiso.servicio.ServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaViewController {

    private final ReservaService service;
    private final CalendarioService calendarioService;
    private final ClienteService clienteService;
    private final EmpleadoService empleadoService;
    private final HabitacionService habitacionService;
    private final ServicioService servicioService;

    /** Vista mensual del calendario de ocupación. */
    @GetMapping("/calendario")
    public String calendario(@RequestParam(required = false) Integer anio,
                             @RequestParam(required = false) Integer mes, Model model) {
        YearMonth ym = (anio != null && mes != null) ? YearMonth.of(anio, mes) : YearMonth.now();
        model.addAttribute("title", "Calendario de reservas");
        model.addAttribute("ym", ym);
        model.addAttribute("anterior", ym.minusMonths(1));
        model.addAttribute("siguiente", ym.plusMonths(1));
        model.addAttribute("nombreMes",
                ym.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("es"))
                        + " " + ym.getYear());
        model.addAttribute("semanas", calendarioService.mes(ym));
        return "reservas/calendario";
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(required = false) EstadoReserva estado,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        @PageableDefault(size = 15) Pageable pageable, Model model) {
        model.addAttribute("title", "Reservas");
        model.addAttribute("page", service.buscar(q, estado, null, desde, hasta, pageable));
        model.addAttribute("q", q);
        model.addAttribute("estado", estado);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("estados", EstadoReserva.values());
        return "reservas/lista";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) EstadoReserva estado,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<ReservaResponse> filas = service.buscar(q, estado, null, desde, hasta, PageRequest.of(0, 10_000)).getContent();
        return CsvExporter.exportar("reservas.csv",
                List.of("ID", "Código", "Cliente", "Documento", "Entrada", "Salida", "Noches",
                        "Huéspedes", "Total", "Estado", "Creada"),
                filas, r -> List.of(r.getId(), r.getCodigoReserva(), r.getClienteNombreCompleto(),
                        r.getClienteDocumento(), r.getFechaEntrada(), r.getFechaSalida(), r.getTotalNoches(),
                        r.getNumeroHuespedes(), r.getPrecioTotal(), r.getEstado(), r.getCreadoEn()));
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        ReservaDetalleResponse reserva = service.findDetalle(id);
        model.addAttribute("title", "Reserva " + reserva.getCodigoReserva());
        model.addAttribute("r", reserva);
        return "reservas/detalle";
    }

    @GetMapping("/new")
    public String crearForm(Model model) {
        model.addAttribute("title", "Nueva reserva");
        model.addAttribute("request", new ReservaRequest());
        addFormData(model);
        return "reservas/form";
    }

    @GetMapping("/{id}/edit")
    public String editarForm(@PathVariable Long id, Model model) {
        ReservaDetalleResponse d = service.findDetalle(id);
        ReservaRequest r = new ReservaRequest();
        r.setFechaEntrada(d.getFechaEntrada());
        r.setFechaSalida(d.getFechaSalida());
        r.setNumeroHuespedes(d.getNumeroHuespedes());
        r.setObservaciones(d.getObservaciones());
        r.setClienteId(d.getClienteId());
        r.setEmpleadoId(d.getEmpleadoId());
        r.setHabitacionIds(d.getHabitaciones().stream().map(HabitacionResponse::getId).toList());
        r.setServicioIds(d.getServicios().stream().map(ServicioResponse::getId).toList());
        model.addAttribute("title", "Editar reserva");
        model.addAttribute("request", r);
        model.addAttribute("editId", id);
        addFormData(model);
        return "reservas/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("request") ReservaRequest request,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Nueva reserva");
            addFormData(model);
            return "reservas/form";
        }
        ReservaDetalleResponse creada = service.create(request);
        redirect.addFlashAttribute("success", "Reserva " + creada.getCodigoReserva() + " creada correctamente.");
        return "redirect:/reservas/" + creada.getId();
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("request") ReservaRequest request,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Editar reserva");
            model.addAttribute("editId", id);
            addFormData(model);
            return "reservas/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Reserva actualizada correctamente.");
        return "redirect:/reservas/" + id;
    }

    /** Transiciones de estado desde los botones contextuales del detalle. */
    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam EstadoReserva estado,
                                RedirectAttributes redirect) {
        service.cambiarEstado(id, estado);
        redirect.addFlashAttribute("success", "Estado actualizado a " + estado + ".");
        return "redirect:/reservas/" + id;
    }

    @PostMapping("/{id}/delete")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirect) {
        service.cancelar(id);
        redirect.addFlashAttribute("success", "Reserva cancelada.");
        return "redirect:/reservas";
    }

    private void addFormData(Model model) {
        model.addAttribute("clientes", clienteService.findAll());
        model.addAttribute("empleados", empleadoService.findAll());
        model.addAttribute("habitaciones", habitacionService.findAll());
        model.addAttribute("servicios", servicioService.findAll());
    }
}
