package com.hotel.paraiso.service;

import com.hotel.paraiso.dto.PagoDTO;
import com.hotel.paraiso.exception.BusinessException;
import com.hotel.paraiso.exception.ResourceNotFoundException;
import com.hotel.paraiso.model.Factura;
import com.hotel.paraiso.model.Pago;
import com.hotel.paraiso.model.Reserva;
import com.hotel.paraiso.repository.FacturaRepository;
import com.hotel.paraiso.repository.PagoRepository;
import com.hotel.paraiso.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PagoService implements IViewMapService<PagoDTO.Response> {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final FacturaRepository facturaRepository;

    public List<PagoDTO.Response> findAll() {
        return pagoRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PagoDTO.Response findById(Long id) {
        return toResponse(getPagoOrThrow(id));
    }

    @Override
    public List<Map<String, Object>> findAllAsMap() {
        return findAll().stream()
                .map(PagoDTO.Response::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> findByIdAsMap(Long id) {
        return findById(id).toMap();
    }

    public List<PagoDTO.Response> findByReserva(Long reservaId) {
        return pagoRepository.findByReservaId(reservaId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public PagoDTO.Response create(PagoDTO.Request request) {
        Reserva reserva = reservaRepository.findById(request.getReservaId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", request.getReservaId()));

        if (reserva.getEstado() == Reserva.EstadoReserva.CANCELADA) {
            throw new BusinessException("No se puede registrar un pago en una reserva cancelada");
        }

        // Verificar que el monto no supere el saldo pendiente
        BigDecimal totalPagado = pagoRepository.sumPagosAprobadosByReservaId(reserva.getId());
        BigDecimal saldo = reserva.getPrecioTotal().subtract(totalPagado);
        if (request.getMonto().compareTo(saldo) > 0) {
            throw new BusinessException(
                    String.format("El monto ($%.2f) supera el saldo pendiente ($%.2f)",
                            request.getMonto(), saldo));
        }

        Factura factura = null;
        if (request.getFacturaId() != null) {
            factura = facturaRepository.findById(request.getFacturaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Factura", "id", request.getFacturaId()));
        }

        Pago pago = Pago.builder()
                .monto(request.getMonto())
                .metodoPago(request.getMetodoPago())
                .referenciaTransaccion(request.getReferenciaTransaccion())
                .descripcion(request.getDescripcion())
                .estadoPago(Pago.EstadoPago.APROBADO)
                .reserva(reserva)
                .factura(factura)
                .build();

        Pago guardado = pagoRepository.save(pago);
        log.info("Pago registrado id={}, monto={}", guardado.getId(), guardado.getMonto());
        return toResponse(guardado);
    }

    @Transactional
    public PagoDTO.Response update(Long id, PagoDTO.Request request) {
        Pago pago = getPagoOrThrow(id);

        if (pago.getEstadoPago() == Pago.EstadoPago.REEMBOLSADO) {
            throw new BusinessException("No se puede modificar un pago reembolsado");
        }

        pago.setMonto(request.getMonto());
        pago.setMetodoPago(request.getMetodoPago());
        pago.setReferenciaTransaccion(request.getReferenciaTransaccion());
        pago.setDescripcion(request.getDescripcion());

        return toResponse(pagoRepository.save(pago));
    }

    @Transactional
    public void delete(Long id) {
        Pago pago = getPagoOrThrow(id);
        pago.setEstadoPago(Pago.EstadoPago.CANCELADO);
        pagoRepository.save(pago);
        log.info("Pago cancelado id={}", id);
    }

    // ─── helpers ─────────────────────────────────────────────
    private Pago getPagoOrThrow(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", "id", id));
    }

    public PagoDTO.Response toResponse(Pago p) {
        return PagoDTO.Response.builder()
                .id(p.getId())
                .monto(p.getMonto())
                .metodoPago(p.getMetodoPago())
                .referenciaTransaccion(p.getReferenciaTransaccion())
                .estadoPago(p.getEstadoPago())
                .descripcion(p.getDescripcion())
                .fechaPago(p.getFechaPago())
                .reservaId(p.getReserva().getId())
                .codigoReserva(p.getReserva().getCodigoReserva())
                .facturaId(p.getFactura() != null ? p.getFactura().getId() : null)
                .build();
    }
}
