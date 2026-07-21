package com.hotel.paraiso.habitacion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class TipoHabitacionRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    private String descripcion;

    @NotNull(message = "La capacidad máxima es obligatoria")
    @Min(value = 1, message = "La capacidad mínima es 1 persona")
    @Max(value = 20, message = "La capacidad máxima es 20 personas")
    private Integer capacidadMaxima;

    @NotNull(message = "El precio base por noche es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal precioBaseNoche;

    @Size(max = 255, message = "La ruta de la imagen no puede superar 255 caracteres")
    private String imagen;

    @Size(max = 500, message = "Las comodidades no pueden superar 500 caracteres")
    private String comodidades;
}
