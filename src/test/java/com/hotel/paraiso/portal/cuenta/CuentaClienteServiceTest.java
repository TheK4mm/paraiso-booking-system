package com.hotel.paraiso.portal.cuenta;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.cliente.ClienteRepository;
import com.hotel.paraiso.common.exception.BusinessException;
import com.hotel.paraiso.common.exception.ResourceNotFoundException;
import com.hotel.paraiso.reserva.Reserva;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import com.hotel.paraiso.reserva.ReservaRepository;
import com.hotel.paraiso.reserva.ReservaService;
import com.hotel.paraiso.security.Usuario;
import com.hotel.paraiso.security.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaClienteServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private ReservaService reservaService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CuentaClienteService service;

    private Cliente miCliente;
    private Usuario miUsuario;

    @BeforeEach
    void setUp() {
        miCliente = Cliente.builder().nombre("Andrés").apellido("Restrepo")
                .email("andres@example.com").build();
        miCliente.setId(1L);
        miUsuario = Usuario.builder().id(7L).username("andres@example.com")
                .email("andres@example.com").cliente(miCliente).build();
        lenient().when(usuarioRepository.findConClienteByUsernameIgnoreCase("andres@example.com"))
                .thenReturn(Optional.of(miUsuario));
        lenient().when(usuarioRepository.findByUsernameIgnoreCase("andres@example.com"))
                .thenReturn(Optional.of(miUsuario));
    }

    private Reserva reservaDe(Long clienteId, EstadoReserva estado) {
        Cliente dueno = Cliente.builder().build();
        dueno.setId(clienteId);
        return Reserva.builder().id(20L).codigoReserva("RES-2026-000020")
                .cliente(dueno).estado(estado).build();
    }

    @Test
    void elDetalleDeUnaReservaAjenaEs404NoConfirmaExistencia() {
        when(reservaRepository.findByCodigoReserva("RES-2026-000020"))
                .thenReturn(Optional.of(reservaDe(99L, EstadoReserva.CONFIRMADA)));

        assertThatThrownBy(() -> service.detalle("andres@example.com", "RES-2026-000020"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(reservaService, never()).findDetalle(20L);
    }

    @Test
    void elDetalleDeUnaReservaPropiaSeCarga() {
        when(reservaRepository.findByCodigoReserva("RES-2026-000020"))
                .thenReturn(Optional.of(reservaDe(1L, EstadoReserva.CONFIRMADA)));
        when(reservaService.findDetalle(20L)).thenReturn(
                com.hotel.paraiso.reserva.ReservaDetalleResponse.builder()
                        .codigoReserva("RES-2026-000020").build());

        assertThat(service.detalle("andres@example.com", "RES-2026-000020").getCodigoReserva())
                .isEqualTo("RES-2026-000020");
        verify(reservaService).findDetalle(20L);
    }

    @Test
    void elClienteCancelaPendientesYConfirmadas() {
        when(reservaRepository.findByCodigoReserva("RES-2026-000020"))
                .thenReturn(Optional.of(reservaDe(1L, EstadoReserva.PENDIENTE)));

        service.cancelar("andres@example.com", "RES-2026-000020");

        verify(reservaService).cancelar(20L);
    }

    @Test
    void elClienteNoCancelaUnCheckinEnCurso() {
        when(reservaRepository.findByCodigoReserva("RES-2026-000020"))
                .thenReturn(Optional.of(reservaDe(1L, EstadoReserva.CHECKIN)));

        assertThatThrownBy(() -> service.cancelar("andres@example.com", "RES-2026-000020"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pendientes o confirmadas");
        verify(reservaService, never()).cancelar(20L);
    }

    @Test
    void cancelarUnaReservaAjenaEs404() {
        when(reservaRepository.findByCodigoReserva("RES-2026-000020"))
                .thenReturn(Optional.of(reservaDe(99L, EstadoReserva.PENDIENTE)));

        assertThatThrownBy(() -> service.cancelar("andres@example.com", "RES-2026-000020"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(reservaService, never()).cancelar(20L);
    }

    @Test
    void actualizarPerfilTocaSoloCamposEditablesYSincronizaElNombre() {
        PerfilClienteRequest request = new PerfilClienteRequest();
        request.setNombre("Andrés Felipe");
        request.setApellido("Restrepo");
        request.setTelefono("3110001122");
        request.setPais("Colombia");

        service.actualizarPerfil("andres@example.com", request);

        assertThat(miCliente.getNombre()).isEqualTo("Andrés Felipe");
        assertThat(miCliente.getEmail()).isEqualTo("andres@example.com");
        assertThat(miUsuario.getNombreCompleto()).isEqualTo("Andrés Felipe Restrepo");
        verify(clienteRepository).save(miCliente);
        verify(usuarioRepository).save(miUsuario);
    }

    @Test
    void actualizarPerfilSinFichaVinculadaFallaConMensajeClaro() {
        miUsuario.setCliente(null);

        assertThatThrownBy(() -> service.actualizarPerfil("andres@example.com", new PerfilClienteRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ficha de cliente");
    }

    @Test
    void misReservasDeUnaCuentaSinFichaEsListaVacia() {
        miUsuario.setCliente(null);

        assertThat(service.misReservas("andres@example.com")).isEmpty();
    }
}
