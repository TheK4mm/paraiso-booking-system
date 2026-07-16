package com.hotel.paraiso.common.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Registro inmutable de actividad del sistema (quién hizo qué y cuándo).
 */
@Entity
@Table(name = "activity_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "accion", nullable = false, length = 60)
    private String accion;

    @Column(name = "tipo_entidad", length = 40)
    private String tipoEntidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "detalle", length = 500)
    private String detalle;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;
}
