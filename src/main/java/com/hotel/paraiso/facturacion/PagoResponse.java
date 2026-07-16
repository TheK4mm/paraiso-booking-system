package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.facturacion.Pago.EstadoPago;
import com.hotel.paraiso.facturacion.Pago.MetodoPago;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PagoResponse {

    private Long id;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private String referenciaTransaccion;
    private EstadoPago estadoPago;
    private String descripcion;
    private LocalDateTime fechaPago;
    private Long reservaId;
    private String codigoReserva;
    private Long facturaId;
}
