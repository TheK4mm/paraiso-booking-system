package com.hotel.paraiso.reserva;

import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CambioEstadoRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoReserva estado;
}
