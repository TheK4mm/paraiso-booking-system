package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.exception.BusinessException;
import com.hotel.paraiso.facturacion.Factura.EstadoFactura;
import com.hotel.paraiso.facturacion.Pago.EstadoPago;
import com.hotel.paraiso.facturacion.Pago.MetodoPago;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock private PagoRepository pagoRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private FacturaRepository facturaRepository;
    @Mock private FacturaService facturaService;
    @Mock private PagoMapper pagoMapper;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PagoService service;

    private Reserva reserva;

    @BeforeEach
    void setUp() {
        reserva = Reserva.builder()
                .id(10L).codigoReserva("RES-2026-000010")
                .estado(EstadoReserva.CONFIRMADA)
                .precioTotal(new BigDecimal("1000000"))
                .build();
        lenient().when(reservaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(reserva));
        lenient().when(facturaRepository.findByReservaId(10L)).thenReturn(Optional.empty());
        lenient().when(pagoMapper.toResponse(any())).thenReturn(PagoResponse.builder().build());
    }

    private PagoRequest request(String monto) {
        PagoRequest r = new PagoRequest();
        r.setMonto(new BigDecimal(monto));
        r.setMetodoPago(MetodoPago.EFECTIVO);
        r.setReservaId(10L);
        return r;
    }

    @Test
    void creaPagoAprobadoYRecalculaFactura() {
        when(pagoRepository.sumPagosAprobadosByReservaId(10L)).thenReturn(BigDecimal.ZERO);
        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        when(pagoRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.create(request("400000"));

        assertThat(captor.getValue().getEstadoPago()).isEqualTo(EstadoPago.APROBADO);
        verify(facturaService).recalcularEstadoPorReserva(10L);
    }

    @Test
    void rechazaSobrepagoContraPrecioDeReserva() {
        when(pagoRepository.sumPagosAprobadosByReservaId(10L)).thenReturn(new BigDecimal("800000"));
        assertThatThrownBy(() -> service.create(request("300000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("saldo");
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void elLimiteEsElTotalDeLaFacturaSiExiste() {
        // factura con IVA: total 1.190.000 > precio de reserva 1.000.000
        Factura factura = Factura.builder().id(3L).reserva(reserva)
                .total(new BigDecimal("1190000")).estadoFactura(EstadoFactura.PENDIENTE).build();
        when(facturaRepository.findByReservaId(10L)).thenReturn(Optional.of(factura));
        when(pagoRepository.sumPagosAprobadosByReservaId(10L)).thenReturn(new BigDecimal("1000000"));
        when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 190.000 restantes del IVA: válido contra el total de la factura
        service.create(request("190000"));
        verify(pagoRepository).save(any());
    }

    @Test
    void rechazaPagoEnReservaCancelada() {
        reserva.setEstado(EstadoReserva.CANCELADA);
        assertThatThrownBy(() -> service.create(request("100000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    void rechazaFacturaDeOtraReserva() {
        Reserva otra = Reserva.builder().id(99L).codigoReserva("RES-2026-000099").build();
        Factura ajena = Factura.builder().id(4L).numeroFactura("FAC-2026-000004")
                .reserva(otra).estadoFactura(EstadoFactura.PENDIENTE).build();
        when(facturaRepository.findById(4L)).thenReturn(Optional.of(ajena));

        PagoRequest req = request("100000");
        req.setFacturaId(4L);
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void updateRevalidaSaldoExcluyendoElMontoPropio() {
        Pago pago = Pago.builder().id(5L).monto(new BigDecimal("200000"))
                .estadoPago(EstadoPago.APROBADO).reserva(reserva).build();
        when(pagoRepository.findById(5L)).thenReturn(Optional.of(pago));
        // otros pagos aprobados suman 700.000; con este pago en 400.000 → 1.100.000 > 1.000.000
        when(pagoRepository.sumPagosAprobadosByReservaIdExcluyendo(10L, 5L))
                .thenReturn(new BigDecimal("700000"));

        assertThatThrownBy(() -> service.update(5L, request("400000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("saldo");

        // pero 300.000 sí cabe exactamente
        when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.update(5L, request("300000"));
        assertThat(pago.getMonto()).isEqualByComparingTo(new BigDecimal("300000"));
    }

    @Test
    void noSePuedeModificarUnPagoReembolsado() {
        Pago pago = Pago.builder().id(5L).estadoPago(EstadoPago.REEMBOLSADO).reserva(reserva).build();
        when(pagoRepository.findById(5L)).thenReturn(Optional.of(pago));
        assertThatThrownBy(() -> service.update(5L, request("100000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("REEMBOLSADO");
    }

    @Test
    void cancelarMarcaElPagoYRecalculaFactura() {
        Pago pago = Pago.builder().id(5L).monto(new BigDecimal("100000"))
                .estadoPago(EstadoPago.APROBADO).reserva(reserva).build();
        when(pagoRepository.findById(5L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancelar(5L);

        assertThat(pago.getEstadoPago()).isEqualTo(EstadoPago.CANCELADO);
        verify(facturaService).recalcularEstadoPorReserva(10L);
    }
}
