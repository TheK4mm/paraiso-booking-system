package com.hotel.paraiso.security;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.cliente.ClienteRepository;
import com.hotel.paraiso.common.exception.BadRequestException;
import com.hotel.paraiso.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificacionEmailServiceTest {

    @Mock private TokenVerificacionEmailRepository tokenRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VerificacionEmailService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
        lenient().when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(tokenRepository.save(any(TokenVerificacionEmail.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(77L);
            return c;
        });
    }

    private RegistroClienteRequest registro() {
        RegistroClienteRequest r = new RegistroClienteRequest();
        r.setNombre("Laura");
        r.setApellido("Mejía");
        r.setEmail("laura@example.com");
        r.setTipoDocumento("CC");
        r.setNumeroDocumento("1002003001");
        r.setTelefono("3001234567");
        r.setPassword("clave-segura");
        r.setConfirmarPassword("clave-segura");
        return r;
    }

    private TokenVerificacionEmail tokenVigente(Usuario usuario) {
        return TokenVerificacionEmail.builder()
                .tokenHash(sha256("tok"))
                .expiraEn(LocalDateTime.now().plusHours(1))
                .usuario(usuario)
                .nombre("Laura").apellido("Mejía")
                .tipoDocumento("CC").numeroDocumento("1002003001")
                .telefono("3001234567")
                .build();
    }

    private static String sha256(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ─── registrarCliente ───

    @Test
    void elRegistroCreaCuentaClienteSinVerificarYEmiteToken() {
        when(usuarioRepository.existsByEmailIgnoreCase("laura@example.com")).thenReturn(false);
        when(clienteRepository.findByNumeroDocumento("1002003001")).thenReturn(Optional.empty());

        service.registrarCliente(registro());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(guardado.getUsername()).isEqualTo("laura@example.com");
        assertThat(guardado.getEmailVerificado()).isFalse();
        assertThat(guardado.getCliente()).isNull();

        ArgumentCaptor<TokenVerificacionEmail> tokenCaptor =
                ArgumentCaptor.forClass(TokenVerificacionEmail.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getNumeroDocumento()).isEqualTo("1002003001");
    }

    @Test
    void elRegistroRechazaEmailYaRegistrado() {
        when(usuarioRepository.existsByEmailIgnoreCase("laura@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarCliente(registro()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ya está registrado");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void elRegistroRechazaDocumentoAsociadoAOtroEmail() {
        when(usuarioRepository.existsByEmailIgnoreCase("laura@example.com")).thenReturn(false);
        when(clienteRepository.findByNumeroDocumento("1002003001"))
                .thenReturn(Optional.of(Cliente.builder().email("otra@persona.com").build()));

        assertThatThrownBy(() -> service.registrarCliente(registro()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("otro email");
        verify(usuarioRepository, never()).save(any());
    }

    // ─── verificar ───

    @Test
    void verificarRechazaTokenInvalidoOExpirado() {
        when(tokenRepository.findByTokenHash(sha256("malo"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verificar("malo"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inválido o expiró");
    }

    @Test
    void verificarVinculaClienteExistentePorEmailSinSobrescribir() {
        Usuario usuario = Usuario.builder().id(5L).username("laura@example.com")
                .email("laura@example.com").rol(Rol.CLIENTE).emailVerificado(false).build();
        Cliente existente = Cliente.builder().nombre("Laura del Carmen").email("laura@example.com").build();
        existente.setId(40L);
        when(tokenRepository.findByTokenHash(sha256("tok"))).thenReturn(Optional.of(tokenVigente(usuario)));
        when(clienteRepository.findByEmailIgnoreCase("laura@example.com")).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByClienteId(40L)).thenReturn(false);

        service.verificar("tok");

        assertThat(usuario.getEmailVerificado()).isTrue();
        assertThat(usuario.getCliente()).isSameAs(existente);
        assertThat(existente.getNombre()).isEqualTo("Laura del Carmen");
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void verificarCreaClienteDesdeElPayloadSiNoExiste() {
        Usuario usuario = Usuario.builder().id(5L).username("laura@example.com")
                .email("laura@example.com").rol(Rol.CLIENTE).emailVerificado(false).build();
        when(tokenRepository.findByTokenHash(sha256("tok"))).thenReturn(Optional.of(tokenVigente(usuario)));
        when(clienteRepository.findByEmailIgnoreCase("laura@example.com")).thenReturn(Optional.empty());
        when(clienteRepository.findByNumeroDocumento("1002003001")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByClienteId(77L)).thenReturn(false);

        service.verificar("tok");

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        Cliente creado = captor.getValue();
        assertThat(creado.getNombre()).isEqualTo("Laura");
        assertThat(creado.getNumeroDocumento()).isEqualTo("1002003001");
        assertThat(creado.getActivo()).isTrue();
        assertThat(usuario.getCliente()).isSameAs(creado);
    }

    @Test
    void verificarRechazaSiElDocumentoAparecioConOtroEmail() {
        Usuario usuario = Usuario.builder().id(5L).username("laura@example.com")
                .email("laura@example.com").rol(Rol.CLIENTE).emailVerificado(false).build();
        when(tokenRepository.findByTokenHash(sha256("tok"))).thenReturn(Optional.of(tokenVigente(usuario)));
        when(clienteRepository.findByEmailIgnoreCase("laura@example.com")).thenReturn(Optional.empty());
        when(clienteRepository.findByNumeroDocumento("1002003001"))
                .thenReturn(Optional.of(Cliente.builder().email("otra@persona.com").build()));

        assertThatThrownBy(() -> service.verificar("tok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("otro email");
    }

    @Test
    void verificarRechazaFichaYaVinculadaAOtraCuenta() {
        Usuario usuario = Usuario.builder().id(5L).username("laura@example.com")
                .email("laura@example.com").rol(Rol.CLIENTE).emailVerificado(false).build();
        Cliente existente = Cliente.builder().email("laura@example.com").build();
        existente.setId(40L);
        when(tokenRepository.findByTokenHash(sha256("tok"))).thenReturn(Optional.of(tokenVigente(usuario)));
        when(clienteRepository.findByEmailIgnoreCase("laura@example.com")).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByClienteId(40L)).thenReturn(true);

        assertThatThrownBy(() -> service.verificar("tok"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vinculada a otra cuenta");
    }

    // ─── reenviar ───

    @Test
    void reenviarNoHaceNadaSiLaCuentaNoExisteOYaVerifico() {
        when(usuarioRepository.findByEmailIgnoreCase("nadie@example.com")).thenReturn(Optional.empty());

        service.reenviar("nadie@example.com");

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void reenviarEmiteTokenNuevoCopiandoElPayload() {
        Usuario usuario = Usuario.builder().id(5L).username("laura@example.com")
                .email("laura@example.com").rol(Rol.CLIENTE).emailVerificado(false).build();
        when(usuarioRepository.findByEmailIgnoreCase("laura@example.com")).thenReturn(Optional.of(usuario));
        when(tokenRepository.findTopByUsuarioOrderByCreadoEnDesc(usuario))
                .thenReturn(Optional.of(tokenVigente(usuario)));

        service.reenviar("laura@example.com");

        ArgumentCaptor<TokenVerificacionEmail> captor =
                ArgumentCaptor.forClass(TokenVerificacionEmail.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getNumeroDocumento()).isEqualTo("1002003001");
    }
}
