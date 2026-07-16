package com.hotel.paraiso.portal.reserva;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Selección del huésped entre el paso 2 y la confirmación. Vive en la
 * sesión HTTP para sobrevivir a un desvío por login/registro. El precio
 * es informativo: la confirmación recalcula todo en ReservaService bajo
 * lock — la sesión nunca es autoritativa.
 */
public record ReservaEnCurso(
        Long habitacionId,
        String numero,
        String tipoNombre,
        String imagen,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        Integer huespedes,
        long noches,
        BigDecimal precioNoche,
        BigDecimal total
) implements Serializable {
}
