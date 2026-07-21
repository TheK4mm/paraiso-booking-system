-- =====================================================================
-- Usuario administrador inicial.
-- Credenciales por defecto (cambiar en producción): admin / admin123
-- El hash se genera con pgcrypto (bcrypt $2a$, compatible con
-- BCryptPasswordEncoder de Spring Security).
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO usuarios (username, email, password_hash, nombre_completo, rol, creado_por)
VALUES ('admin', 'admin@hotelparaiso.com',
        crypt('admin123', gen_salt('bf', 10)),
        'Administrador del Sistema', 'ADMIN', 'sistema');
