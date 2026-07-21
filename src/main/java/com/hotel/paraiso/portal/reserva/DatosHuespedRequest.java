package com.hotel.paraiso.portal.reserva;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Paso 3 del flujo público: datos del titular de la reserva. Para un
 * cliente autenticado los campos de identidad se ignoran en el servidor
 * (manda su ficha vinculada); para un invitado alimentan el get-or-create.
 */
@Getter
@Setter
@NoArgsConstructor
public class DatosHuespedRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100)
    private String apellido;

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "CC|CE|PASAPORTE|NIT", message = "Tipo de documento inválido")
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 30)
    private String numeroDocumento;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 150)
    private String email;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String telefono;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observaciones;
}
