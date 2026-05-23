package com.hotel.paraiso.controller;

import com.hotel.paraiso.dto.TipoHabitacionDTO;
import com.hotel.paraiso.service.TipoHabitacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de tipos de habitación.
 * Base URL: /api/tipos-habitacion
 */
@RestController
@RequestMapping("/api/tipos-habitacion")
@RequiredArgsConstructor
public class TipoHabitacionController {

    private final TipoHabitacionService service;

    @GetMapping
    public ResponseEntity<List<TipoHabitacionDTO.Response>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoHabitacionDTO.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<TipoHabitacionDTO.Response> create(
            @Valid @RequestBody TipoHabitacionDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoHabitacionDTO.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody TipoHabitacionDTO.Request request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
