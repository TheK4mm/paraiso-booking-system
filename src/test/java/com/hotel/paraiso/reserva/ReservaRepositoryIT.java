package com.hotel.paraiso.reserva;

import com.hotel.paraiso.cliente.Cliente;
import com.hotel.paraiso.habitacion.Habitacion;
import com.hotel.paraiso.habitacion.TipoHabitacion;
import com.hotel.paraiso.reserva.Reserva.EstadoReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de las consultas críticas contra PostgreSQL real (Testcontainers):
 * solapamiento de fechas, exclusión de la propia reserva y secuencias.
 * Se omiten automáticamente si Docker no está disponible.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class ReservaRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private TestEntityManager em;

    private Habitacion habitacion;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        TipoHabitacion tipo = em.persist(TipoHabitacion.builder()
                .nombre("Doble Test " + System.nanoTime()).capacidadMaxima(2)
                .precioBaseNoche(new BigDecimal("200000")).activo(true).build());
        habitacion = em.persist(Habitacion.builder()
                .numero("T" + (System.nanoTime() % 100000)).piso(1)
                .estado(Habitacion.EstadoHabitacion.DISPONIBLE).activo(true)
                .tipoHabitacion(tipo).build());
        cliente = em.persist(Cliente.builder()
                .nombre("Test").apellido("Cliente").tipoDocumento("CC")
                .numeroDocumento(String.valueOf(System.nanoTime()))
                .email("test" + System.nanoTime() + "@test.com").activo(true).build());
    }

    private Reserva reserva(LocalDate entrada, LocalDate salida, EstadoReserva estado) {
        return em.persist(Reserva.builder()
                .codigoReserva("RES-TEST-" + System.nanoTime())
                .fechaEntrada(entrada).fechaSalida(salida)
                .numeroHuespedes(2)
                .totalNoches((int) (salida.toEpochDay() - entrada.toEpochDay()))
                .precioTotal(new BigDecimal("400000"))
                .estado(estado)
                .cliente(cliente)
                .habitaciones(List.of(habitacion))
                .build());
    }

    @Test
    void detectaSolapamientoDeFechas() {
        reserva(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 14), EstadoReserva.CONFIRMADA);

        // solapa parcialmente
        long conflictos = reservaRepository.countReservasActivasParaHabitacion(
                habitacion.getId(), LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 16), -1L);
        assertThat(conflictos).isEqualTo(1);

        // contiguo (la salida coincide con la entrada existente): no solapa
        long sinConflicto = reservaRepository.countReservasActivasParaHabitacion(
                habitacion.getId(), LocalDate.of(2027, 3, 6), LocalDate.of(2027, 3, 10), -1L);
        assertThat(sinConflicto).isZero();
    }

    @Test
    void lasReservasFinalizadasNoBloqueanDisponibilidad() {
        reserva(LocalDate.of(2027, 4, 1), LocalDate.of(2027, 4, 5), EstadoReserva.CHECKOUT);
        reserva(LocalDate.of(2027, 4, 1), LocalDate.of(2027, 4, 5), EstadoReserva.CANCELADA);

        long conflictos = reservaRepository.countReservasActivasParaHabitacion(
                habitacion.getId(), LocalDate.of(2027, 4, 2), LocalDate.of(2027, 4, 4), -1L);
        assertThat(conflictos).isZero();
    }

    @Test
    void excluyeLaPropiaReservaAlEditar() {
        Reserva existente = reserva(LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 14),
                EstadoReserva.CONFIRMADA);

        long sinExcluir = reservaRepository.countReservasActivasParaHabitacion(
                habitacion.getId(), LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 14), -1L);
        long excluyendo = reservaRepository.countReservasActivasParaHabitacion(
                habitacion.getId(), LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 14),
                existente.getId());

        assertThat(sinExcluir).isEqualTo(1);
        assertThat(excluyendo).isZero();
    }

    @Test
    void laSecuenciaDeCodigosEsMonotona() {
        long primero = reservaRepository.nextCodigoSeq();
        long segundo = reservaRepository.nextCodigoSeq();
        assertThat(segundo).isGreaterThan(primero);
    }
}
