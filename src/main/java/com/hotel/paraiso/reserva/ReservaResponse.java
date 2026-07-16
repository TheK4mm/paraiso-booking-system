package com.hotel.paraiso.reserva;

import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vista de listado de una reserva: solo escalares y resúmenes to-one
 * (las colecciones viven en {@link ReservaDetalleResponse}).
 */
@Getter
@Builder
public class ReservaResponse {

    private Long id;
    private String codigoReserva;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private Integer numeroHuespedes;
    private Integer totalNoches;
    private BigDecimal precioTotal;
    private String observaciones;
    private EstadoReserva estado;
    private LocalDateTime creadoEn;

    private Long clienteId;
    private String clienteNombreCompleto;
    private String clienteDocumento;

    private Long empleadoId;
    private String empleadoNombreCompleto;
}
