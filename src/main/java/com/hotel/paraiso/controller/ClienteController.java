package com.hotel.paraiso.controller;

import com.hotel.paraiso.dto.ClienteDTO;
import com.hotel.paraiso.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de clientes.
 * Base URL: /api/clientes
 */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    @GetMapping
    public ResponseEntity<List<ClienteDTO.Response>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteDTO.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /** GET /api/clientes/search?termino=Juan */
    @GetMapping("/search")
    public ResponseEntity<List<ClienteDTO.Response>> search(@RequestParam String termino) {
        return ResponseEntity.ok(service.search(termino));
    }

    @PostMapping
    public ResponseEntity<ClienteDTO.Response> create(
            @Valid @RequestBody ClienteDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteDTO.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody ClienteDTO.Request request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
