package com.hotel.paraiso.controller;

import com.hotel.paraiso.dto.ServicioDTO;
import com.hotel.paraiso.model.Servicio.CategoriaServicio;
import com.hotel.paraiso.service.ServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService service;

    @GetMapping
    public ResponseEntity<List<ServicioDTO.Response>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioDTO.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /** GET /api/servicios/categoria/ALIMENTACION */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ServicioDTO.Response>> getByCategoria(
            @PathVariable CategoriaServicio categoria) {
        return ResponseEntity.ok(service.findByCategoria(categoria));
    }

    @PostMapping
    public ResponseEntity<ServicioDTO.Response> create(
            @Valid @RequestBody ServicioDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioDTO.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody ServicioDTO.Request request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
