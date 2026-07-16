package com.hotel.paraiso.habitacion;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class TipoHabitacionResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Integer capacidadMaxima;
    private BigDecimal precioBaseNoche;
    private String imagen;
    private String comodidades;
    private Boolean activo;
    private LocalDateTime creadoEn;

    /** Solo se calcula en la vista de detalle (evita N+1 en listados). */
    private Long totalHabitaciones;
}
