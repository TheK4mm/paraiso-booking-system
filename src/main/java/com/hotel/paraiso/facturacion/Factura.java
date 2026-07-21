package com.hotel.paraiso.facturacion;

import com.hotel.paraiso.common.audit.AuditableEntity;
import com.hotel.paraiso.reserva.Reserva;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad que representa la factura generada al finalizar una reserva.
 * Relación 1:1 con Reserva y 1:N con Pago.
 */
@Entity
@Table(name = "facturas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"reserva", "pagos"})
public class Factura extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_factura", nullable = false, unique = true, length = 30)
    private String numeroFactura; // ej: "FAC-2024-000001"

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "impuesto_porcentaje", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal impuestoPorcentaje = new BigDecimal("19.00"); // IVA Colombia

    @Column(name = "impuesto_valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal impuestoValor;

    @Column(name = "descuento", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "notas", length = 500)
    private String notas;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_factura", nullable = false, length = 20)
    @Builder.Default
    private EstadoFactura estadoFactura = EstadoFactura.PENDIENTE;

    @CreationTimestamp
    @Column(name = "fecha_emision", updatable = false)
    private LocalDateTime fechaEmision;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // ─── Relaciones ────────────────────────────────────

    // Una Factura pertenece a una Reserva (1:1)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    private Reserva reserva;

    // Una Factura tiene muchos Pagos (1:N)
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pago> pagos;

    public enum EstadoFactura {
        PENDIENTE, PAGADA_PARCIALMENTE, PAGADA, ANULADA
    }
}
