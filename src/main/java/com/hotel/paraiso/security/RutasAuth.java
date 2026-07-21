package com.hotel.paraiso.security;

import java.util.Set;

/**
 * Destinos de autenticación dentro del portal público. Toda la
 * experiencia ocurre en el modal del portal: no existen páginas de login,
 * registro ni recuperación independientes. Las rutas {@code /login},
 * {@code /registro} y {@code /recuperar} sobreviven solo como
 * redirecciones internas (marcadores, enlaces de correo y el
 * {@code loginPage} del filtro de Spring Security).
 *
 * <p>Centralizado aquí porque estas URL las construyen a la vez el
 * controlador, el entry point, el handler de fallo y las plantillas.
 */
public final class RutasAuth {

    /** Portal con el modal abierto en el panel de inicio de sesión. */
    public static final String PORTAL_LOGIN = "/?auth=login";
    /** Portal con el modal abierto en el panel de registro. */
    public static final String PORTAL_REGISTRO = "/?auth=registro";
    /** Portal con el modal abierto en el panel de recuperación. */
    public static final String PORTAL_RECUPERAR = "/?auth=recuperar";
    /** Portal con el modal abierto en el panel de nueva contraseña. */
    public static final String PORTAL_RESTABLECER = "/?auth=restablecer";
    /** Portal sin modal: destino tras cerrar sesión. */
    public static final String PORTAL = "/";

    /** Nombres de panel admitidos en el parámetro {@code auth}. */
    public static final Set<String> PANELES =
            Set.of("login", "registro", "recuperar", "restablecer");

    private RutasAuth() {
    }
}
