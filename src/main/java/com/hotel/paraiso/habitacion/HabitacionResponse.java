package com.hotel.paraiso.habitacion;

import com.hotel.paraiso.habitacion.Habitacion.EstadoHabitacion;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class HabitacionResponse {

    private Long id;
    private String numero;
    private Integer piso;
    private String descripcion;
    private EstadoHabitacion estado;
    private Boolean activo;
    private LocalDateTime creadoEn;

    private Long tipoHabitacionId;
    private String tipoHabitacionNombre;
    private BigDecimal precioBaseNoche;
    private Integer capacidadMaxima;
}
