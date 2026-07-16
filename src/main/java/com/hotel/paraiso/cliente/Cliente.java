package com.hotel.paraiso.cliente;

import com.hotel.paraiso.common.audit.AuditableEntity;
import com.hotel.paraiso.common.crud.Activable;
import com.hotel.paraiso.reserva.Reserva;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Entidad que representa a un cliente del hotel.
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "reservas")
public class Cliente extends AuditableEntity implements Activable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "tipo_documento", nullable = false, length = 20)
    private String tipoDocumento; // CC, CE, Pasaporte, NIT

    @Column(name = "numero_documento", nullable = false, unique = true, length = 30)
    private String numeroDocumento;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Column(name = "pais", length = 80)
    private String pais;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    // ─── Relaciones ────────────────────────────────────
    // Un Cliente puede tener muchas Reservas (1:N)
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reserva> reservas;
}
