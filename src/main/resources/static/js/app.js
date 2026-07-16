/* Hotel Paraíso V2.0 — comportamiento global de la interfaz */
(function () {
    'use strict';

    // ─── Sidebar responsive ─────────────────────────────────────────
    const sidebar = document.getElementById('hpSidebar');
    const backdrop = document.getElementById('hpBackdrop');
    const toggle = document.getElementById('hpSidebarToggle');

    if (toggle && sidebar && backdrop) {
        const cerrar = () => {
            sidebar.classList.remove('show');
            backdrop.classList.remove('show');
        };
        toggle.addEventListener('click', () => {
            sidebar.classList.toggle('show');
            backdrop.classList.toggle('show');
        });
        backdrop.addEventListener('click', cerrar);
    }

    // ─── Toasts (mensajes flash) ────────────────────────────────────
    document.querySelectorAll('.toast[data-hp-autoshow]').forEach(el => {
        new bootstrap.Toast(el, { delay: 4500 }).show();
    });

    // ─── Modal de confirmación genérico ─────────────────────────────
    // Cualquier <form data-hp-confirm="mensaje"> pasa por el modal en
    // lugar del confirm() nativo del navegador.
    const modalEl = document.getElementById('hpConfirmModal');
    if (modalEl) {
        const modal = new bootstrap.Modal(modalEl);
        const msgEl = modalEl.querySelector('[data-hp-confirm-message]');
        const okBtn = modalEl.querySelector('[data-hp-confirm-ok]');
        let formPendiente = null;

        document.querySelectorAll('form[data-hp-confirm]').forEach(form => {
            form.addEventListener('submit', event => {
                if (form.dataset.hpConfirmado === 'si') return;
                event.preventDefault();
                formPendiente = form;
                msgEl.textContent = form.dataset.hpConfirm;
                modal.show();
            });
        });

        okBtn.addEventListener('click', () => {
            if (formPendiente) {
                formPendiente.dataset.hpConfirmado = 'si';
                modal.hide();
                formPendiente.requestSubmit();
                formPendiente = null;
            }
        });
    }
})();
