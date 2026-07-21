/* Hotel Paraíso V2.0 — gráficos del dashboard (datos inline, sin fetch) */
(function () {
    'use strict';
    const d = window.hpDashboard;
    if (!d || typeof Chart === 'undefined') return;

    Chart.defaults.font.family = "'Inter', system-ui, sans-serif";
    Chart.defaults.color = '#6b7280';

    const etiquetasEstado = {
        PENDIENTE: 'Pendiente', CONFIRMADA: 'Confirmada', CHECKIN: 'Check-in',
        CHECKOUT: 'Check-out', CANCELADA: 'Cancelada', NO_SHOW: 'No-show'
    };
    const coloresEstado = {
        PENDIENTE: '#f59e0b', CONFIRMADA: '#3b82f6', CHECKIN: '#4338ca',
        CHECKOUT: '#10b981', CANCELADA: '#ef4444', NO_SHOW: '#9ca3af'
    };

    const cop = v => '$' + Number(v).toLocaleString('es-CO');

    new Chart(document.getElementById('chartIngresos'), {
        type: 'bar',
        data: {
            labels: d.meses,
            datasets: [{
                data: d.ingresos,
                backgroundColor: '#4338ca',
                borderRadius: 6,
                maxBarThickness: 34
            }]
        },
        options: {
            plugins: {
                legend: { display: false },
                tooltip: { callbacks: { label: ctx => cop(ctx.parsed.y) } }
            },
            scales: {
                y: { ticks: { callback: v => cop(v) }, grid: { color: '#f3f4f6' } },
                x: { grid: { display: false } }
            }
        }
    });

    new Chart(document.getElementById('chartEstados'), {
        type: 'doughnut',
        data: {
            labels: d.estados.map(e => etiquetasEstado[e] || e),
            datasets: [{
                data: d.porEstado,
                backgroundColor: d.estados.map(e => coloresEstado[e] || '#9ca3af'),
                borderWidth: 2,
                borderColor: '#ffffff'
            }]
        },
        options: {
            cutout: '62%',
            plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, usePointStyle: true } } }
        }
    });

    new Chart(document.getElementById('chartReservas'), {
        type: 'line',
        data: {
            labels: d.meses,
            datasets: [{
                data: d.reservas,
                borderColor: '#4338ca',
                backgroundColor: 'rgba(67, 56, 202, .08)',
                fill: true,
                tension: .35,
                pointRadius: 3,
                pointBackgroundColor: '#4338ca'
            }]
        },
        options: {
            plugins: { legend: { display: false } },
            scales: {
                y: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: '#f3f4f6' } },
                x: { grid: { display: false } }
            }
        }
    });
})();
