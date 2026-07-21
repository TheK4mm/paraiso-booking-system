package com.hotel.paraiso.security;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.cliente.ClienteRepository;
import com.hotel.paraiso.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class RegistroClienteServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private VerificacionEmailService verificacionEmailService;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RegistroClienteService service;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
        lenient().when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
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

    @Test
    void laCuentaNaceActivaYConFichaPropiaCuandoElEmailEsNuevo() {
        when(usuarioRepository.existsByEmailIgnoreCase("laura@example.com")).thenReturn(false);
        when(clienteRepository.findByNumeroDocumento("1002003001")).thenReturn(Optional.empty());
        when(clienteRepository.findByEmailIgnoreCase("laura@example.com")).thenReturn(Optional.empty());

        service.registrar(registro());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(guardado.getUsername()).isEqualTo("laura@example.com");
        // Puede iniciar sesión de inmediato: la verificación es opcional
        assertThat(guardado.getActivo()).isTrue();
        assertThat(guardado.getEmailVerificado()).isFalse();
        assertThat(guardado.getCliente()).isNotNull();
        assertThat(guardado.getCliente().getNumeroDocumento()).isEqualTo("1002003001");

        verify(verificacionEmailService).emitirEnlace(guardado);
    }

    @Test
    void laFichaAjenaNoSeVinculaHastaVerificarElEmail() {
        Cliente huespedPrevio = Cliente.builder().email("laura@example.com").build();
        huespedPrevio.setId(40L);
        when(usuarioRepository.existsByEmailIgnoreCase("laura@example.com")).thenReturn(false);
        when(clienteRepository.findByNumeroDocumento("1002003001")).thenReturn(Optional.empty());
        when(clienteRepository.findByEmailIgnoreCase("laura@example.com"))
                .thenReturn(Optional.of(huespedPrevio));

        service.registrar(registro());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        // Activa igualmente, pero sin acceso al historial de ese email
        assertThat(guardado.getActivo()).isTrue();
        assertThat(guardado.getCliente()).isNull();
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void rechazaEmailYaRegistrado() {
        when(usuarioRepository.existsByEmailIgnoreCase("laura@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(registro()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ya está registrado");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaDocumentoAsociadoAOtroEmail() {
        when(usuarioRepository.existsByEmailIgnoreCase("laura@example.com")).thenReturn(false);
        when(clienteRepository.findByNumeroDocumento("1002003001"))
                .thenReturn(Optional.of(Cliente.builder().email("otra@persona.com").build()));

        assertThatThrownBy(() -> service.registrar(registro()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("otro email");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaContrasenasQueNoCoinciden() {
        RegistroClienteRequest request = registro();
        request.setConfirmarPassword("otra-cosa");

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no coinciden");
        verify(usuarioRepository, never()).save(any());
    }
}
