package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.exception.BusinessException;
import com.hotel.paraiso.facturacion.Factura.EstadoFactura;
import com.hotel.paraiso.reserva.Reserva;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import com.hotel.paraiso.reserva.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @Mock private FacturaRepository facturaRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private FacturaMapper facturaMapper;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FacturaService service;

    private Reserva reserva;

    @BeforeEach
    void setUp() {
        reserva = Reserva.builder()
                .id(10L).codigoReserva("RES-2026-000010")
                .estado(EstadoReserva.CHECKOUT)
                .precioTotal(new BigDecimal("1000000"))
                .build();
        lenient().when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        lenient().when(facturaMapper.toResponse(any())).thenReturn(FacturaResponse.builder().build());
        lenient().when(pagoRepository.sumPagosAprobadosByReservaId(anyLong())).thenReturn(BigDecimal.ZERO);
    }

    private FacturaRequest request(String descuento, String impuestoPct) {
        FacturaRequest r = new FacturaRequest();
        r.setReservaId(10L);
        if (descuento != null) {
            r.setDescuento(new BigDecimal(descuento));
        }
        if (impuestoPct != null) {
            r.setImpuestoPorcentaje(new BigDecimal(impuestoPct));
        }
        return r;
    }

    @Test
    void calculaIvaSobreBaseConDescuento() {
        when(facturaRepository.existsByReservaId(10L)).thenReturn(false);
        when(facturaRepository.nextNumeroSeq()).thenReturn(7L);
        ArgumentCaptor<Factura> captor = ArgumentCaptor.forClass(Factura.class);
        when(facturaRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.create(request("100000", "19.00"));

        Factura f = captor.getValue();
        assertThat(f.getNumeroFactura()).isEqualTo(String.format("FAC-%d-000007", Year.now().getValue()));
        assertThat(f.getSubtotal()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(f.getDescuento()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(f.getImpuestoValor()).isEqualByComparingTo(new BigDecimal("171000.00")); // 19% de 900.000
        assertThat(f.getTotal()).isEqualByComparingTo(new BigDecimal("1071000.00"));
    }

    @Test
    void rechazaDescuentoMayorAlSubtotal() {
        when(facturaRepository.existsByReservaId(10L)).thenReturn(false);
        assertThatThrownBy(() -> service.create(request("1000001", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("descuento");
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void rechazaFacturarReservaPendiente() {
        reserva.setEstado(EstadoReserva.PENDIENTE);
        when(facturaRepository.existsByReservaId(10L)).thenReturn(false);
        assertThatThrownBy(() -> service.create(request(null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("facturar");
    }

    @Test
    void rechazaSegundaFacturaParaLaMismaReserva() {
        when(facturaRepository.existsByReservaId(10L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(request(null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya tiene una factura");
    }

    @Test
    void recalculaEstadoSegunPagosAprobados() {
        Factura factura = Factura.builder().id(3L).reserva(reserva)
                .total(new BigDecimal("1190000")).estadoFactura(EstadoFactura.PENDIENTE).build();
        when(facturaRepository.findByReservaId(10L)).thenReturn(Optional.of(factura));
        when(facturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // pago parcial
        when(pagoRepository.sumPagosAprobadosByReservaId(10L)).thenReturn(new BigDecimal("500000"));
        service.recalcularEstadoPorReserva(10L);
        assertThat(factura.getEstadoFactura()).isEqualTo(EstadoFactura.PAGADA_PARCIALMENTE);

        // pago completo
        when(pagoRepository.sumPagosAprobadosByReservaId(10L)).thenReturn(new BigDecimal("1190000"));
        service.recalcularEstadoPorReserva(10L);
        assertThat(factura.getEstadoFactura()).isEqualTo(EstadoFactura.PAGADA);

        // los pagos se cancelan → vuelve a pendiente
        when(pagoRepository.sumPagosAprobadosByReservaId(10L)).thenReturn(BigDecimal.ZERO);
        service.recalcularEstadoPorReserva(10L);
        assertThat(factura.getEstadoFactura()).isEqualTo(EstadoFactura.PENDIENTE);
    }

    @Test
    void unaFacturaAnuladaNoCambiaDeEstado() {
        Factura factura = Factura.builder().id(3L).reserva(reserva)
                .total(new BigDecimal("1190000")).estadoFactura(EstadoFactura.ANULADA).build();
        when(facturaRepository.findByReservaId(10L)).thenReturn(Optional.of(factura));

        service.recalcularEstadoPorReserva(10L);

        assertThat(factura.getEstadoFactura()).isEqualTo(EstadoFactura.ANULADA);
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void noSePuedeModificarUnaFacturaAnulada() {
        Factura factura = Factura.builder().id(3L).reserva(reserva)
                .estadoFactura(EstadoFactura.ANULADA).build();
        when(facturaRepository.findById(3L)).thenReturn(Optional.of(factura));
        assertThatThrownBy(() -> service.update(3L, request(null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("anulada");
    }
}
