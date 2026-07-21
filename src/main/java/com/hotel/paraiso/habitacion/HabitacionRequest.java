package com.hotel.paraiso.habitacion;

import com.hotel.paraiso.habitacion.Habitacion.EstadoHabitacion;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HabitacionRequest {

    @NotBlank(message = "El número de habitación es obligatorio")
    @Size(max = 10, message = "El número no puede superar 10 caracteres")
    private String numero;

    @NotNull(message = "El piso es obligatorio")
    @Min(value = 1, message = "El piso mínimo es 1")
    private Integer piso;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    private String descripcion;

    private EstadoHabitacion estado;

    @NotNull(message = "El tipo de habitación es obligatorio")
    private Long tipoHabitacionId;
}
