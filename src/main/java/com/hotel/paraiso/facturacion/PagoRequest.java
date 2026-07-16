package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.facturacion.Pago.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PagoRequest {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    @Size(max = 100)
    private String referenciaTransaccion;

    @Size(max = 300)
    private String descripcion;

    @NotNull(message = "La reserva es obligatoria")
    private Long reservaId;

    private Long facturaId;
}
