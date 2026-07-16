package com.hotel.paraiso.security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Token de verificación de email para cuentas CLIENTE. Se persiste solo
 * el hash SHA-256; el valor en claro viaja únicamente en el enlace.
 * Lleva además el payload de la ficha de cliente pendiente: la entidad
 * Cliente se crea solo tras verificar, así que sus datos obligatorios
 * deben sobrevivir entre el registro y la verificación.
 */
@Entity
@Table(name = "tokens_verificacion_email")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "usuario")
public class TokenVerificacionEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "usado_en")
    private LocalDateTime usadoEn;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // ─── Payload de la ficha de cliente pendiente ───

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "tipo_documento", nullable = false, length = 20)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @Column(name = "telefono", length = 20)
    private String telefono;

    public boolean estaVigente() {
        return usadoEn == null && expiraEn.isAfter(LocalDateTime.now());
    }
}
