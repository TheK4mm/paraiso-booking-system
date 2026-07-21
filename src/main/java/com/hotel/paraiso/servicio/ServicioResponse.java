package com.hotel.paraiso.servicio;

import com.hotel.paraiso.servicio.Servicio.CategoriaServicio;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ServicioResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private CategoriaServicio categoria;
    private Boolean activo;
    private LocalDateTime creadoEn;
}
