package com.hotel.paraiso.portal.cuenta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Edición del perfil del huésped. Email y documento NO son editables
 * desde el portal: el email es el vínculo cuenta↔ficha y la clave de la
 * consulta de invitado; el documento es identidad legal. Ambos se
 * corrigen en recepción.
 */
@Getter
@Setter
@NoArgsConstructor
public class PerfilClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100)
    private String apellido;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String telefono;

    @Size(max = 300)
    private String direccion;

    @Size(max = 80)
    private String pais;
}
