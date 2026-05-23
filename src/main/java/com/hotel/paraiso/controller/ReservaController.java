package com.hotel.paraiso.controller;

import com.hotel.paraiso.dto.ReservaDTO;
import com.hotel.paraiso.model.Reserva.EstadoReserva;
import com.hotel.paraiso.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de reservas.
 * Base URL: /api/reservas
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService service;

    @GetMapping
    public ResponseEntity<List<ReservaDTO.Response>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaDTO.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /** GET /api/reservas/codigo/RES-2024-000001 */
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ReservaDTO.Response> getByCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(service.findByCodigo(codigo));
    }

    /** GET /api/reservas/cliente/5 */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<ReservaDTO.Response>> getByCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.findByCliente(clienteId));
    }

    /** GET /api/reservas/estado/CONFIRMADA */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ReservaDTO.Response>> getByEstado(@PathVariable EstadoReserva estado) {
        return ResponseEntity.ok(service.findByEstado(estado));
    }

    @PostMapping
    public ResponseEntity<ReservaDTO.Response> create(
            @Valid @RequestBody ReservaDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservaDTO.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody ReservaDTO.Request request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    /**
     * Cambia el estado de una reserva.
     * PATCH /api/reservas/1/estado
     * Body: { "estado": "CONFIRMADA" }
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReservaDTO.Response> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ReservaDTO.EstadoRequest request) {
        return ResponseEntity.ok(service.cambiarEstado(id, request));
    }

    /** Cancela la reserva (soft delete con cambio de estado) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        service.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
