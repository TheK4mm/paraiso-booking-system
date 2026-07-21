package com.hotel.paraiso.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Reglas de seguridad de extremo a extremo: redirección a login, 401 JSON
 * en la API, separación de roles y protección CSRF.
 * Se omiten automáticamente si Docker no está disponible.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class SecurityFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void elNavegadorAnonimoEsRedirigidoAlPortal() throws Exception {
        mockMvc.perform(get("/reservas").accept("text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?auth=login"));
    }

    @Test
    void laApiAnonimaResponde401SinRedireccion() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }

    /** El módulo de autenticación aislado ya no existe: solo redirige al portal. */
    @Test
    void lasRutasDeAuthRedirigenAlModalDelPortal() throws Exception {
        mockMvc.perform(get("/login")).andExpect(redirectedUrl("/?auth=login"));
        mockMvc.perform(get("/registro")).andExpect(redirectedUrl("/?auth=registro"));
        mockMvc.perform(get("/recuperar")).andExpect(redirectedUrl("/?auth=recuperar"));
    }

    @Test
    void elPortalAbreElModalConElParametroAuth() throws Exception {
        mockMvc.perform(get("/").param("auth", "login").accept("text/html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hpAuthModal")));
    }

    @Test
    void elPortalPublicoEsAccesibleSinSesion() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/reservar")).andExpect(status().isOk());
        mockMvc.perform(get("/consulta-reserva")).andExpect(status().isOk());
    }

    @Test
    void unPostPublicoSinTokenCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/consulta-reserva")
                        .param("codigo", "RES-2026-000001").param("email", "x@x.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "recepcion", roles = "RECEPCIONISTA")
    void elRecepcionistaNoAccedeAAdministracion() throws Exception {
        mockMvc.perform(get("/usuarios")).andExpect(status().isForbidden());
        mockMvc.perform(get("/empleados")).andExpect(status().isForbidden());
        mockMvc.perform(get("/actividad")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/empleados")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "recepcion", roles = "RECEPCIONISTA")
    void elRecepcionistaOperaSusModulos() throws Exception {
        mockMvc.perform(get("/reservas")).andExpect(status().isOk());
        mockMvc.perform(get("/clientes")).andExpect(status().isOk());
        mockMvc.perform(get("/api/clientes")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "recepcion", roles = "RECEPCIONISTA")
    void elRecepcionistaNoEscribeCatalogos() throws Exception {
        mockMvc.perform(post("/servicios").with(csrf())
                        .param("nombre", "X").param("precio", "1").param("categoria", "OTROS"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unPostSinTokenCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/clientes").param("nombre", "X"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void elAdminAccedeATodo() throws Exception {
        mockMvc.perform(get("/usuarios")).andExpect(status().isOk());
        mockMvc.perform(get("/actividad")).andExpect(status().isOk());
        mockMvc.perform(get("/dashboard")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cliente@example.com", roles = "CLIENTE")
    void elClienteAccedeASuCuenta() throws Exception {
        mockMvc.perform(get("/mi-cuenta")).andExpect(status().isOk());
        mockMvc.perform(get("/mi-cuenta/reservas")).andExpect(status().isOk());
        mockMvc.perform(get("/mi-cuenta/perfil")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "recepcion", roles = "RECEPCIONISTA")
    void elPersonalNoAccedeAlAreaDeClientes() throws Exception {
        mockMvc.perform(get("/mi-cuenta")).andExpect(status().isForbidden());
    }

    // ─── Login del modal del portal (AJAX) vs navegación clásica ───

    @Test
    void elLoginAjaxValidoRespondeJsonSinRedireccion() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("username", "admin").param("password", "admin123"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json("{\"redirect\":null}"));
    }

    @Test
    void elLoginAjaxInvalidoResponde401ConMensaje() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("username", "noexiste").param("password", "mal"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos."));
    }

    @Test
    void elLoginClasicoInvalidoVuelveAlPortalConElModalAbierto() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "noexiste").param("password", "mal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?auth=login&error"));
    }

    // ─── Cierre de sesión: los tres roles vuelven al portal ───
    // Accept: text/html es obligatorio — Spring Security negocia el logout y
    // responde 204 sin redirección a un cliente que no pide HTML.

    @Test
    @WithMockUser(username = "cliente@example.com", roles = "CLIENTE")
    void elClienteCierraSesionHaciaElPortal() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()).accept(MediaType.TEXT_HTML))
                .andExpect(redirectedUrl("/?logout"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void elAdminCierraSesionHaciaElPortal() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()).accept(MediaType.TEXT_HTML))
                .andExpect(redirectedUrl("/?logout"));
    }

    @Test
    @WithMockUser(username = "recepcion", roles = "RECEPCIONISTA")
    void elRecepcionistaCierraSesionHaciaElPortal() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()).accept(MediaType.TEXT_HTML))
                .andExpect(redirectedUrl("/?logout"));
    }

    // ─── Regresión: 403 tras iniciar sesión como CLIENTE ───

    /**
     * Una URL de back-office pedida mientras el visitante era anónimo queda
     * en el RequestCache; honrarla tras un login de CLIENTE lo mandaba a una
     * zona prohibida y producía un 403 justo después de autenticarse.
     */
    @Test
    void elClienteNoEsArrastradoAlDestinoGuardadoDeOtroRol() throws Exception {
        String email = registrarClienteNuevo();

        MockHttpSession sesion = new MockHttpSession();
        mockMvc.perform(get("/dashboard").accept("text/html").session(sesion))
                .andExpect(redirectedUrl("/?auth=login"));

        // Aterriza en su zona, NO en el /dashboard que quedó cacheado
        mockMvc.perform(post("/login").with(csrf()).session(sesion)
                        .param("username", email).param("password", CLAVE))
                .andExpect(redirectedUrl("/mi-cuenta"));
        mockMvc.perform(get("/mi-cuenta").session(sesion)).andExpect(status().isOk());
    }

    /** En el modal, un destino descartado deja al huésped donde estaba. */
    @Test
    void elLoginAjaxDelClienteIgnoraElDestinoDeOtroRol() throws Exception {
        String email = registrarClienteNuevo();

        MockHttpSession sesion = new MockHttpSession();
        mockMvc.perform(get("/dashboard").accept("text/html").session(sesion));

        mockMvc.perform(post("/login").with(csrf()).session(sesion)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("username", email).param("password", CLAVE))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"redirect\":null}"));
    }

    /** El personal sí conserva su destino interceptado. */
    @Test
    void elPersonalConservaElDestinoGuardado() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mockMvc.perform(get("/actividad").accept("text/html").session(sesion))
                .andExpect(redirectedUrl("/?auth=login"));

        // Spring Security añade ?continue al reanudar el destino guardado
        mockMvc.perform(post("/login").with(csrf()).session(sesion)
                        .param("username", "admin").param("password", "admin123"))
                .andExpect(redirectedUrlPattern("**/actividad*"));
    }

    /** Peticiones que el navegador emite solo no deben ensuciar el cache. */
    @Test
    void lasPeticionesNoNavegablesNoSeGuardanComoDestino() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mockMvc.perform(get("/reservas").accept("application/json")
                .header("X-Requested-With", "XMLHttpRequest").session(sesion));

        mockMvc.perform(post("/login").with(csrf()).session(sesion)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("username", "admin").param("password", "admin123"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"redirect\":null}"));
    }

    // ─── Registro: la cuenta nace activa y entra de inmediato ───

    @Test
    void elClienteRecienRegistradoIniciaSesionSinVerificarElEmail() throws Exception {
        String email = registrarClienteNuevo();

        mockMvc.perform(post("/login").with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("username", email).param("password", CLAVE))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"redirect\":null}"));
    }

    @Test
    void elClienteRecienRegistradoEntraASuCuenta() throws Exception {
        String email = registrarClienteNuevo();

        MockHttpSession sesion = new MockHttpSession();
        mockMvc.perform(post("/login").with(csrf()).session(sesion)
                        .param("username", email).param("password", CLAVE))
                .andExpect(redirectedUrl("/mi-cuenta"));
        // Sin 403: la ficha se creó y vinculó en el propio registro
        mockMvc.perform(get("/mi-cuenta").session(sesion)).andExpect(status().isOk());
    }

    private static final String CLAVE = "clave-segura";

    /** Registra un CLIENTE por el flujo público y devuelve su email. */
    private String registrarClienteNuevo() throws Exception {
        long unico = System.nanoTime();
        String email = "huesped-" + unico + "@example.com";
        mockMvc.perform(post("/registro").with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("nombre", "Nuevo").param("apellido", "Huésped")
                        .param("tipoDocumento", "CC")
                        .param("numeroDocumento", String.valueOf(unico))
                        .param("email", email).param("telefono", "3001112233")
                        .param("password", CLAVE).param("confirmarPassword", CLAVE))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/auth :: registroExito"));
        return email;
    }

    @Test
    void laRecuperacionAjaxConErroresDevuelveSuPaneDelModal() throws Exception {
        mockMvc.perform(post("/recuperar").with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("email", "no-es-un-email"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(view().name("fragments/auth :: recuperarPane"));
    }

    @Test
    void laRecuperacionAjaxValidaDevuelveElMensajeGenerico() throws Exception {
        mockMvc.perform(post("/recuperar").with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("email", "no-existe@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/auth :: recuperarExito"));
    }

    @Test
    void elRegistroAjaxConErroresDevuelveElPaneDelModal() throws Exception {
        mockMvc.perform(post("/registro").with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("nombre", "").param("apellido", "")
                        .param("tipoDocumento", "").param("numeroDocumento", "")
                        .param("email", "no-es-un-email").param("telefono", "")
                        .param("password", "corta").param("confirmarPassword", "distinta"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(view().name("fragments/auth :: registroPane"));
    }

    @Test
    @WithMockUser(username = "cliente@example.com", roles = "CLIENTE")
    void elClienteNoAccedeAlBackOffice() throws Exception {
        mockMvc.perform(get("/dashboard")).andExpect(status().isForbidden());
        mockMvc.perform(get("/reservas")).andExpect(status().isForbidden());
        mockMvc.perform(get("/clientes")).andExpect(status().isForbidden());
        mockMvc.perform(get("/facturas")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/clientes")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reservas")).andExpect(status().isForbidden());
    }
}
