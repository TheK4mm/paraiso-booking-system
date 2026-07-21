package com.hotel.paraiso.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * KPIs y series del dashboard. Las series van alineadas por índice
 * (labels[i] ↔ valores[i]) y se serializan inline en la vista.
 */
@Getter
@Builder
public class DashboardData {

    // KPIs del día
    private long habitacionesOcupadas;
    private long habitacionesActivas;
    private int porcentajeOcupacion;
    private long llegadasHoy;
    private long salidasHoy;
    private long reservasActivas;
    private BigDecimal ingresosMes;
    private long facturasPendientes;

    // Series de los últimos 12 meses
    private List<String> meses;
    private List<BigDecimal> ingresosPorMes;
    private List<Long> reservasPorMes;

    // Distribución por estado
    private List<String> estadosReserva;
    private List<Long> reservasPorEstado;
}
