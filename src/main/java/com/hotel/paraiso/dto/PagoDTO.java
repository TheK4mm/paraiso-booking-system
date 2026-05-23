package com.hotel.paraiso.dto;

import com.hotel.paraiso.model.Pago.EstadoPago;
import com.hotel.paraiso.model.Pago.MetodoPago;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class PagoDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Request {

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        private BigDecimal monto;

        @NotNull(message = "El método de pago es obligatorio")
        private MetodoPago metodoPago;

        @Size(max = 100)
        private String referenciaTransaccion;

        @Size(max = 300)
        private String descripcion;

        @NotNull(message = "El ID de reserva es obligatorio")
        private Long reservaId;

        private Long facturaId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private BigDecimal monto;
        private MetodoPago metodoPago;
        private String referenciaTransaccion;
        private EstadoPago estadoPago;
        private String descripcion;
        private LocalDateTime fechaPago;
        private Long reservaId;
        private String codigoReserva;
        private Long facturaId;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", this.id);
            map.put("monto", this.monto);
            map.put("metodoPago", this.metodoPago);
            map.put("referenciaTransaccion", this.referenciaTransaccion);
            map.put("estadoPago", this.estadoPago);
            map.put("descripcion", this.descripcion);
            map.put("fechaPago", this.fechaPago);
            map.put("reservaId", this.reservaId);
            map.put("codigoReserva", this.codigoReserva);
            map.put("facturaId", this.facturaId);
            return map;
        }
    }
}
