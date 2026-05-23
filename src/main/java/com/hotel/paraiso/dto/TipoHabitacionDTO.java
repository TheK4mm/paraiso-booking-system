package com.hotel.paraiso.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DTOs para TipoHabitacion.
 * Separan la capa de presentación de la capa de persistencia.
 */
public class TipoHabitacionDTO {

    /** Usado en POST/PUT para crear o actualizar */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Request {

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        private String nombre;

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        private String descripcion;

        @NotNull(message = "La capacidad máxima es obligatoria")
        @Min(value = 1, message = "La capacidad mínima es 1 persona")
        @Max(value = 20, message = "La capacidad máxima es 20 personas")
        private Integer capacidadMaxima;

        @NotNull(message = "El precio base por noche es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        private BigDecimal precioBaseNoche;

        private Boolean activo;
    }

    /** Usado en respuestas GET */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String nombre;
        private String descripcion;
        private Integer capacidadMaxima;
        private BigDecimal precioBaseNoche;
        private Boolean activo;
        private Integer totalHabitaciones;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", this.id);
            map.put("nombre", this.nombre);
            map.put("descripcion", this.descripcion);
            map.put("capacidadMaxima", this.capacidadMaxima);
            map.put("precioBaseNoche", this.precioBaseNoche);
            map.put("activo", this.activo);
            map.put("totalHabitaciones", this.totalHabitaciones);
            return map;
        }
    }
}
