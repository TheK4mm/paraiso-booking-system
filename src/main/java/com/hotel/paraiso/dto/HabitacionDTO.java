package com.hotel.paraiso.dto;

import com.hotel.paraiso.model.Habitacion.EstadoHabitacion;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class HabitacionDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Request {

        @NotBlank(message = "El número de habitación es obligatorio")
        @Size(max = 10, message = "El número no puede superar 10 caracteres")
        private String numero;

        @NotNull(message = "El piso es obligatorio")
        @Min(value = 1, message = "El piso mínimo es 1")
        private Integer piso;

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        private String descripcion;

        private EstadoHabitacion estado;

        @NotNull(message = "El tipo de habitación es obligatorio")
        private Long tipoHabitacionId;

        private Boolean activo;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String numero;
        private Integer piso;
        private String descripcion;
        private EstadoHabitacion estado;
        private Boolean activo;
        private Long tipoHabitacionId;
        private String tipoHabitacionNombre;
        private BigDecimal precioBaseNoche;
        private Integer capacidadMaxima;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", this.id);
            map.put("numero", this.numero);
            map.put("piso", this.piso);
            map.put("descripcion", this.descripcion);
            map.put("estado", this.estado);
            map.put("activo", this.activo);
            map.put("tipoHabitacionId", this.tipoHabitacionId);
            map.put("tipoHabitacionNombre", this.tipoHabitacionNombre);
            map.put("precioBaseNoche", this.precioBaseNoche);
            map.put("capacidadMaxima", this.capacidadMaxima);
            return map;
        }
    }
}
