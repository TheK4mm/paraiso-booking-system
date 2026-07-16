package com.hotel.paraiso.habitacion;

import com.hotel.paraiso.common.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tipos-habitacion")
@RequiredArgsConstructor
public class TipoHabitacionRestController {

    private final TipoHabitacionService service;

    @GetMapping
    public PageResponse<TipoHabitacionResponse> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(service.buscar(q, incluirInactivos, pageable));
    }

    @GetMapping("/{id}")
    public TipoHabitacionResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<TipoHabitacionResponse> create(@Valid @RequestBody TipoHabitacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public TipoHabitacionResponse update(@PathVariable Long id, @Valid @RequestBody TipoHabitacionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
