package com.hotel.paraiso.servicio;

import com.hotel.paraiso.servicio.Servicio.CategoriaServicio;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ServicioRequest {

    @NotBlank(message = "El nombre del servicio es obligatorio")
    @Size(max = 100)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    private BigDecimal precio;

    @NotNull(message = "La categoría es obligatoria")
    private CategoriaServicio categoria;
}
