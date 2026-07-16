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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
