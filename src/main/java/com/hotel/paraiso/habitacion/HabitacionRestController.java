package com.hotel.paraiso.habitacion;

import com.hotel.paraiso.common.web.PageResponse;
import com.hotel.paraiso.habitacion.Habitacion.EstadoHabitacion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/habitaciones")
@RequiredArgsConstructor
public class HabitacionRestController {

    private final HabitacionService service;

    @GetMapping
    public PageResponse<HabitacionResponse> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoHabitacion estado,
            @RequestParam(required = false) Long tipoHabitacionId,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(service.buscar(q, estado, tipoHabitacionId, incluirInactivos, pageable));
    }

    /** GET /api/habitaciones/disponibles?entrada=2026-08-01&salida=2026-08-05 */
    @GetMapping("/disponibles")
    public List<HabitacionResponse> disponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida) {
        return service.findDisponibles(entrada, salida);
    }

    @GetMapping("/{id}")
    public HabitacionResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<HabitacionResponse> create(@Valid @RequestBody HabitacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public HabitacionResponse update(@PathVariable Long id, @Valid @RequestBody HabitacionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
