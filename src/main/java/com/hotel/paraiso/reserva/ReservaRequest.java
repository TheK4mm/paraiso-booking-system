package com.hotel.paraiso.reserva;

import com.hotel.paraiso.common.validation.RangoFechas;
import com.hotel.paraiso.common.validation.RangoFechasValido;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@RangoFechasValido
public class ReservaRequest implements RangoFechas {

    @NotNull(message = "La fecha de entrada es obligatoria")
    private LocalDate fechaEntrada;

    @NotNull(message = "La fecha de salida es obligatoria")
    private LocalDate fechaSalida;

    @NotNull(message = "El número de huéspedes es obligatorio")
    @Min(value = 1, message = "Debe haber al menos 1 huésped")
    @Max(value = 20, message = "No puede superar 20 huéspedes")
    private Integer numeroHuespedes;

    @Size(max = 500)
    private String observaciones;

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    private Long empleadoId;

    @NotEmpty(message = "Debe seleccionar al menos una habitación")
    private List<Long> habitacionIds;

    private List<Long> servicioIds;
}
