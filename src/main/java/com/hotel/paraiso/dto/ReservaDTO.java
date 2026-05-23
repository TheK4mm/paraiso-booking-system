package com.hotel.paraiso.dto;

import com.hotel.paraiso.model.Reserva.EstadoReserva;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReservaDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Request {

        @NotNull(message = "La fecha de entrada es obligatoria")
        @FutureOrPresent(message = "La fecha de entrada no puede ser en el pasado")
        private LocalDate fechaEntrada;

        @NotNull(message = "La fecha de salida es obligatoria")
        private LocalDate fechaSalida;

        @NotNull(message = "El número de huéspedes es obligatorio")
        @Min(value = 1, message = "Debe haber al menos 1 huésped")
        @Max(value = 20, message = "No puede superar 20 huéspedes")
        private Integer numeroHuespedes;

        @Size(max = 500)
        private String observaciones;

        @NotNull(message = "El ID del cliente es obligatorio")
        private Long clienteId;

        private Long empleadoId;

        @NotEmpty(message = "Debe seleccionar al menos una habitación")
        private List<Long> habitacionIds;

        private List<Long> servicioIds;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String codigoReserva;
        private LocalDate fechaEntrada;
        private LocalDate fechaSalida;
        private Integer numeroHuespedes;
        private Integer totalNoches;
        private BigDecimal precioTotal;
        private String observaciones;
        private EstadoReserva estado;
        private LocalDateTime fechaCreacion;

        // Resumen del cliente
        private Long clienteId;
        private String clienteNombreCompleto;
        private String clienteDocumento;

        // Resumen del empleado
        private Long empleadoId;
        private String empleadoNombreCompleto;

        // Habitaciones reservadas
        private List<HabitacionDTO.Response> habitaciones;

        // Servicios adicionales
        private List<ServicioDTO.Response> servicios;

        // Pagos realizados
        private List<PagoDTO.Response> pagos;

        // Factura asociada
        private FacturaDTO.Response factura;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", this.id);
            map.put("codigoReserva", this.codigoReserva);
            map.put("fechaEntrada", this.fechaEntrada);
            map.put("fechaSalida", this.fechaSalida);
            map.put("numeroHuespedes", this.numeroHuespedes);
            map.put("totalNoches", this.totalNoches);
            map.put("precioTotal", this.precioTotal);
            map.put("observaciones", this.observaciones);
            map.put("estado", this.estado);
            map.put("fechaCreacion", this.fechaCreacion);
            map.put("clienteId", this.clienteId);
            map.put("clienteNombreCompleto", this.clienteNombreCompleto);
            map.put("clienteDocumento", this.clienteDocumento);
            map.put("empleadoId", this.empleadoId);
            map.put("empleadoNombreCompleto", this.empleadoNombreCompleto);
            return map;
        }
    }

    /** Para actualizar solo el estado */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EstadoRequest {
        @NotNull(message = "El nuevo estado es obligatorio")
        private EstadoReserva estado;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("estado", this.estado);
            return map;
        }
    }
}
