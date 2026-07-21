package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.facturacion.Factura.EstadoFactura;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class FacturaResponse {

    private Long id;
    private String numeroFactura;
    private BigDecimal subtotal;
    private BigDecimal impuestoPorcentaje;
    private BigDecimal impuestoValor;
    private BigDecimal descuento;
    private BigDecimal total;
    private String notas;
    private EstadoFactura estadoFactura;
    private LocalDateTime fechaEmision;
    private Long reservaId;
    private String codigoReserva;
}
