package com.hotel.paraiso.reserva;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.common.audit.AuditableEntity;
import com.hotel.paraiso.empleado.Empleado;
import com.hotel.paraiso.facturacion.Factura;
import com.hotel.paraiso.facturacion.Pago;
import com.hotel.paraiso.habitacion.Habitacion;
import com.hotel.paraiso.servicio.Servicio;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Entidad central del sistema. Representa una reserva de habitaciones
 * hecha por un cliente, gestionada por un empleado.
 *
 * Relaciones:
 *  - N:1 con Cliente
 *  - N:1 con Empleado
 *  - N:M con Habitacion
 *  - N:M con Servicio
 *  - 1:N con Pago
 *  - 1:1 con Factura
 */
@Entity
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cliente", "empleado", "habitaciones", "servicios", "pagos", "factura"})
public class Reserva extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_reserva", nullable = false, unique = true, length = 20)
    private String codigoReserva; // ej: "RES-2024-000001"

    @Column(name = "fecha_entrada", nullable = false)
    private LocalDate fechaEntrada;

    @Column(name = "fecha_salida", nullable = false)
    private LocalDate fechaSalida;

    @Column(name = "numero_huespedes", nullable = false)
    private Integer numeroHuespedes;

    @Column(name = "total_noches", nullable = false)
    private Integer totalNoches;

    @Column(name = "precio_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // ─── Relaciones ────────────────────────────────────

    // Muchas Reservas pertenecen a un Cliente (N:1)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Muchas Reservas son gestionadas por un Empleado (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    // Una Reserva puede incluir muchas Habitaciones y viceversa (N:M)
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "reserva_habitacion",
            joinColumns = @JoinColumn(name = "reserva_id"),
            inverseJoinColumns = @JoinColumn(name = "habitacion_id")
    )
    private List<Habitacion> habitaciones;

    // Una Reserva puede incluir muchos Servicios y viceversa (N:M)
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "reserva_servicio",
            joinColumns = @JoinColumn(name = "reserva_id"),
            inverseJoinColumns = @JoinColumn(name = "servicio_id")
    )
    private List<Servicio> servicios;

    // Una Reserva puede tener muchos Pagos (1:N)
    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pago> pagos;

    // Una Reserva tiene exactamente una Factura (1:1)
    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Factura factura;

    public enum EstadoReserva {
        PENDIENTE, CONFIRMADA, CHECKIN, CHECKOUT, CANCELADA, NO_SHOW
    }
}
