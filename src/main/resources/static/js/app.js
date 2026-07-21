/* Hotel Paraíso V2.0 — comportamiento global de la interfaz */
(function () {
    'use strict';

    // ─── Menú lateral ───────────────────────────────────────────────
    // Un único botón (☰) con dos comportamientos según el ancho:
    //  · escritorio: pliega/despliega el menú (el contenido pasa a ancho
    //    completo). El estado se guarda en localStorage y lo restaura un
    //    script en línea del <head>, así sobrevive a la navegación.
    //  · móvil: panel deslizante con fondo oscurecido, como hasta ahora.
    const sidebar = document.getElementById('hpSidebar');
    const backdrop = document.getElementById('hpBackdrop');
    const toggle = document.getElementById('hpSidebarToggle');

    if (toggle && sidebar && backdrop) {
        const CLAVE_MENU = 'hp-sidebar';
        const raiz = document.documentElement;
        const escritorio = window.matchMedia('(min-width: 992px)');

        const cerrarPanelMovil = () => {
            sidebar.classList.remove('show');
            backdrop.classList.remove('show');
        };

        const sincronizarBoton = () => {
            const visible = escritorio.matches
                ? !raiz.classList.contains('hp-sidebar-oculto')
                : sidebar.classList.contains('show');
            const etiqueta = visible ? 'Contraer menú' : 'Desplegar menú';
            toggle.setAttribute('aria-expanded', String(visible));
            toggle.setAttribute('aria-label', etiqueta);
            toggle.setAttribute('title', etiqueta);
        };

        toggle.addEventListener('click', () => {
            if (escritorio.matches) {
                const oculto = raiz.classList.toggle('hp-sidebar-oculto');
                try {
                    localStorage.setItem(CLAVE_MENU, oculto ? 'oculto' : 'visible');
                } catch (e) { /* almacenamiento no disponible: solo esta página */ }
            } else {
                sidebar.classList.toggle('show');
                backdrop.classList.toggle('show');
            }
            sincronizarBoton();
        });

        backdrop.addEventListener('click', () => {
            cerrarPanelMovil();
            sincronizarBoton();
        });

        // Al cruzar el punto de ruptura el panel móvil no debe quedar abierto
        escritorio.addEventListener('change', () => {
            cerrarPanelMovil();
            sincronizarBoton();
        });

        sincronizarBoton();
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
