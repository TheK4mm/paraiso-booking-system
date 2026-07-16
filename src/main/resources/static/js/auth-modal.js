/* Hotel Paraíso — modal de autenticación del portal público.
 *
 * Progresivo: sin este script (o para usuarios ya autenticados, sin modal
 * en la página) los disparadores [data-hp-auth] navegan a las páginas
 * clásicas /login y /registro por su href.
 *
 * Envíos por fetch con X-Requested-With:
 *  - login  → los handlers de seguridad responden JSON
 *             (200 {redirect} | 401 {mensaje});
 *  - registro → el controller responde el fragment del pane re-renderizado
 *             con los errores (422) o el mensaje de éxito (200), que se
 *             intercambia dentro del modal sin recargar la página.
 */
(function () {
    'use strict';

    const modalEl = document.getElementById('hpAuthModal');
    if (!modalEl || typeof bootstrap === 'undefined') return;

    const modal = new bootstrap.Modal(modalEl);
    const panes = {
        login: modalEl.querySelector('[data-hp-auth-pane="login"]'),
        registro: modalEl.querySelector('[data-hp-auth-pane="registro"]')
    };
    const titulos = { login: 'hpAuthTituloLogin', registro: 'hpAuthTituloRegistro' };

    const paneActivo = () => (panes.login.hidden ? panes.registro : panes.login);

    const enfocar = () => {
        const campo = paneActivo().querySelector('.is-invalid')
            || paneActivo().querySelector('input:not([type="hidden"]), select');
        if (campo) campo.focus();
    };

    const mostrarPane = (nombre) => {
        Object.entries(panes).forEach(([n, pane]) => { pane.hidden = n !== nombre; });
        modalEl.setAttribute('aria-labelledby', titulos[nombre]);
    };

    const mostrarAlerta = (pane, mensaje) => {
        const alerta = pane.querySelector('[data-hp-auth-alert]');
        if (!alerta) return;
        alerta.textContent = mensaje;
        alerta.classList.remove('d-none');
    };

    const ocultarAlertas = (pane) => {
        pane.querySelectorAll('[data-hp-auth-alert]').forEach(a => a.classList.add('d-none'));
    };

    const cargando = (form, estado) => {
        const btn = form.querySelector('[data-hp-auth-submit]');
        if (!btn) return;
        btn.disabled = estado;
        btn.classList.toggle('hp-btn-cargando', estado);
    };

    // ─── Apertura y conmutación login ⇄ registro ────────────────────
    document.addEventListener('click', event => {
        const disparador = event.target.closest('[data-hp-auth]');
        if (disparador) {
            event.preventDefault();
            mostrarPane(disparador.dataset.hpAuth);
            modal.show();
            return;
        }
        const cambio = event.target.closest('[data-hp-auth-switch]');
        if (cambio && modalEl.contains(cambio)) {
            event.preventDefault();
            mostrarPane(cambio.dataset.hpAuthSwitch);
            enfocar();
        }
    });

    // Al reabrir, el error del intento anterior ya no aplica
    modalEl.addEventListener('show.bs.modal', () => ocultarAlertas(modalEl));
    modalEl.addEventListener('shown.bs.modal', enfocar);

    // ─── Envíos (delegado: el pane de registro se reemplaza entero) ──
    modalEl.addEventListener('submit', event => {
        const form = event.target.closest('form[data-hp-auth-form]');
        if (!form) return;
        event.preventDefault();
        ocultarAlertas(paneActivo());
        cargando(form, true);

        fetch(form.action, {
            method: 'POST',
            body: new FormData(form),
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(respuesta => form.dataset.hpAuthForm === 'login'
                ? procesarLogin(respuesta)
                : procesarRegistro(respuesta))
            .catch(() => mostrarAlerta(paneActivo(),
                'No pudimos conectar con el servidor. Inténtalo de nuevo.'))
            .finally(() => cargando(form, false));
    });

    function procesarLogin(respuesta) {
        if (respuesta.ok) {
            return respuesta.json().then(datos => {
                if (datos.redirect) {
                    window.location.assign(datos.redirect);
                } else {
                    window.location.reload();
                }
            });
        }
        if (respuesta.status === 401) {
            return respuesta.json()
                .then(datos => datos.mensaje || 'Usuario o contraseña incorrectos.')
                .catch(() => 'Usuario o contraseña incorrectos.')
                .then(mensaje => {
                    mostrarAlerta(panes.login, mensaje);
                    const password = panes.login.querySelector('#hpAuthPassword');
                    if (password) password.value = '';
                    enfocar();
                });
        }
        // 403 típico: sesión (y token CSRF) caducados con la página abierta
        mostrarAlerta(panes.login, 'La sesión expiró. Recarga la página e inténtalo de nuevo.');
    }

    function procesarRegistro(respuesta) {
        if (respuesta.status !== 200 && respuesta.status !== 422) {
            mostrarAlerta(panes.registro, 'La sesión expiró. Recarga la página e inténtalo de nuevo.');
            return;
        }
        return respuesta.text().then(html => {
            panes.registro.innerHTML = html;
            enfocar();
        });
    }
})();
