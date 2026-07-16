package com.hotel.paraiso.cliente;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class ClienteResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String nombreCompleto;
    private String tipoDocumento;
    private String numeroDocumento;
    private String email;
    private String telefono;
    private String direccion;
    private String pais;
    private Boolean activo;
    private LocalDateTime creadoEn;

    /** Solo se calcula en la vista de detalle (evita N+1 en listados). */
    private Long totalReservas;
}
