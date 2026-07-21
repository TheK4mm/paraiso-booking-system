package com.hotel.paraiso.security;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Cuenta de acceso al sistema. Independiente de Empleado: un empleado
 * puede existir sin cuenta y viceversa (p.ej. el administrador).
 * Las cuentas CLIENTE se vinculan 1:1 con su ficha de {@link Cliente}
 * tras verificar el email (los huéspedes inician sesión con su email).
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 150)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /**
     * Distinto de {@code activo}: activo es la sanción administrativa;
     * esto marca que el dueño del email demostró controlarlo.
     */
    @Column(name = "email_verificado", nullable = false)
    @Builder.Default
    private Boolean emailVerificado = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
}
