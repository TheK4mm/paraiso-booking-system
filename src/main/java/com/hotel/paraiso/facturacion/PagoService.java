package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.audit.ActividadEvent;
import com.hotel.paraiso.common.exception.BusinessException;
import com.hotel.paraiso.common.exception.ResourceNotFoundException;
import com.hotel.paraiso.common.web.SortWhitelist;
import com.hotel.paraiso.facturacion.Factura.EstadoFactura;
import com.hotel.paraiso.facturacion.Pago.EstadoPago;
import com.hotel.paraiso.reserva.Reserva;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import com.hotel.paraiso.reserva.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Pagos: validación de saldo bajo lock pesimista de la reserva (evita
 * sobre-pagos concurrentes) y sincronización del estado de la factura.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PagoService {

    private static final Set<String> ORDENABLES = Set.of("id", "monto", "metodoPago", "estadoPago", "fechaPago");

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final FacturaRepository facturaRepository;
    private final FacturaService facturaService;
    private final PagoMapper pagoMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ─── Consultas ────────────────────────────────────────────────────

    public Page<PagoResponse> buscar(EstadoPago estado, Long reservaId, Pageable pageable) {
        Specification<Pago> porEstado = estado == null ? null
                : (root, query, cb) -> cb.equal(root.get("estadoPago"), estado);
        Specification<Pago> porReserva = reservaId == null ? null
                : (root, query, cb) -> cb.equal(root.get("reserva").get("id"), reservaId);
        Specification<Pago> spec = Specification.allOf(
                Stream.of(porEstado, porReserva).filter(Objects::nonNull).toList());
        Pageable saneado = SortWhitelist.sanitize(pageable, ORDENABLES, Sort.by(Sort.Direction.DESC, "id"));
        return pagoRepository.findAll(spec, saneado).map(pagoMapper::toResponse);
    }

    public PagoResponse findById(Long id) {
        return pagoMapper.toResponse(getPagoOrThrow(id));
    }

    public List<PagoResponse> findByReserva(Long reservaId) {
        return pagoRepository.findByReservaId(reservaId)
                .stream().map(pagoMapper::toResponse).toList();
    }

    // ─── Escritura ────────────────────────────────────────────────────

    @Transactional
    public PagoResponse create(PagoRequest request) {
        // Lock pesimista: serializa los pagos concurrentes de la misma reserva
        Reserva reserva = reservaRepository.findByIdForUpdate(request.getReservaId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", request.getReservaId()));

        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new BusinessException("No se puede registrar un pago en una reserva cancelada");
        }

        Factura factura = resolverFactura(request.getFacturaId(), reserva);
        validarSaldo(reserva, request.getMonto(), pagoRepository.sumPagosAprobadosByReservaId(reserva.getId()));

        Pago pago = Pago.builder()
                .monto(request.getMonto())
                .metodoPago(request.getMetodoPago())
                .referenciaTransaccion(request.getReferenciaTransaccion())
                .descripcion(request.getDescripcion())
                .estadoPago(EstadoPago.APROBADO)
                .reserva(reserva)
                .factura(factura)
                .build();

        Pago guardado = pagoRepository.save(pago);
        facturaService.recalcularEstadoPorReserva(reserva.getId());
        eventPublisher.publishEvent(new ActividadEvent("PAGO_REGISTRADO", "Pago", guardado.getId(),
                "$" + guardado.getMonto() + " a " + reserva.getCodigoReserva()));
        log.info("Pago registrado id={}, monto={}, reserva={}",
                guardado.getId(), guardado.getMonto(), reserva.getCodigoReserva());
        return pagoMapper.toResponse(guardado);
    }

    @Transactional
    public PagoResponse update(Long id, PagoRequest request) {
        Pago pago = getPagoOrThrow(id);

        if (pago.getEstadoPago() == EstadoPago.REEMBOLSADO || pago.getEstadoPago() == EstadoPago.CANCELADO) {
            throw new BusinessException("No se puede modificar un pago en estado: " + pago.getEstadoPago());
        }

        // Lock de la reserva y re-validación del saldo excluyendo el monto previo de este pago
        Reserva reserva = reservaRepository.findByIdForUpdate(pago.getReserva().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", pago.getReserva().getId()));
        BigDecimal pagadoSinEste = pagoRepository.sumPagosAprobadosByReservaIdExcluyendo(reserva.getId(), pago.getId());
        validarSaldo(reserva, request.getMonto(), pagadoSinEste);

        pago.setMonto(request.getMonto());
        pago.setMetodoPago(request.getMetodoPago());
        pago.setReferenciaTransaccion(request.getReferenciaTransaccion());
        pago.setDescripcion(request.getDescripcion());

        Pago guardado = pagoRepository.save(pago);
        facturaService.recalcularEstadoPorReserva(reserva.getId());
        return pagoMapper.toResponse(guardado);
    }

    @Transactional
    public void cancelar(Long id) {
        Pago pago = getPagoOrThrow(id);
        pago.setEstadoPago(EstadoPago.CANCELADO);
        pagoRepository.save(pago);
        facturaService.recalcularEstadoPorReserva(pago.getReserva().getId());
        eventPublisher.publishEvent(new ActividadEvent("PAGO_CANCELADO", "Pago", id,
                "$" + pago.getMonto()));
        log.info("Pago cancelado id={}", id);
    }

    // ─── Reglas internas ──────────────────────────────────────────────

    /** Si el pago referencia una factura, debe pertenecer a la misma reserva y no estar anulada. */
    private Factura resolverFactura(Long facturaId, Reserva reserva) {
        if (facturaId == null) {
            return null;
        }
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", "id", facturaId));
        if (!factura.getReserva().getId().equals(reserva.getId())) {
            throw new BusinessException("La factura " + factura.getNumeroFactura()
                    + " no pertenece a la reserva " + reserva.getCodigoReserva());
        }
        if (factura.getEstadoFactura() == EstadoFactura.ANULADA) {
            throw new BusinessException("No se puede asociar un pago a una factura anulada");
        }
        return factura;
    }

    /**
     * El límite a pagar es el total de la factura vigente (incluye IVA);
     * si aún no hay factura, el precio de la reserva.
     */
    private void validarSaldo(Reserva reserva, BigDecimal monto, BigDecimal yaPagado) {
        BigDecimal limite = facturaRepository.findByReservaId(reserva.getId())
                .filter(f -> f.getEstadoFactura() != EstadoFactura.ANULADA)
                .map(Factura::getTotal)
                .orElse(reserva.getPrecioTotal());
        BigDecimal saldo = limite.subtract(yaPagado);
        if (monto.compareTo(saldo) > 0) {
            throw new BusinessException(String.format(
                    "El monto ($%,.2f) supera el saldo pendiente ($%,.2f)", monto, saldo));
        }
    }

    private Pago getPagoOrThrow(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", "id", id));
    }
}
