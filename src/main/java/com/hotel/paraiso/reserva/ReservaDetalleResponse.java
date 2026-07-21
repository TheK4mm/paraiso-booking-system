package com.hotel.paraiso.reserva;

import com.hotel.paraiso.facturacion.FacturaResponse;
import com.hotel.paraiso.facturacion.PagoResponse;
import com.hotel.paraiso.habitacion.HabitacionResponse;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import com.hotel.paraiso.servicio.ServicioResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vista completa de una reserva: incluye habitaciones, servicios, pagos,
 * factura y saldos calculados. Se carga con consultas fetch dedicadas.
 */
@Getter
@Builder(toBuilder = true)
public class ReservaDetalleResponse {

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

    private List<HabitacionResponse> habitaciones;
    private List<ServicioResponse> servicios;
    private List<PagoResponse> pagos;
    private FacturaResponse factura;

    /** Suma de pagos aprobados de la reserva. */
    private BigDecimal totalPagado;

    /** Contra el total de la factura si existe; si no, contra el precio de la reserva. */
    private BigDecimal saldoPendiente;
}
