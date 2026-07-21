package com.hotel.paraiso.security;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.cliente.ClienteRepository;
import com.hotel.paraiso.common.email.EmailSender;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificacionEmailServiceTest {

    @Mock private TokenVerificacionEmailRepository tokenRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private EmailSender emailSender;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VerificacionEmailService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "https://hotelparaiso.test");
        lenient().when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(tokenRepository.save(any(TokenVerificacionEmail.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private Usuario cliente() {
        return Usuario.builder().id(5L).username("laura@example.com").nombreCompleto("Laura Mejía")
                .email("laura@example.com").rol(Rol.CLIENTE).emailVerificado(false).build();
    }

    private TokenVerificacionEmail tokenVigente(Usuario usuario) {
        return TokenVerificacionEmail.builder()
                .tokenHash(sha256("tok"))
                .expiraEn(LocalDateTime.now().plusHours(1))
                .usuario(usuario)
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

    // ─── emitirEnlace ───

    @Test
    void elEnlaceUsaLaUrlPublicaConfigurada() {
        service.emitirEnlace(cliente());

        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(emailSender).enviar(eq("laura@example.com"), anyString(), cuerpo.capture());
        assertThat(cuerpo.getValue()).contains("https://hotelparaiso.test/verificar-email?token=");
    }

    @Test
    void soloSePersisteElHashDelToken() {
        service.emitirEnlace(cliente());

        ArgumentCaptor<TokenVerificacionEmail> tokenCaptor =
                ArgumentCaptor.forClass(TokenVerificacionEmail.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(emailSender).enviar(anyString(), anyString(), cuerpo.capture());

        String hash = tokenCaptor.getValue().getTokenHash();
        assertThat(hash).hasSize(64);
        assertThat(cuerpo.getValue()).doesNotContain(hash);
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
    void verificarMarcaElEmailYConsumeElToken() {
        Usuario usuario = cliente();
        usuario.setCliente(Cliente.builder().email("laura@example.com").build());
        TokenVerificacionEmail token = tokenVigente(usuario);
        when(tokenRepository.findByTokenHash(sha256("tok"))).thenReturn(Optional.of(token));

        service.verificar("tok");

        assertThat(usuario.getEmailVerificado()).isTrue();
        assertThat(token.getUsadoEn()).isNotNull();
        // Ya tenía ficha desde el registro: no se vuelve a buscar
        verify(clienteRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void verificarVinculaLaFichaPendientePorEmailSinSobrescribirla() {
        Usuario usuario = cliente();
        Cliente existente = Cliente.builder().nombre("Laura del Carmen").email("laura@example.com").build();
        existente.setId(40L);
        when(tokenRepository.findByTokenHash(sha256("tok"))).thenReturn(Optional.of(tokenVigente(usuario)));
        when(clienteRepository.findByEmailIgnoreCase("laura@example.com")).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByClienteId(40L)).thenReturn(false);

        service.verificar("tok");

        assertThat(usuario.getCliente()).isSameAs(existente);
        assertThat(existente.getNombre()).isEqualTo("Laura del Carmen");
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void verificarRechazaFichaYaVinculadaAOtraCuenta() {
        Usuario usuario = cliente();
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
    void reenviarNoHaceNadaSiLaCuentaNoExiste() {
        when(usuarioRepository.findByEmailIgnoreCase("nadie@example.com")).thenReturn(Optional.empty());

        service.reenviar("nadie@example.com");

        verify(tokenRepository, never()).save(any());
        verify(emailSender, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void reenviarNoHaceNadaSiElEmailYaEstaVerificado() {
        Usuario usuario = cliente();
        usuario.setEmailVerificado(true);
        when(usuarioRepository.findByEmailIgnoreCase("laura@example.com")).thenReturn(Optional.of(usuario));

        service.reenviar("laura@example.com");

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void reenviarEmiteUnTokenNuevo() {
        when(usuarioRepository.findByEmailIgnoreCase("laura@example.com"))
                .thenReturn(Optional.of(cliente()));

        service.reenviar("laura@example.com");

        verify(tokenRepository).save(any(TokenVerificacionEmail.class));
        verify(emailSender).enviar(eq("laura@example.com"), anyString(), anyString());
    }
}
