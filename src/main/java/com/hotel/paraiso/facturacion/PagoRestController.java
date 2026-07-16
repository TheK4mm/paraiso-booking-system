package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.web.PageResponse;
import com.hotel.paraiso.facturacion.Pago.EstadoPago;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoRestController {

    private final PagoService service;

    @GetMapping
    public PageResponse<PagoResponse> buscar(
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(required = false) Long reservaId,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(service.buscar(estado, reservaId, pageable));
    }

    @GetMapping("/reserva/{reservaId}")
    public List<PagoResponse> byReserva(@PathVariable Long reservaId) {
        return service.findByReserva(reservaId);
    }

    @GetMapping("/{id}")
    public PagoResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<PagoResponse> create(@Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public PagoResponse update(@PathVariable Long id, @Valid @RequestBody PagoRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
