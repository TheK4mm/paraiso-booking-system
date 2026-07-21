package com.hotel.paraiso.dashboard;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Métricas agregadas para el dashboard. Consultas nativas con agregación
 * en base de datos: ninguna serie carga entidades a memoria.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final int MESES_SERIE = 12;

    @PersistenceContext
    private EntityManager em;

    public DashboardData obtener() {
        LocalDate hoy = LocalDate.now();
        YearMonth mesActual = YearMonth.now();
        LocalDate inicioSerie = mesActual.minusMonths(MESES_SERIE - 1).atDay(1);

        long ocupadas = contar("SELECT COUNT(*) FROM habitaciones WHERE estado = 'OCUPADA' AND activo = TRUE");
        long activas = contar("SELECT COUNT(*) FROM habitaciones WHERE activo = TRUE");
        long llegadas = contar("""
                SELECT COUNT(*) FROM reservas
                WHERE fecha_entrada = :hoy AND estado IN ('PENDIENTE','CONFIRMADA')
                """, Map.of("hoy", hoy));
        long salidas = contar("""
                SELECT COUNT(*) FROM reservas WHERE fecha_salida = :hoy AND estado = 'CHECKIN'
                """, Map.of("hoy", hoy));
        long reservasActivas = contar(
                "SELECT COUNT(*) FROM reservas WHERE estado IN ('PENDIENTE','CONFIRMADA','CHECKIN')");
        BigDecimal ingresosMes = (BigDecimal) em.createNativeQuery("""
                        SELECT COALESCE(SUM(monto), 0) FROM pagos
                        WHERE estado_pago = 'APROBADO' AND fecha_pago >= :inicioMes
                        """)
                .setParameter("inicioMes", mesActual.atDay(1).atStartOfDay())
                .getSingleResult();
        long facturasPendientes = contar(
                "SELECT COUNT(*) FROM facturas WHERE estado_factura IN ('PENDIENTE','PAGADA_PARCIALMENTE')");

        // Series mensuales (mapa mes → valor, con relleno de ceros)
        Map<YearMonth, BigDecimal> ingresos = serieMensual(inicioSerie);
        Map<YearMonth, Long> reservasMes = reservasPorMes(inicioSerie);

        List<String> meses = new ArrayList<>();
        List<BigDecimal> serieIngresos = new ArrayList<>();
        List<Long> serieReservas = new ArrayList<>();
        for (int i = 0; i < MESES_SERIE; i++) {
            YearMonth ym = mesActual.minusMonths(MESES_SERIE - 1L - i);
            meses.add(ym.getMonth().getDisplayName(TextStyle.SHORT, ES) + " " + (ym.getYear() % 100));
            serieIngresos.add(ingresos.getOrDefault(ym, BigDecimal.ZERO));
            serieReservas.add(reservasMes.getOrDefault(ym, 0L));
        }

        // Distribución por estado
        List<String> estados = new ArrayList<>();
        List<Long> porEstado = new ArrayList<>();
        for (Object[] fila : listar("SELECT estado, COUNT(*) FROM reservas GROUP BY estado ORDER BY estado")) {
            estados.add((String) fila[0]);
            porEstado.add(((Number) fila[1]).longValue());
        }

        return DashboardData.builder()
                .habitacionesOcupadas(ocupadas)
                .habitacionesActivas(activas)
                .porcentajeOcupacion(activas == 0 ? 0 : (int) Math.round(ocupadas * 100.0 / activas))
                .llegadasHoy(llegadas)
                .salidasHoy(salidas)
                .reservasActivas(reservasActivas)
                .ingresosMes(ingresosMes)
                .facturasPendientes(facturasPendientes)
                .meses(meses)
                .ingresosPorMes(serieIngresos)
                .reservasPorMes(serieReservas)
                .estadosReserva(estados)
                .reservasPorEstado(porEstado)
                .build();
    }

    private Map<YearMonth, BigDecimal> serieMensual(LocalDate desde) {
        Map<YearMonth, BigDecimal> resultado = new LinkedHashMap<>();
        for (Object[] fila : listar("""
                SELECT date_trunc('month', fecha_pago) AS mes, SUM(monto)
                FROM pagos
                WHERE estado_pago = 'APROBADO' AND fecha_pago >= :desde
                GROUP BY mes ORDER BY mes
                """, Map.of("desde", desde.atStartOfDay()))) {
            resultado.put(aYearMonth(fila[0]), (BigDecimal) fila[1]);
        }
        return resultado;
    }

    private Map<YearMonth, Long> reservasPorMes(LocalDate desde) {
        Map<YearMonth, Long> resultado = new LinkedHashMap<>();
        for (Object[] fila : listar("""
                SELECT date_trunc('month', fecha_entrada) AS mes, COUNT(*)
                FROM reservas
                WHERE fecha_entrada >= :desde AND estado <> 'CANCELADA'
                GROUP BY mes ORDER BY mes
                """, Map.of("desde", desde))) {
            resultado.put(aYearMonth(fila[0]), ((Number) fila[1]).longValue());
        }
        return resultado;
    }

    /** date_trunc llega como tipo temporal distinto según el mapeo del driver. */
    private YearMonth aYearMonth(Object valor) {
        if (valor instanceof java.sql.Timestamp ts) {
            return YearMonth.from(ts.toLocalDateTime());
        }
        if (valor instanceof java.time.Instant instant) {
            return YearMonth.from(instant.atZone(java.time.ZoneId.systemDefault()));
        }
        if (valor instanceof java.time.LocalDateTime ldt) {
            return YearMonth.from(ldt);
        }
        if (valor instanceof java.time.OffsetDateTime odt) {
            return YearMonth.from(odt.toLocalDate());
        }
        throw new IllegalStateException("Tipo temporal no soportado: " + valor.getClass());
    }

    private long contar(String sql) {
        return contar(sql, Map.of());
    }

    private long contar(String sql, Map<String, Object> params) {
        var query = em.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return ((Number) query.getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> listar(String sql) {
        return em.createNativeQuery(sql).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> listar(String sql, Map<String, Object> params) {
        var query = em.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query.getResultList();
    }
}
