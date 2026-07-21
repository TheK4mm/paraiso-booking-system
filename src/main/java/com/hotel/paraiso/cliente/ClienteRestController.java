package com.hotel.paraiso.cliente;

import com.hotel.paraiso.common.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST de clientes. Listado paginado con búsqueda libre (?q=).
 */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteRestController {

    private final ClienteService service;

    @GetMapping
    public PageResponse<ClienteResponse> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(service.buscar(q, incluirInactivos, pageable));
    }

    /** Compatibilidad con la API v1: búsqueda simple sin paginar. */
    @GetMapping("/search")
    public List<ClienteResponse> search(@RequestParam String termino) {
        return service.buscar(termino, false, Pageable.ofSize(50)).getContent();
    }

    @GetMapping("/{id}")
    public ClienteResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> create(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ClienteResponse update(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
