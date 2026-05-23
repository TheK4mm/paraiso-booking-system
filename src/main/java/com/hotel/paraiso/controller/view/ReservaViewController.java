package com.hotel.paraiso.controller.view;

import com.hotel.paraiso.dto.ReservaDTO;
import com.hotel.paraiso.service.ClienteService;
import com.hotel.paraiso.service.EmpleadoService;
import com.hotel.paraiso.service.HabitacionService;
import com.hotel.paraiso.service.ReservaService;
import com.hotel.paraiso.service.ServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.hotel.paraiso.controller.view.ViewSupport.*;

@Controller
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaViewController {

    private static final String ENTITY_NAME = "Reserva";
    private static final String ENTITY_PATH = "/reservas";

    private final ReservaService service;
    private final ClienteService clienteService;
    private final EmpleadoService empleadoService;
    private final HabitacionService habitacionService;
    private final ServicioService servicioService;

    @GetMapping
    public String list(Model model) {
        List<Map<String, String>> columns = List.of(
                column("id", "ID"),
                column("codigoReserva", "Código"),
                column("clienteNombreCompleto", "Cliente"),
                column("fechaEntrada", "Entrada"),
                column("fechaSalida", "Salida"),
                column("totalNoches", "Noches"),
                column("numeroHuespedes", "Huéspedes"),
                column("precioTotal", "Total"),
                column("estado", "Estado")
        );
        model.addAttribute("title", "Reservas");
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
        prepareFormModel(model, enrichEntityForEdit(id), true);
        return "pages/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("entity") ReservaDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, null), false);
            return "pages/form";
        }
        service.create(request);
        redirect.addFlashAttribute("success", "Reserva creada correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("entity") ReservaDTO.Request request,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepareFormModel(model, requestToMap(request, id), true);
            return "pages/form";
        }
        service.update(id, request);
        redirect.addFlashAttribute("success", "Reserva actualizada correctamente.");
        return "redirect:" + ENTITY_PATH;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        service.cancel(id);
        redirect.addFlashAttribute("success", "Reserva cancelada.");
        return "redirect:" + ENTITY_PATH;
    }

    private void prepareFormModel(Model model, Map<String, Object> entity, boolean isEdit) {
        List<Map<String, String>> clientes = clienteService.findAll().stream()
                .map(c -> option(String.valueOf(c.getId()), c.getNombreCompleto()))
                .toList();
        List<Map<String, String>> empleados = empleadoService.findAll().stream()
                .map(e -> option(String.valueOf(e.getId()), e.getNombreCompleto()))
                .toList();
        List<Map<String, String>> habitaciones = habitacionService.findAll().stream()
                .map(h -> option(String.valueOf(h.getId()), "Hab. " + h.getNumero() + " - " + h.getTipoHabitacionNombre()))
                .toList();
        List<Map<String, String>> servicios = servicioService.findAll().stream()
                .map(s -> option(String.valueOf(s.getId()), s.getNombre()))
                .toList();

        List<Map<String, Object>> fields = List.of(
                field("fechaEntrada", "Fecha de Entrada", "date", true),
                field("fechaSalida", "Fecha de Salida", "date", true),
                field("numeroHuespedes", "Número de Huéspedes", "number", true),
                select("clienteId", "Cliente", true, clientes),
                select("empleadoId", "Empleado", false, empleados),
                multiselect("habitacionIds", "Habitaciones", true, habitaciones),
                multiselect("servicioIds", "Servicios", false, servicios),
                field("observaciones", "Observaciones", "textarea", false)
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

    /** Combina el Response con los IDs de habitaciones/servicios para precargar el form. */
    private Map<String, Object> enrichEntityForEdit(Long id) {
        ReservaDTO.Response r = service.findById(id);
        Map<String, Object> map = r.toMap();
        map.put("habitacionIds", r.getHabitaciones() == null ? List.of() :
                r.getHabitaciones().stream().map(h -> h.getId()).toList());
        map.put("servicioIds", r.getServicios() == null ? List.of() :
                r.getServicios().stream().map(s -> s.getId()).toList());
        return map;
    }

    private Map<String, Object> requestToMap(ReservaDTO.Request r, Long id) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("fechaEntrada", r.getFechaEntrada());
        map.put("fechaSalida", r.getFechaSalida());
        map.put("numeroHuespedes", r.getNumeroHuespedes());
        map.put("observaciones", r.getObservaciones());
        map.put("clienteId", r.getClienteId());
        map.put("empleadoId", r.getEmpleadoId());
        map.put("habitacionIds", r.getHabitacionIds());
        map.put("servicioIds", r.getServicioIds());
        return map;
    }
}
