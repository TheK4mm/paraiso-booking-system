package com.hotel.paraiso.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Alta de usuarios desde el módulo de administración (permite elegir rol). */
@Getter
@Setter
@NoArgsConstructor
public class UsuarioAdminRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "Solo letras, números, punto, guion y guion bajo")
    private String username;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 150)
    private String nombreCompleto;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;
}
