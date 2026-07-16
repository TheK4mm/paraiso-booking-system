-- =====================================================================
-- Datos demo del portal público (solo perfil dev)
--  - Imágenes y comodidades de los tipos de habitación
--  - Cuenta de cliente demo: cliente@hotelparaiso.com / cliente123
--    (vinculada a la ficha de Valentina Ospina, cliente id=2)
-- =====================================================================

UPDATE tipos_habitacion SET
    imagen      = '/img/tipos/individual-estandar.jpg',
    comodidades = 'Wifi,TV,Baño privado,Escritorio,Caja fuerte'
WHERE id = 1;

UPDATE tipos_habitacion SET
    imagen      = '/img/tipos/doble-estandar.jpg',
    comodidades = 'Wifi,TV,Minibar,Aire acondicionado,Baño privado'
WHERE id = 2;

UPDATE tipos_habitacion SET
    imagen      = '/img/tipos/junior-suite.jpg',
    comodidades = 'Wifi,Smart TV,Jacuzzi,Sala de estar,Minibar,Vista a la piscina'
WHERE id = 3;

UPDATE tipos_habitacion SET
    imagen      = '/img/tipos/suite-presidencial.jpg',
    comodidades = 'Wifi,Smart TV 55",Jacuzzi,Terraza privada,Minibar,Room service 24h,Cava de vinos'
WHERE id = 4;

UPDATE tipos_habitacion SET
    imagen      = '/img/tipos/familiar.jpg',
    comodidades = 'Wifi,2 TV,Cocineta,Sofá cama,Aire acondicionado,Juegos de mesa'
WHERE id = 5;

-- Cuenta demo de huésped (hash bcrypt vía pgcrypto, ya instalada en V3).
-- El email de la ficha se alinea con el de la cuenta: el registro público
-- garantiza ese invariante y el seed lo respeta.
-- ON CONFLICT DO NOTHING la hace tolerante a BDs dev donde la ficha ya
-- quedó vinculada a otra cuenta durante pruebas manuales.
UPDATE clientes SET email = 'cliente@hotelparaiso.com' WHERE id = 2;

INSERT INTO usuarios (username, email, password_hash, nombre_completo, rol,
                      activo, email_verificado, cliente_id, creado_por)
VALUES ('cliente@hotelparaiso.com',
        'cliente@hotelparaiso.com',
        crypt('cliente123', gen_salt('bf', 10)),
        'Valentina Ospina',
        'CLIENTE',
        TRUE,
        TRUE,
        2,
        'sistema')
ON CONFLICT DO NOTHING;
