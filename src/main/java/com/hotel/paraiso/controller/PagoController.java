package com.hotel.paraiso.controller;

import com.hotel.paraiso.dto.PagoDTO;
import com.hotel.paraiso.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService service;

    @GetMapping
    public ResponseEntity<List<PagoDTO.Response>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoDTO.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /** GET /api/pagos/reserva/3 */
    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<List<PagoDTO.Response>> getByReserva(@PathVariable Long reservaId) {
        return ResponseEntity.ok(service.findByReserva(reservaId));
    }

    @PostMapping
    public ResponseEntity<PagoDTO.Response> create(
            @Valid @RequestBody PagoDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PagoDTO.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody PagoDTO.Request request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
