package com.hotel.paraiso.portal.reserva;

import com.hotel.paraiso.common.validation.RangoFechas;
import com.hotel.paraiso.common.validation.RangoFechasValido;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Paso 1 del flujo público de reservas: fechas de la estancia y número
 * de huéspedes. Viaja por query params (idempotente y compartible).
 */
@Getter
@Setter
@NoArgsConstructor
@RangoFechasValido
public class BusquedaDisponibilidadRequest implements RangoFechas {

    @NotNull(message = "La fecha de llegada es obligatoria")
    @FutureOrPresent(message = "La fecha de llegada no puede ser en el pasado")
    private LocalDate fechaEntrada;

    @NotNull(message = "La fecha de salida es obligatoria")
    private LocalDate fechaSalida;

    @NotNull(message = "Indica cuántos huéspedes son")
    @Min(value = 1, message = "Debe haber al menos 1 huésped")
    @Max(value = 20, message = "No puede superar 20 huéspedes")
    private Integer huespedes = 2;

    public long noches() {
        return fechaEntrada == null || fechaSalida == null
                ? 0 : ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
    }
}
