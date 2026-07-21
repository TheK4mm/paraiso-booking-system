package com.hotel.paraiso.servicio;

import com.hotel.paraiso.common.audit.AuditableEntity;
import com.hotel.paraiso.common.crud.Activable;
import com.hotel.paraiso.reserva.Reserva;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Entidad que representa servicios adicionales del hotel
 * (spa, desayuno, transfer, lavandería, etc.)
 */
@Entity
@Table(name = "servicios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "reservas")
public class Servicio extends AuditableEntity implements Activable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre; // ej: "Desayuno Buffet", "Servicio de Spa"

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 30)
    private CategoriaServicio categoria;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    // ─── Relaciones ────────────────────────────────────
    // Un Servicio puede estar en muchas Reservas (N:M)
    @ManyToMany(mappedBy = "servicios", fetch = FetchType.LAZY)
    private List<Reserva> reservas;

    public enum CategoriaServicio {
        ALIMENTACION, SPA_BIENESTAR, TRANSPORTE, ENTRETENIMIENTO,
        LAVANDERIA, NEGOCIOS, OTROS
    }
}
