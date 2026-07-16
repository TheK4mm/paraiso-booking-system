package com.hotel.paraiso.habitacion;

import com.hotel.paraiso.common.audit.AuditableEntity;
import com.hotel.paraiso.common.crud.Activable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Entidad que representa el tipo/categoría de una habitación
 * (ej: Suite, Doble, Individual, Junior Suite, etc.)
 */
@Entity
@Table(name = "tipos_habitacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "habitaciones")
public class TipoHabitacion extends AuditableEntity implements Activable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre; // ej: "Suite Presidencial", "Doble Estándar"

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "capacidad_maxima", nullable = false)
    private Integer capacidadMaxima;

    @Column(name = "precio_base_noche", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBaseNoche;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    // ─── Relaciones ────────────────────────────────────
    // Un TipoHabitacion puede tener muchas Habitaciones (1:N)
    @OneToMany(mappedBy = "tipoHabitacion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Habitacion> habitaciones;
}
