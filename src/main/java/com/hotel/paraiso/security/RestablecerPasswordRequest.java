package com.hotel.paraiso.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nueva contraseña elegida desde el enlace del correo. El token viaja en
 * un campo oculto del propio panel del modal.
 */
@Getter
@Setter
@NoArgsConstructor
public class RestablecerPasswordRequest {

    @NotBlank(message = "Falta el token de restablecimiento")
    private String token;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "Debe confirmar la contraseña")
    private String confirmarPassword;
}
