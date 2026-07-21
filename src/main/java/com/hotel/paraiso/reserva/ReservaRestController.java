package com.hotel.paraiso.reserva;

import com.hotel.paraiso.common.web.PageResponse;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
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
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaRestController {

    private final ReservaService service;

    @GetMapping
    public PageResponse<ReservaResponse> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(service.buscar(q, estado, clienteId, desde, hasta, pageable));
    }

    @GetMapping("/{id}")
    public ReservaDetalleResponse getById(@PathVariable Long id) {
        return service.findDetalle(id);
    }

    @GetMapping("/codigo/{codigo}")
    public ReservaDetalleResponse getByCodigo(@PathVariable String codigo) {
        return service.findByCodigo(codigo);
    }

    @GetMapping("/cliente/{clienteId}")
    public List<ReservaResponse> byCliente(@PathVariable Long clienteId) {
        return service.findByCliente(clienteId);
    }

    @GetMapping("/estado/{estado}")
    public List<ReservaResponse> byEstado(@PathVariable EstadoReserva estado) {
        return service.findByEstado(estado);
    }

    @PostMapping
    public ResponseEntity<ReservaDetalleResponse> create(@Valid @RequestBody ReservaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ReservaDetalleResponse update(@PathVariable Long id, @Valid @RequestBody ReservaRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/estado")
    public ReservaDetalleResponse cambiarEstado(@PathVariable Long id,
                                                @Valid @RequestBody CambioEstadoRequest request) {
        return service.cambiarEstado(id, request.getEstado());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
