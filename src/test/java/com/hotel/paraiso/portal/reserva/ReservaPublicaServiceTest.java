package com.hotel.paraiso.portal.reserva;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.cliente.ClienteRepository;
import com.hotel.paraiso.common.exception.BadRequestException;
import com.hotel.paraiso.common.exception.BusinessException;
import com.hotel.paraiso.habitacion.Habitacion;
import com.hotel.paraiso.habitacion.Habitacion.EstadoHabitacion;
import com.hotel.paraiso.habitacion.HabitacionRepository;
import com.hotel.paraiso.habitacion.TipoHabitacion;
import com.hotel.paraiso.reserva.ReservaDetalleResponse;
import com.hotel.paraiso.reserva.ReservaRepository;
import com.hotel.paraiso.reserva.ReservaRequest;
import com.hotel.paraiso.reserva.ReservaService;
import com.hotel.paraiso.security.Usuario;
import com.hotel.paraiso.security.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaPublicaServiceTest {

    @Mock private HabitacionRepository habitacionRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ReservaService reservaService;

    @InjectMocks
    private ReservaPublicaService service;

    private final LocalDate entrada = LocalDate.now().plusDays(10);
    private final LocalDate salida = LocalDate.now().plusDays(13);

    private Habitacion habitacion;

    @BeforeEach
    void setUp() {
        TipoHabitacion tipo = TipoHabitacion.builder()
                .id(2L).nombre("Doble Estándar").capacidadMaxima(2)
                .precioBaseNoche(new BigDecimal("220000"))
                .comodidades("Wifi, TV, Minibar")
                .build();
        habitacion = Habitacion.builder()
                .id(5L).numero("201").piso(2)
                .estado(EstadoHabitacion.DISPONIBLE)
                .tipoHabitacion(tipo)
                .build();
        lenient().when(reservaService.create(any(ReservaRequest.class)))
                .thenReturn(ReservaDetalleResponse.builder().codigoReserva("RES-2026-000099").build());
        lenient().when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(33L);
            return c;
        });
    }

    private DatosHuespedRequest datos() {
        DatosHuespedRequest d = new DatosHuespedRequest();
        d.setNombre("Mario");
        d.setApellido("Rojas");
        d.setTipoDocumento("CC");
        d.setNumeroDocumento("555111222");
        d.setEmail("mario@example.com");
        return d;
    }

    private ReservaEnCurso enCurso() {
        return new ReservaEnCurso(5L, "201", "Doble Estándar", null,
                entrada, salida, 2, 3, new BigDecimal("220000"), new BigDecimal("660000"));
    }

    // ─── buscarDisponibles ───

    @Test
    void laBusquedaFiltraPorCapacidadYCalculaElTotal() {
        when(habitacionRepository.findHabitacionesReservables(entrada, salida))
                .thenReturn(List.of(habitacion));

        List<HabitacionCardView> cards = service.buscarDisponibles(entrada, salida, 2);

        assertThat(cards).hasSize(1);
        HabitacionCardView card = cards.get(0);
        assertThat(card.totalEstancia()).isEqualByComparingTo("660000");
        assertThat(card.comodidades()).containsExactly("Wifi", "TV", "Minibar");

        assertThat(service.buscarDisponibles(entrada, salida, 3)).isEmpty();
    }

    @Test
    void laBusquedaRechazaFechasInvalidas() {
        assertThatThrownBy(() -> service.buscarDisponibles(salida, entrada, 2))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.buscarDisponibles(
                LocalDate.now().minusDays(1), salida, 2))
                .isInstanceOf(BadRequestException.class);
    }

    // ─── prepararSeleccion ───

    @Test
    void laSeleccionCongelaElResumenSiSigueReservable() {
        when(habitacionRepository.findById(5L)).thenReturn(Optional.of(habitacion));
        when(reservaRepository.countReservasActivasParaHabitacion(5L, entrada, salida, -1L))
                .thenReturn(0L);

        ReservaEnCurso enCurso = service.prepararSeleccion(5L, entrada, salida, 2);

        assertThat(enCurso.numero()).isEqualTo("201");
        assertThat(enCurso.noches()).isEqualTo(3);
        assertThat(enCurso.total()).isEqualByComparingTo("660000");
    }

    @Test
    void laSeleccionRechazaSolapesYMantenimiento() {
        when(habitacionRepository.findById(5L)).thenReturn(Optional.of(habitacion));
        when(reservaRepository.countReservasActivasParaHabitacion(5L, entrada, salida, -1L))
                .thenReturn(1L);
        assertThatThrownBy(() -> service.prepararSeleccion(5L, entrada, salida, 2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("acaba de ser reservada");

        habitacion.setEstado(EstadoHabitacion.MANTENIMIENTO);
        assertThatThrownBy(() -> service.prepararSeleccion(5L, entrada, salida, 2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya no está disponible");
    }

    // ─── confirmar (get-or-create y delegación) ───

    @Test
    void elInvitadoConEmailNuevoCreaClienteYDelegaEnReservaService() {
        when(clienteRepository.findByEmailIgnoreCase("mario@example.com")).thenReturn(Optional.empty());
        when(clienteRepository.findByNumeroDocumento("555111222")).thenReturn(Optional.empty());

        ReservaDetalleResponse reserva = service.confirmar(enCurso(), datos(), null);

        assertThat(reserva.getCodigoReserva()).isEqualTo("RES-2026-000099");
        ArgumentCaptor<ReservaRequest> captor = ArgumentCaptor.forClass(ReservaRequest.class);
        verify(reservaService).create(captor.capture());
        ReservaRequest request = captor.getValue();
        assertThat(request.getClienteId()).isEqualTo(33L);
        assertThat(request.getHabitacionIds()).containsExactly(5L);
        assertThat(request.getEmpleadoId()).isNull();
    }

    @Test
    void elInvitadoConEmailExistenteReutilizaLaFichaSinSobrescribir() {
        Cliente existente = Cliente.builder().nombre("Mario Alberto").email("mario@example.com").build();
        existente.setId(8L);
        when(clienteRepository.findByEmailIgnoreCase("mario@example.com"))
                .thenReturn(Optional.of(existente));

        service.confirmar(enCurso(), datos(), null);

        verify(clienteRepository, never()).save(any());
        assertThat(existente.getNombre()).isEqualTo("Mario Alberto");
        ArgumentCaptor<ReservaRequest> captor = ArgumentCaptor.forClass(ReservaRequest.class);
        verify(reservaService).create(captor.capture());
        assertThat(captor.getValue().getClienteId()).isEqualTo(8L);
    }

    @Test
    void elInvitadoConDocumentoDeOtroEmailEsRechazado() {
        when(clienteRepository.findByEmailIgnoreCase("mario@example.com")).thenReturn(Optional.empty());
        when(clienteRepository.findByNumeroDocumento("555111222"))
                .thenReturn(Optional.of(Cliente.builder().email("otro@example.com").build()));

        assertThatThrownBy(() -> service.confirmar(enCurso(), datos(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("otro email");
        verify(reservaService, never()).create(any());
    }

    @Test
    void elClienteAutenticadoUsaSuFichaEIgnoraElFormulario() {
        Cliente vinculado = Cliente.builder().nombre("Andrés").email("andres@example.com").build();
        vinculado.setId(1L);
        Usuario usuario = Usuario.builder().username("andres@example.com").cliente(vinculado).build();
        when(usuarioRepository.findConClienteByUsernameIgnoreCase("andres@example.com"))
                .thenReturn(Optional.of(usuario));

        service.confirmar(enCurso(), datos(), "andres@example.com");

        verify(clienteRepository, never()).findByEmailIgnoreCase(any());
        verify(clienteRepository, never()).save(any());
        ArgumentCaptor<ReservaRequest> captor = ArgumentCaptor.forClass(ReservaRequest.class);
        verify(reservaService).create(captor.capture());
        assertThat(captor.getValue().getClienteId()).isEqualTo(1L);
    }

    @Test
    void elClienteInactivoNoPuedeReservarComoInvitado() {
        Cliente inactivo = Cliente.builder().email("mario@example.com").activo(false).build();
        inactivo.setId(9L);
        when(clienteRepository.findByEmailIgnoreCase("mario@example.com"))
                .thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> service.confirmar(enCurso(), datos(), null))
                .isInstanceOf(BusinessException.class);
        verify(reservaService, never()).create(any());
    }

    // ─── consultar ───

    @Test
    void laConsultaExigeCodigoYEmailCoincidentes() {
        Cliente cliente = Cliente.builder().email("mario@example.com").build();
        com.hotel.paraiso.reserva.Reserva reserva = com.hotel.paraiso.reserva.Reserva.builder()
                .id(4L).codigoReserva("RES-2026-000004").cliente(cliente).build();
        when(reservaRepository.findByCodigoReserva("RES-2026-000004")).thenReturn(Optional.of(reserva));
        lenient().when(reservaService.findDetalle(4L))
                .thenReturn(ReservaDetalleResponse.builder().codigoReserva("RES-2026-000004").build());

        assertThat(service.consultar("res-2026-000004", "MARIO@example.com")).isPresent();
        assertThat(service.consultar("RES-2026-000004", "intruso@example.com")).isEmpty();
        assertThat(service.consultar("RES-2026-999999", "mario@example.com")).isEmpty();
        assertThat(service.consultar("", "mario@example.com")).isEmpty();
    }
}
