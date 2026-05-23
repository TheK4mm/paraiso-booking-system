package com.hotel.paraiso.service;

import com.hotel.paraiso.dto.FacturaDTO;
import com.hotel.paraiso.exception.BusinessException;
import com.hotel.paraiso.exception.ResourceNotFoundException;
import com.hotel.paraiso.model.Factura;
import com.hotel.paraiso.model.Reserva;
import com.hotel.paraiso.repository.FacturaRepository;
import com.hotel.paraiso.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FacturaService implements IViewMapService<FacturaDTO.Response> {

    private final FacturaRepository facturaRepository;
    private final ReservaRepository reservaRepository;

    public List<FacturaDTO.Response> findAll() {
        return facturaRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public FacturaDTO.Response findById(Long id) {
        return toResponse(getFacturaOrThrow(id));
    }

    @Override
    public List<Map<String, Object>> findAllAsMap() {
        return findAll().stream()
                .map(FacturaDTO.Response::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> findByIdAsMap(Long id) {
        return findById(id).toMap();
    }

    public FacturaDTO.Response findByReserva(Long reservaId) {
        Factura factura = facturaRepository.findByReservaId(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", "reservaId", reservaId));
        return toResponse(factura);
    }

    @Transactional
    public FacturaDTO.Response create(FacturaDTO.Request request) {
        Reserva reserva = reservaRepository.findById(request.getReservaId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", request.getReservaId()));

        if (facturaRepository.existsByReservaId(request.getReservaId())) {
            throw new BusinessException("La reserva ya tiene una factura asociada");
        }

        if (reserva.getEstado() != Reserva.EstadoReserva.CHECKOUT
                && reserva.getEstado() != Reserva.EstadoReserva.CHECKIN
                && reserva.getEstado() != Reserva.EstadoReserva.CONFIRMADA) {
            throw new BusinessException("Solo se puede facturar reservas confirmadas, en check-in o check-out");
        }

        BigDecimal subtotal         = reserva.getPrecioTotal();
        BigDecimal descuento        = request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO;
        BigDecimal impuestoPct      = request.getImpuestoPorcentaje() != null
                ? request.getImpuestoPorcentaje() : new BigDecimal("19.00");
        BigDecimal base             = subtotal.subtract(descuento);
        BigDecimal impuestoValor    = base.multiply(impuestoPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total            = base.add(impuestoValor);

        Factura factura = Factura.builder()
                .numeroFactura(generarNumeroFactura())
                .subtotal(subtotal)
                .impuestoPorcentaje(impuestoPct)
                .impuestoValor(impuestoValor)
                .descuento(descuento)
                .total(total)
                .notas(request.getNotas())
                .estadoFactura(Factura.EstadoFactura.PENDIENTE)
                .reserva(reserva)
                .build();

        Factura guardada = facturaRepository.save(factura);
        log.info("Factura creada: numero={}, total={}", guardada.getNumeroFactura(), guardada.getTotal());
        return toResponse(guardada);
    }

    @Transactional
    public FacturaDTO.Response update(Long id, FacturaDTO.Request request) {
        Factura factura = getFacturaOrThrow(id);
        if (factura.getEstadoFactura() == Factura.EstadoFactura.ANULADA) {
            throw new BusinessException("No se puede modificar una factura anulada");
        }

        BigDecimal subtotal      = factura.getReserva().getPrecioTotal();
        BigDecimal descuento     = request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO;
        BigDecimal impuestoPct   = request.getImpuestoPorcentaje() != null
                ? request.getImpuestoPorcentaje() : factura.getImpuestoPorcentaje();
        BigDecimal base          = subtotal.subtract(descuento);
        BigDecimal impuestoValor = base.multiply(impuestoPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        factura.setDescuento(descuento);
        factura.setImpuestoPorcentaje(impuestoPct);
        factura.setImpuestoValor(impuestoValor);
        factura.setTotal(base.add(impuestoValor));
        factura.setNotas(request.getNotas());

        return toResponse(facturaRepository.save(factura));
    }

    @Transactional
    public void delete(Long id) {
        Factura factura = getFacturaOrThrow(id);
        factura.setEstadoFactura(Factura.EstadoFactura.ANULADA);
        facturaRepository.save(factura);
        log.info("Factura anulada id={}", id);
    }

    // ─── helpers ─────────────────────────────────────────────
    private Factura getFacturaOrThrow(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", "id", id));
    }

    private String generarNumeroFactura() {
        String anio = DateTimeFormatter.ofPattern("yyyy").format(LocalDate.now());
        long count = facturaRepository.count() + 1;
        return String.format("FAC-%s-%06d", anio, count);
    }

    public FacturaDTO.Response toResponse(Factura f) {
        return FacturaDTO.Response.builder()
                .id(f.getId())
                .numeroFactura(f.getNumeroFactura())
                .subtotal(f.getSubtotal())
                .impuestoPorcentaje(f.getImpuestoPorcentaje())
                .impuestoValor(f.getImpuestoValor())
                .descuento(f.getDescuento())
                .total(f.getTotal())
                .notas(f.getNotas())
                .estadoFactura(f.getEstadoFactura())
                .fechaEmision(f.getFechaEmision())
                .reservaId(f.getReserva().getId())
                .codigoReserva(f.getReserva().getCodigoReserva())
                .build();
    }
}
