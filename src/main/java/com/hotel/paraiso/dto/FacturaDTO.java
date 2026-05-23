package com.hotel.paraiso.dto;

import com.hotel.paraiso.model.Factura.EstadoFactura;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class FacturaDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Request {

        @NotNull(message = "El ID de la reserva es obligatorio")
        private Long reservaId;

        @DecimalMin(value = "0.00")
        private BigDecimal descuento;

        @DecimalMin(value = "0.00") @DecimalMax(value = "100.00")
        private BigDecimal impuestoPorcentaje;

        @Size(max = 500)
        private String notas;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String numeroFactura;
        private BigDecimal subtotal;
        private BigDecimal impuestoPorcentaje;
        private BigDecimal impuestoValor;
        private BigDecimal descuento;
        private BigDecimal total;
        private String notas;
        private EstadoFactura estadoFactura;
        private LocalDateTime fechaEmision;
        private Long reservaId;
        private String codigoReserva;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", this.id);
            map.put("numeroFactura", this.numeroFactura);
            map.put("subtotal", this.subtotal);
            map.put("impuestoPorcentaje", this.impuestoPorcentaje);
            map.put("impuestoValor", this.impuestoValor);
            map.put("descuento", this.descuento);
            map.put("total", this.total);
            map.put("notas", this.notas);
            map.put("estadoFactura", this.estadoFactura);
            map.put("fechaEmision", this.fechaEmision);
            map.put("reservaId", this.reservaId);
            map.put("codigoReserva", this.codigoReserva);
            return map;
        }
    }
}
