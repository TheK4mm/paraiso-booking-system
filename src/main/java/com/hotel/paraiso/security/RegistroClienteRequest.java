package com.hotel.paraiso.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro público de huéspedes. Sin username: los clientes inician
 * sesión con su email. Los datos de identidad alimentan la ficha de
 * Cliente que se crea al verificar el email.
 */
@Getter
@Setter
@NoArgsConstructor
public class RegistroClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100)
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "CC|CE|PASAPORTE|NIT", message = "Tipo de documento inválido")
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 30)
    private String numeroDocumento;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String telefono;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "Debe confirmar la contraseña")
    private String confirmarPassword;
}
