package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.exception.BusinessException;
import com.hotel.paraiso.common.exception.ResourceNotFoundException;
import com.hotel.paraiso.common.web.SortWhitelist;
import com.hotel.paraiso.facturacion.Factura.EstadoFactura;
import com.hotel.paraiso.reserva.Reserva;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import com.hotel.paraiso.reserva.ReservaRepository;
import com.hotel.paraiso.common.audit.ActividadEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Facturación: cálculo de totales (IVA sobre base con descuento),
 * numeración por secuencia y estado derivado de los pagos aprobados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FacturaService {

    private static final BigDecimal IVA_POR_DEFECTO = new BigDecimal("19.00");
    private static final Set<String> ORDENABLES =
            Set.of("id", "numeroFactura", "subtotal", "total", "estadoFactura", "fechaEmision");

    private final FacturaRepository facturaRepository;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final FacturaMapper facturaMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ─── Consultas ────────────────────────────────────────────────────

    public Page<FacturaResponse> buscar(String q, EstadoFactura estado, Pageable pageable) {
        Specification<Factura> texto = !StringUtils.hasText(q) ? null
                : (root, query, cb) -> cb.or(
                        cb.like(cb.lower(root.get("numeroFactura")), "%" + q.trim().toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("reserva").get("codigoReserva")), "%" + q.trim().toLowerCase() + "%"));
        Specification<Factura> porEstado = estado == null ? null
                : (root, query, cb) -> cb.equal(root.get("estadoFactura"), estado);
        Specification<Factura> spec = Specification.allOf(
                Stream.of(texto, porEstado).filter(Objects::nonNull).toList());
        Pageable saneado = SortWhitelist.sanitize(pageable, ORDENABLES, Sort.by(Sort.Direction.DESC, "id"));
        return facturaRepository.findAll(spec, saneado).map(facturaMapper::toResponse);
    }

    public FacturaResponse findById(Long id) {
        return facturaMapper.toResponse(getFacturaOrThrow(id));
    }

    public FacturaResponse findByReserva(Long reservaId) {
        Factura factura = facturaRepository.findByReservaId(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", "reservaId", reservaId));
        return facturaMapper.toResponse(factura);
    }

    // ─── Escritura ────────────────────────────────────────────────────

    @Transactional
    public FacturaResponse create(FacturaRequest request) {
        Reserva reserva = reservaRepository.findById(request.getReservaId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", request.getReservaId()));

        if (facturaRepository.existsByReservaId(request.getReservaId())) {
            throw new BusinessException("La reserva ya tiene una factura asociada");
        }
        if (reserva.getEstado() != EstadoReserva.CHECKOUT
                && reserva.getEstado() != EstadoReserva.CHECKIN
                && reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new BusinessException("Solo se puede facturar reservas confirmadas, en check-in o check-out");
        }

        BigDecimal subtotal = reserva.getPrecioTotal();
        BigDecimal descuento = request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO;
        BigDecimal impuestoPct = request.getImpuestoPorcentaje() != null
                ? request.getImpuestoPorcentaje() : IVA_POR_DEFECTO;
        validarDescuento(descuento, subtotal);

        BigDecimal base = subtotal.subtract(descuento);
        BigDecimal impuestoValor = calcularImpuesto(base, impuestoPct);

        Factura factura = Factura.builder()
                .numeroFactura(generarNumeroFactura())
                .subtotal(subtotal)
                .impuestoPorcentaje(impuestoPct)
                .impuestoValor(impuestoValor)
                .descuento(descuento)
                .total(base.add(impuestoValor))
                .notas(request.getNotas())
                .estadoFactura(EstadoFactura.PENDIENTE)
                .reserva(reserva)
                .build();

        Factura guardada = facturaRepository.save(factura);
        recalcularEstado(guardada); // la reserva puede tener anticipos previos
        eventPublisher.publishEvent(new ActividadEvent("FACTURA_EMITIDA", "Factura", guardada.getId(),
                guardada.getNumeroFactura() + " por $" + guardada.getTotal()));
        log.info("Factura creada: numero={}, total={}", guardada.getNumeroFactura(), guardada.getTotal());
        return facturaMapper.toResponse(guardada);
    }

    @Transactional
    public FacturaResponse update(Long id, FacturaRequest request) {
        Factura factura = getFacturaOrThrow(id);
        if (factura.getEstadoFactura() == EstadoFactura.ANULADA) {
            throw new BusinessException("No se puede modificar una factura anulada");
        }

        BigDecimal subtotal = factura.getReserva().getPrecioTotal();
        BigDecimal descuento = request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO;
        BigDecimal impuestoPct = request.getImpuestoPorcentaje() != null
                ? request.getImpuestoPorcentaje() : factura.getImpuestoPorcentaje();
        validarDescuento(descuento, subtotal);

        BigDecimal base = subtotal.subtract(descuento);
        BigDecimal impuestoValor = calcularImpuesto(base, impuestoPct);

        factura.setSubtotal(subtotal);
        factura.setDescuento(descuento);
        factura.setImpuestoPorcentaje(impuestoPct);
        factura.setImpuestoValor(impuestoValor);
        factura.setTotal(base.add(impuestoValor));
        factura.setNotas(request.getNotas());

        Factura guardada = facturaRepository.save(factura);
        recalcularEstado(guardada); // el total pudo cambiar respecto a lo pagado
        return facturaMapper.toResponse(guardada);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void anular(Long id) {
        Factura factura = getFacturaOrThrow(id);
        factura.setEstadoFactura(EstadoFactura.ANULADA);
        facturaRepository.save(factura);
        eventPublisher.publishEvent(new ActividadEvent("FACTURA_ANULADA", "Factura", id,
                factura.getNumeroFactura()));
        log.info("Factura anulada id={}", id);
    }

    /**
     * Estado derivado de los pagos aprobados de la reserva:
     * PAGADA si cubren el total, PAGADA_PARCIALMENTE si hay abonos,
     * PENDIENTE si no hay ninguno. Lo invoca PagoService tras cada cambio.
     */
    @Transactional
    public void recalcularEstadoPorReserva(Long reservaId) {
        facturaRepository.findByReservaId(reservaId).ifPresent(this::recalcularEstado);
    }

    private void recalcularEstado(Factura factura) {
        if (factura.getEstadoFactura() == EstadoFactura.ANULADA) {
            return;
        }
        BigDecimal pagado = pagoRepository.sumPagosAprobadosByReservaId(factura.getReserva().getId());
        EstadoFactura nuevo;
        if (pagado.compareTo(factura.getTotal()) >= 0) {
            nuevo = EstadoFactura.PAGADA;
        } else if (pagado.compareTo(BigDecimal.ZERO) > 0) {
            nuevo = EstadoFactura.PAGADA_PARCIALMENTE;
        } else {
            nuevo = EstadoFactura.PENDIENTE;
        }
        if (factura.getEstadoFactura() != nuevo) {
            factura.setEstadoFactura(nuevo);
            facturaRepository.save(factura);
        }
    }

    // ─── Reglas internas ──────────────────────────────────────────────

    private void validarDescuento(BigDecimal descuento, BigDecimal subtotal) {
        if (descuento.compareTo(subtotal) > 0) {
            throw new BusinessException(String.format(
                    "El descuento ($%,.2f) no puede superar el subtotal ($%,.2f)", descuento, subtotal));
        }
    }

    private BigDecimal calcularImpuesto(BigDecimal base, BigDecimal porcentaje) {
        return base.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** Número con secuencia de BD: atómico, sin colisiones bajo concurrencia. */
    private String generarNumeroFactura() {
        return String.format("FAC-%d-%06d", Year.now().getValue(), facturaRepository.nextNumeroSeq());
    }

    private Factura getFacturaOrThrow(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", "id", id));
    }
}
