-- Usuario recepcionista de demostración (solo perfil dev).
-- Credenciales: recepcion / recepcion123

INSERT INTO usuarios (username, email, password_hash, nombre_completo, rol, creado_por)
VALUES ('recepcion', 'recepcion@hotelparaiso.com',
        crypt('recepcion123', gen_salt('bf', 10)),
        'María González', 'RECEPCIONISTA', 'sistema');
