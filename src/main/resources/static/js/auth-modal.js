/* Hotel Paraíso — modal de autenticación del portal público.
 *
 * Toda la autenticación vive aquí: login, registro, recuperación de
 * contraseña y elección de contraseña nueva. No hay páginas sueltas.
 *
 * Progresivo: sin este script el servidor pinta el modal ya abierto
 * (clases show/d-block + backdrop, ver fragments/auth.html) y los
 * formularios funcionan por POST clásico; los disparadores [data-hp-auth]
 * navegan a /login, /registro o /recuperar por su href, que redirigen al
 * portal con el panel correspondiente abierto.
 *
 * Envíos por fetch con X-Requested-With:
 *  - login → los handlers de seguridad responden JSON
 *            (200 {redirect} | 401 {mensaje});
 *  - el resto → el controller responde el fragment del panel
 *            re-renderizado con sus errores (422) o el de éxito (200),
 *            que se intercambia dentro del modal sin recargar.
 */
(function () {
    'use strict';

    const modalEl = document.getElementById('hpAuthModal');
    if (!modalEl || typeof bootstrap === 'undefined') return;

    const panes = {};
    modalEl.querySelectorAll('[data-hp-auth-pane]').forEach(pane => {
        panes[pane.dataset.hpAuthPane] = pane;
    });

    const modal = new bootstrap.Modal(modalEl);

    const paneActivo = () => Object.values(panes).find(pane => !pane.hidden) || panes.login;

    const tituloDe = (nombre) => 'hpAuthTitulo' + nombre.charAt(0).toUpperCase() + nombre.slice(1);

    const enfocar = () => {
        const pane = paneActivo();
        const campo = pane.querySelector('.is-invalid')
            || pane.querySelector('input:not([type="hidden"]), select');
        if (campo) campo.focus();
    };

    const mostrarPane = (nombre) => {
        if (!panes[nombre]) return;
        Object.entries(panes).forEach(([n, pane]) => { pane.hidden = n !== nombre; });
        modalEl.setAttribute('aria-labelledby', tituloDe(nombre));
    };

    const mostrarAlerta = (pane, mensaje) => {
        const alerta = pane.querySelector('[data-hp-auth-alert]');
        if (!alerta) return;
        alerta.textContent = mensaje;
        alerta.classList.remove('d-none');
    };

    const ocultarAlertas = (raiz) => {
        raiz.querySelectorAll('[data-hp-auth-alert]').forEach(a => a.classList.add('d-none'));
    };

    const cargando = (form, estado) => {
        const btn = form.querySelector('[data-hp-auth-submit]');
        if (!btn) return;
        btn.disabled = estado;
        btn.classList.toggle('hp-btn-cargando', estado);
    };

    // ─── Adopción del modal que el servidor pintó abierto ───────────
    // Sin esto convivirían dos estados: el estático del HTML y el que
    // gestiona Bootstrap (foco, ESC, scroll del body).
    if (modalEl.classList.contains('show')) {
        modalEl.classList.remove('show', 'd-block');
        document.querySelectorAll('[data-hp-auth-backdrop]').forEach(b => b.remove());
        modal.show();
    }

    // ─── Apertura y conmutación entre paneles ───────────────────────
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

    // ─── Envíos (delegado: los paneles se reemplazan enteros) ───────
    modalEl.addEventListener('submit', event => {
        const form = event.target.closest('form[data-hp-auth-form]');
        if (!form) return;
        event.preventDefault();

        const nombre = form.dataset.hpAuthForm;
        const pane = panes[nombre] || paneActivo();
        ocultarAlertas(pane);
        cargando(form, true);

        fetch(form.action, {
            method: 'POST',
            body: new FormData(form),
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(respuesta => nombre === 'login'
                ? procesarLogin(respuesta)
                : procesarFragmento(pane, respuesta))
            .catch(() => mostrarAlerta(pane,
                'No pudimos conectar con el servidor. Inténtalo de nuevo.'))
            .finally(() => cargando(form, false));
    });

    function procesarLogin(respuesta) {
        if (respuesta.ok) {
            return respuesta.json().then(datos => {
                if (datos.redirect) {
                    window.location.assign(datos.redirect);
                } else {
                    // Sin destino forzoso: el usuario se queda donde estaba
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

    /** 200 y 422 traen el panel re-renderizado; cualquier otro status, no. */
    function procesarFragmento(pane, respuesta) {
        if (respuesta.status !== 200 && respuesta.status !== 422) {
            mostrarAlerta(pane, 'La sesión expiró. Recarga la página e inténtalo de nuevo.');
            return;
        }
        return respuesta.text().then(html => {
            pane.innerHTML = html;
            enfocar();
        });
    }
})();
