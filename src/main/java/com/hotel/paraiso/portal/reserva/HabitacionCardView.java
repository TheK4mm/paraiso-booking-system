package com.hotel.paraiso.portal.reserva;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tarjeta de habitación disponible en el paso 2 del flujo público.
 * Se construye a mano en el servicio (sin MapStruct: es una vista).
 */
public record HabitacionCardView(
        Long habitacionId,
        String numero,
        Integer piso,
        String descripcion,
        String tipoNombre,
        String tipoDescripcion,
        Integer capacidad,
        String imagen,
        List<String> comodidades,
        BigDecimal precioNoche,
        BigDecimal totalEstancia
) {
}
