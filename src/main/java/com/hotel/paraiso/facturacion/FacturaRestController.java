package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.web.PageResponse;
import com.hotel.paraiso.facturacion.Factura.EstadoFactura;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaRestController {

    private final FacturaService service;

    @GetMapping
    public PageResponse<FacturaResponse> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoFactura estado,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(service.buscar(q, estado, pageable));
    }

    @GetMapping("/reserva/{reservaId}")
    public FacturaResponse byReserva(@PathVariable Long reservaId) {
        return service.findByReserva(reservaId);
    }

    @GetMapping("/{id}")
    public FacturaResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<FacturaResponse> create(@Valid @RequestBody FacturaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public FacturaResponse update(@PathVariable Long id, @Valid @RequestBody FacturaRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        service.anular(id);
        return ResponseEntity.noContent().build();
    }
}
