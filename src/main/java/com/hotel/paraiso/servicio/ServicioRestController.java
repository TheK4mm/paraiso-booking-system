package com.hotel.paraiso.servicio;

import com.hotel.paraiso.common.web.PageResponse;
import com.hotel.paraiso.servicio.Servicio.CategoriaServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioRestController {

    private final ServicioService service;

    @GetMapping
    public PageResponse<ServicioResponse> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CategoriaServicio categoria,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(service.buscar(q, categoria, incluirInactivos, pageable));
    }

    @GetMapping("/categoria/{categoria}")
    public List<ServicioResponse> byCategoria(@PathVariable CategoriaServicio categoria) {
        return service.findByCategoria(categoria);
    }

    @GetMapping("/{id}")
    public ServicioResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ServicioResponse> create(@Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ServicioResponse update(@PathVariable Long id, @Valid @RequestBody ServicioRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
