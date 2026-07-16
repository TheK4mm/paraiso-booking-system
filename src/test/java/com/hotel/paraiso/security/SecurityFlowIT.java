package com.hotel.paraiso.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
    void elNavegadorAnonimoEsRedirigidoALogin() throws Exception {
        mockMvc.perform(get("/reservas").accept("text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void laApiAnonimaResponde401SinRedireccion() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elLoginEsPublico() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/registro")).andExpect(status().isOk());
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
    void elLoginClasicoInvalidoConservaLaRedireccionHistorica() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "noexiste").param("password", "mal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void elRegistroAjaxConErroresDevuelveElPaneDelModal() throws Exception {
        mockMvc.perform(post("/registro").with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("nombre", "").param("apellido", "")
                        .param("tipoDocumento", "").param("numeroDocumento", "")
                        .param("email", "no-es-un-email").param("telefono", "")
                        .param("password", "corta").param("confirmarPassword", "distinta"))
                .andExpect(status().isUnprocessableEntity())
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
