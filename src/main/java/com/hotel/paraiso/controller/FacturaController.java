package com.hotel.paraiso.controller;

import com.hotel.paraiso.dto.FacturaDTO;
import com.hotel.paraiso.service.FacturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService service;

    @GetMapping
    public ResponseEntity<List<FacturaDTO.Response>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacturaDTO.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /** GET /api/facturas/reserva/3 */
    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<FacturaDTO.Response> getByReserva(@PathVariable Long reservaId) {
        return ResponseEntity.ok(service.findByReserva(reservaId));
    }

    @PostMapping
    public ResponseEntity<FacturaDTO.Response> create(
            @Valid @RequestBody FacturaDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacturaDTO.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody FacturaDTO.Request request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
