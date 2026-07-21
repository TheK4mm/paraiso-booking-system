package com.hotel.paraiso.reserva;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.cliente.ClienteRepository;
import com.hotel.paraiso.common.exception.BadRequestException;
import com.hotel.paraiso.common.exception.BusinessException;
import com.hotel.paraiso.empleado.EmpleadoRepository;
import com.hotel.paraiso.facturacion.PagoRepository;
import com.hotel.paraiso.habitacion.Habitacion;
import com.hotel.paraiso.habitacion.Habitacion.EstadoHabitacion;
import com.hotel.paraiso.habitacion.HabitacionRepository;
import com.hotel.paraiso.habitacion.TipoHabitacion;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import com.hotel.paraiso.servicio.Servicio;
import com.hotel.paraiso.servicio.ServicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private HabitacionRepository habitacionRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private ReservaMapper reservaMapper;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReservaService service;

    private Cliente cliente;
    private Habitacion habitacion;
    private final LocalDate entrada = LocalDate.now().plusDays(10);
    private final LocalDate salida = LocalDate.now().plusDays(13); // 3 noches

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder().id(1L).nombre("Ana").apellido("Rojas")
                .email("ana@test.com").activo(true).build();
        TipoHabitacion tipo = TipoHabitacion.builder().id(1L).nombre("Doble")
                .capacidadMaxima(2).precioBaseNoche(new BigDecimal("100000")).activo(true).build();
        habitacion = Habitacion.builder().id(5L).numero("202").piso(2)
                .estado(EstadoHabitacion.DISPONIBLE).activo(true).tipoHabitacion(tipo).build();

        // findDetalle se invoca al final de las operaciones de escritura
        lenient().when(reservaRepository.findByIdConHabitaciones(anyLong()))
                .thenAnswer(inv -> reservaRepository.findById(inv.getArgument(0)));
        lenient().when(pagoRepository.sumPagosAprobadosByReservaId(anyLong())).thenReturn(BigDecimal.ZERO);
        lenient().when(reservaMapper.toDetalle(any())).thenReturn(ReservaDetalleResponse.builder().build());
    }

    private ReservaRequest request() {
        ReservaRequest r = new ReservaRequest();
        r.setFechaEntrada(entrada);
        r.setFechaSalida(salida);
        r.setNumeroHuespedes(2);
        r.setClienteId(1L);
        r.setHabitacionIds(List.of(5L));
        return r;
    }

    @Nested
    class Crear {

        @Test
        void calculaPrecioYGeneraCodigoPorSecuencia() {
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(habitacionRepository.findAllByIdForUpdate(List.of(5L))).thenReturn(List.of(habitacion));
            when(reservaRepository.countReservasActivasParaHabitacion(anyLong(), any(), any(), anyLong()))
                    .thenReturn(0L);
            when(reservaRepository.nextCodigoSeq()).thenReturn(42L);
            when(reservaRepository.save(any())).thenAnswer(inv -> {
                Reserva r = inv.getArgument(0);
                r.setId(99L);
                return r;
            });
            when(reservaRepository.findById(99L))
                    .thenReturn(Optional.of(Reserva.builder().id(99L).precioTotal(BigDecimal.ONE).build()));

            service.create(request());

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).save(captor.capture());
            Reserva guardada = captor.getValue();
            assertThat(guardada.getCodigoReserva())
                    .isEqualTo(String.format("RES-%d-000042", Year.now().getValue()));
            assertThat(guardada.getPrecioTotal()).isEqualByComparingTo(new BigDecimal("300000")); // 100k × 3 noches
            assertThat(guardada.getTotalNoches()).isEqualTo(3);
            assertThat(guardada.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        }

        @Test
        void sumaServiciosAlPrecio() {
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(habitacionRepository.findAllByIdForUpdate(List.of(5L))).thenReturn(List.of(habitacion));
            when(reservaRepository.countReservasActivasParaHabitacion(anyLong(), any(), any(), anyLong()))
                    .thenReturn(0L);
            when(reservaRepository.nextCodigoSeq()).thenReturn(1L);
            Servicio spa = Servicio.builder().id(3L).nombre("Spa").precio(new BigDecimal("85000")).build();
            when(servicioRepository.findAllById(List.of(3L))).thenReturn(List.of(spa));
            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            when(reservaRepository.save(captor.capture())).thenAnswer(inv -> {
                Reserva r = inv.getArgument(0);
                r.setId(99L);
                return r;
            });
            when(reservaRepository.findById(99L))
                    .thenReturn(Optional.of(Reserva.builder().id(99L).precioTotal(BigDecimal.ONE).build()));

            ReservaRequest req = request();
            req.setServicioIds(List.of(3L));
            service.create(req);

            assertThat(captor.getValue().getPrecioTotal()).isEqualByComparingTo(new BigDecimal("385000"));
        }

        @Test
        void rechazaFechaEntradaPasada() {
            ReservaRequest req = request();
            req.setFechaEntrada(LocalDate.now().minusDays(1));
            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("pasado");
        }

        @Test
        void rechazaClienteInactivo() {
            cliente.setActivo(false);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            assertThatThrownBy(() -> service.create(request()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("inactivo");
        }

        @Test
        void rechazaHabitacionConReservaSolapada() {
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(habitacionRepository.findAllByIdForUpdate(List.of(5L))).thenReturn(List.of(habitacion));
            when(reservaRepository.countReservasActivasParaHabitacion(anyLong(), any(), any(), anyLong()))
                    .thenReturn(1L);
            assertThatThrownBy(() -> service.create(request()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya está reservada");
        }

        @Test
        void rechazaHabitacionEnMantenimiento() {
            habitacion.setEstado(EstadoHabitacion.MANTENIMIENTO);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(habitacionRepository.findAllByIdForUpdate(List.of(5L))).thenReturn(List.of(habitacion));
            assertThatThrownBy(() -> service.create(request()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no está disponible");
        }

        @Test
        void rechazaCapacidadInsuficiente() {
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(habitacionRepository.findAllByIdForUpdate(List.of(5L))).thenReturn(List.of(habitacion));
            when(reservaRepository.countReservasActivasParaHabitacion(anyLong(), any(), any(), anyLong()))
                    .thenReturn(0L);
            ReservaRequest req = request();
            req.setNumeroHuespedes(5); // capacidad de la habitación: 2
            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("capacidad");
        }
    }

    @Nested
    class MaquinaDeEstados {

        private Reserva reservaEn(EstadoReserva estado) {
            Reserva r = Reserva.builder()
                    .id(7L).codigoReserva("RES-2026-000007")
                    .estado(estado)
                    .precioTotal(new BigDecimal("300000"))
                    .habitaciones(new ArrayList<>(List.of(habitacion)))
                    .build();
            lenient().when(reservaRepository.findById(7L)).thenReturn(Optional.of(r));
            lenient().when(reservaRepository.findByIdConServicios(7L)).thenReturn(Optional.of(r));
            lenient().when(reservaRepository.findByIdConPagosYFactura(7L)).thenReturn(Optional.of(r));
            lenient().when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            return r;
        }

        @Test
        void pendienteAConfirmadaEsValida() {
            Reserva r = reservaEn(EstadoReserva.PENDIENTE);
            service.cambiarEstado(7L, EstadoReserva.CONFIRMADA);
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        }

        @Test
        void checkinOcupaLasHabitaciones() {
            Reserva r = reservaEn(EstadoReserva.CONFIRMADA);
            service.cambiarEstado(7L, EstadoReserva.CHECKIN);
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.CHECKIN);
            assertThat(habitacion.getEstado()).isEqualTo(EstadoHabitacion.OCUPADA);
        }

        @Test
        void checkoutLiberaLasHabitaciones() {
            habitacion.setEstado(EstadoHabitacion.OCUPADA);
            Reserva r = reservaEn(EstadoReserva.CHECKIN);
            service.cambiarEstado(7L, EstadoReserva.CHECKOUT);
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.CHECKOUT);
            assertThat(habitacion.getEstado()).isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        void pendienteACheckoutEsInvalida() {
            reservaEn(EstadoReserva.PENDIENTE);
            assertThatThrownBy(() -> service.cambiarEstado(7L, EstadoReserva.CHECKOUT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no permitida");
        }

        @Test
        void checkoutEsEstadoTerminal() {
            reservaEn(EstadoReserva.CHECKOUT);
            assertThatThrownBy(() -> service.cambiarEstado(7L, EstadoReserva.CANCELADA))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void noSePuedeCancelarConCheckout() {
            reservaEn(EstadoReserva.CHECKOUT);
            assertThatThrownBy(() -> service.cancelar(7L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("checkout");
        }

        @Test
        void cancelarDesdeCheckinLiberaHabitaciones() {
            habitacion.setEstado(EstadoHabitacion.OCUPADA);
            Reserva r = reservaEn(EstadoReserva.CHECKIN);
            service.cancelar(7L);
            assertThat(r.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
            assertThat(habitacion.getEstado()).isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        void noSePuedeEditarUnaReservaCancelada() {
            reservaEn(EstadoReserva.CANCELADA);
            assertThatThrownBy(() -> service.update(7L, request()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CANCELADA");
        }
    }
}
