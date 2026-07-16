package com.hotel.paraiso.facturacion;

import jakarta.validation.constraints.DecimalMax;
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
public class FacturaRequest {

    @NotNull(message = "La reserva es obligatoria")
    private Long reservaId;

    @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
    private BigDecimal descuento;

    @DecimalMin(value = "0.00", message = "El porcentaje de impuesto no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje de impuesto no puede superar 100")
    private BigDecimal impuestoPorcentaje;

    @Size(max = 500)
    private String notas;
}
