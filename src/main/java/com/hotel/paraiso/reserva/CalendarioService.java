package com.hotel.paraiso.reserva;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Construye la grilla mensual del calendario de reservas en el servidor:
 * semanas de lunes a domingo con chips por noche ocupada.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarioService {

    private final ReservaRepository reservaRepository;

    public record ChipReserva(Long id, String codigo, String cliente, String estado,
                              boolean llegada, boolean ultimaNoche) {
    }

    public record DiaCalendario(LocalDate fecha, boolean enMes, boolean hoy, List<ChipReserva> reservas) {
    }

    public List<List<DiaCalendario>> mes(YearMonth ym) {
        LocalDate inicio = ym.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fin = ym.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<Reserva> reservas = reservaRepository.findParaCalendario(inicio, fin);

        List<List<DiaCalendario>> semanas = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        LocalDate dia = inicio;
        while (!dia.isAfter(fin)) {
            List<DiaCalendario> semana = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                LocalDate actual = dia;
                List<ChipReserva> chips = reservas.stream()
                        .filter(r -> !actual.isBefore(r.getFechaEntrada()) && actual.isBefore(r.getFechaSalida()))
                        .map(r -> new ChipReserva(
                                r.getId(),
                                r.getCodigoReserva(),
                                r.getCliente().getNombre() + " " + r.getCliente().getApellido(),
                                r.getEstado().name(),
                                actual.equals(r.getFechaEntrada()),
                                actual.equals(r.getFechaSalida().minusDays(1))))
                        .toList();
                semana.add(new DiaCalendario(actual, YearMonth.from(actual).equals(ym),
                        actual.equals(hoy), chips));
                dia = dia.plusDays(1);
            }
            semanas.add(semana);
        }
        return semanas;
    }
}
