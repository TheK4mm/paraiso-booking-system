package com.hotel.paraiso.empleado;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class EmpleadoResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String nombreCompleto;
    private String numeroDocumento;
    private String cargo;
    private String emailCorporativo;
    private String telefonoExtension;
    private LocalDate fechaContratacion;
    private Boolean activo;
    private LocalDateTime creadoEn;

    /** Solo se calcula en la vista de detalle (evita N+1 en listados). */
    private Long totalReservasGestionadas;
}
