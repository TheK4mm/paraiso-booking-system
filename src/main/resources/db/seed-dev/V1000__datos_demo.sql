-- =====================================================================
-- Hotel Paraíso V2.0 — Datos de demostración (solo perfil dev)
-- Reservas distribuidas en varios meses y estados para alimentar
-- dashboard, calendario y listados. Totales coherentes con la regla:
--   precio_total = Σ(precio_base_noche × noches) + Σ(servicios)
--   factura.total = (subtotal − descuento) × (1 + IVA%)
-- =====================================================================

-- ─── Catálogos ────────────────────────────────────────────────────────
INSERT INTO tipos_habitacion (id, nombre, descripcion, capacidad_maxima, precio_base_noche, creado_por) VALUES
  (1, 'Individual Estándar', 'Habitación individual con cama sencilla, baño privado y vista al jardín', 1, 150000.00, 'sistema'),
  (2, 'Doble Estándar',      'Habitación doble con cama queen, baño privado y Smart TV 42"',            2, 220000.00, 'sistema'),
  (3, 'Junior Suite',        'Suite junior con sala de estar, jacuzzi y vista a la piscina',             3, 380000.00, 'sistema'),
  (4, 'Suite Presidencial',  'Suite de lujo con dos habitaciones, sala, comedor y terraza panorámica',   4, 750000.00, 'sistema'),
  (5, 'Familiar',            'Habitación familiar con dos camas dobles y sofá cama para niños',          5, 320000.00, 'sistema');

INSERT INTO habitaciones (id, numero, piso, descripcion, estado, tipo_habitacion_id, creado_por) VALUES
  (1,  '101', 1, 'Vista al jardín oriental',            'DISPONIBLE',    1, 'sistema'),
  (2,  '102', 1, 'Vista a la piscina',                  'DISPONIBLE',    1, 'sistema'),
  (3,  '103', 1, 'Cama queen, acceso directo al patio', 'OCUPADA',       2, 'sistema'),
  (4,  '201', 2, 'Cama queen con escritorio ejecutivo', 'DISPONIBLE',    2, 'sistema'),
  (5,  '202', 2, 'Cama king, vista a la montaña',       'DISPONIBLE',    2, 'sistema'),
  (6,  '203', 2, 'Habitación familiar, área de juegos', 'DISPONIBLE',    5, 'sistema'),
  (7,  '301', 3, 'Jacuzzi y balcón privado',            'DISPONIBLE',    3, 'sistema'),
  (8,  '302', 3, 'Jacuzzi, vista panorámica',           'DISPONIBLE',    3, 'sistema'),
  (9,  '401', 4, 'Suite completa, terraza panorámica',  'OCUPADA',       4, 'sistema'),
  (10, '402', 4, 'Cama king, sala de estar',            'DISPONIBLE',    2, 'sistema'),
  (11, '501', 5, 'Suite junior en remodelación',        'MANTENIMIENTO', 3, 'sistema'),
  (12, '502', 5, 'Individual con vista a la ciudad',    'DISPONIBLE',    1, 'sistema');

INSERT INTO servicios (id, nombre, descripcion, precio, categoria, creado_por) VALUES
  (1, 'Desayuno Buffet',     'Desayuno buffet completo con productos frescos de la región', 35000.00,  'ALIMENTACION', 'sistema'),
  (2, 'Cena Romántica',      'Cena de 3 tiempos en el restaurante con decoración especial', 120000.00, 'ALIMENTACION', 'sistema'),
  (3, 'Servicio de Spa',     'Masaje relajante de 60 minutos con aromaterapia',             85000.00,  'SPA_BIENESTAR', 'sistema'),
  (4, 'Transfer Aeropuerto', 'Transporte privado aeropuerto-hotel-aeropuerto',              90000.00,  'TRANSPORTE', 'sistema'),
  (5, 'Lavandería Express',  'Servicio de lavandería con entrega en 4 horas',               45000.00,  'LAVANDERIA', 'sistema'),
  (6, 'Tour Ciudad',         'Tour guiado por los principales atractivos de la ciudad',     65000.00,  'ENTRETENIMIENTO', 'sistema'),
  (7, 'Sala de Juntas',      'Alquiler de sala de juntas por 4 horas con equipos AV',       180000.00, 'NEGOCIOS', 'sistema');

INSERT INTO empleados (id, nombre, apellido, numero_documento, cargo, email_corporativo, fecha_contratacion, creado_por) VALUES
  (1, 'María',  'González', '52445123', 'Recepcionista',    'mgonzalez@hotelparaiso.com', '2022-03-15', 'sistema'),
  (2, 'Carlos', 'Ramírez',  '80123456', 'Gerente Reservas', 'cramirez@hotelparaiso.com',  '2020-01-10', 'sistema'),
  (3, 'Luisa',  'Martínez', '43789012', 'Recepcionista',    'lmartinez@hotelparaiso.com', '2023-06-01', 'sistema');

INSERT INTO clientes (id, nombre, apellido, tipo_documento, numero_documento, email, telefono, direccion, pais, creado_en, creado_por) VALUES
  (1,  'Andrés',   'Restrepo',  'CC',        '1020304050', 'andres.restrepo@gmail.com',   '3001234567', 'Cra 15 #93-45, Bogotá',        'Colombia',       '2026-01-05 10:00', 'sistema'),
  (2,  'Valentina','Ospina',    'CC',        '1030405060', 'valentina.ospina@hotmail.com','3109876543', 'Cl 10 #43-21, Medellín',       'Colombia',       '2026-01-12 11:30', 'sistema'),
  (3,  'John',     'Smith',     'PASAPORTE', 'US4821736',  'john.smith@outlook.com',      '3201112233', NULL,                           'Estados Unidos', '2026-01-28 09:15', 'sistema'),
  (4,  'Camila',   'Torres',    'CC',        '1040506070', 'camila.torres@gmail.com',     '3015556677', 'Av 6N #23-50, Cali',           'Colombia',       '2026-02-03 16:40', 'sistema'),
  (5,  'Pierre',   'Dubois',    'PASAPORTE', 'FR9273645',  'pierre.dubois@gmail.com',     '3168889900', NULL,                           'Francia',        '2026-02-10 08:20', 'sistema'),
  (6,  'Mariana',  'Quintero',  'CC',        '1050607080', 'mariana.quintero@yahoo.com',  '3122223344', 'Cl 84 #51-30, Barranquilla',   'Colombia',       '2026-03-01 14:00', 'sistema'),
  (7,  'Diego',    'Fernández', 'CE',        'E00112233',  'diego.fernandez@gmail.com',   '3054445566', 'Cra 7 #71-21, Bogotá',         'Argentina',      '2026-04-08 12:10', 'sistema'),
  (8,  'Isabella', 'Rossi',     'PASAPORTE', 'IT5566778',  'isabella.rossi@gmail.com',    '3187778899', NULL,                           'Italia',         '2026-05-15 17:25', 'sistema'),
  (9,  'Santiago', 'Mejía',     'CC',        '1060708090', 'santiago.mejia@gmail.com',    '3140001122', 'Cl 33 #74-15, Medellín',       'Colombia',       '2026-06-02 10:45', 'sistema'),
  (10, 'Laura',    'Cardona',   'CC',        '1070809010', 'laura.cardona@gmail.com',     '3176543210', 'Cra 43A #1-50, Medellín',      'Colombia',       '2026-06-20 15:30', 'sistema');

-- ─── Reservas ─────────────────────────────────────────────────────────
-- (id, codigo, entrada, salida, huéspedes, noches, total, estado, creado_en, cliente, empleado)
INSERT INTO reservas (id, codigo_reserva, fecha_entrada, fecha_salida, numero_huespedes, total_noches, precio_total, observaciones, estado, creado_en, creado_por, cliente_id, empleado_id) VALUES
  (1,  'RES-2026-000001', '2026-01-10', '2026-01-14', 2, 4, 915000.00,  NULL,                            'CHECKOUT',   '2026-01-05 10:20', 'sistema', 1, 1),
  (2,  'RES-2026-000002', '2026-01-20', '2026-01-22', 1, 2, 300000.00,  NULL,                            'CHECKOUT',   '2026-01-12 11:45', 'sistema', 2, 1),
  (3,  'RES-2026-000003', '2026-02-05', '2026-02-08', 2, 3, 1225000.00, 'Aniversario de bodas',          'CHECKOUT',   '2026-01-28 09:30', 'sistema', 3, 2),
  (4,  'RES-2026-000004', '2026-02-14', '2026-02-16', 2, 2, 1620000.00, 'Cena de San Valentín incluida', 'CHECKOUT',   '2026-02-03 17:00', 'sistema', 4, 1),
  (5,  'RES-2026-000005', '2026-03-01', '2026-03-05', 2, 4, 880000.00,  NULL,                            'CHECKOUT',   '2026-02-10 08:40', 'sistema', 5, 3),
  (6,  'RES-2026-000006', '2026-03-10', '2026-03-12', 4, 2, 705000.00,  'Cancelada por el cliente',      'CANCELADA',  '2026-03-01 14:20', 'sistema', 6, 1),
  (7,  'RES-2026-000007', '2026-04-02', '2026-04-06', 2, 4, 880000.00,  NULL,                            'CHECKOUT',   '2026-03-20 09:00', 'sistema', 1, 3),
  (8,  'RES-2026-000008', '2026-04-15', '2026-04-18', 3, 3, 1140000.00, 'No se presentó',                'NO_SHOW',    '2026-04-08 12:30', 'sistema', 7, 2),
  (9,  'RES-2026-000009', '2026-05-01', '2026-05-04', 2, 3, 990000.00,  'Dos habitaciones individuales', 'CHECKOUT',   '2026-04-20 10:00', 'sistema', 2, 1),
  (10, 'RES-2026-000010', '2026-05-20', '2026-05-23', 1, 3, 450000.00,  NULL,                            'CHECKOUT',   '2026-05-15 17:40', 'sistema', 8, 3),
  (11, 'RES-2026-000011', '2026-06-10', '2026-06-14', 2, 4, 915000.00,  NULL,                            'CHECKOUT',   '2026-06-02 11:00', 'sistema', 9, 1),
  (12, 'RES-2026-000012', '2026-06-25', '2026-06-28', 2, 3, 1140000.00, NULL,                            'CHECKOUT',   '2026-06-18 09:20', 'sistema', 4, 2),
  (13, 'RES-2026-000013', '2026-07-06', '2026-07-10', 4, 4, 3205000.00, 'Huésped VIP',                   'CHECKIN',    '2026-06-25 15:00', 'sistema', 3, 2),
  (14, 'RES-2026-000014', '2026-07-07', '2026-07-09', 2, 2, 440000.00,  NULL,                            'CHECKIN',    '2026-07-01 10:30', 'sistema', 6, 1),
  (15, 'RES-2026-000015', '2026-07-08', '2026-07-12', 5, 4, 1315000.00, 'Familia con niños',             'CONFIRMADA', '2026-06-28 16:10', 'sistema', 10, 3),
  (16, 'RES-2026-000016', '2026-07-15', '2026-07-18', 2, 3, 1140000.00, NULL,                            'CONFIRMADA', '2026-07-02 12:00', 'sistema', 5, 1),
  (17, 'RES-2026-000017', '2026-07-20', '2026-07-24', 2, 4, 880000.00,  NULL,                            'PENDIENTE',  '2026-07-05 09:45', 'sistema', 7, NULL),
  (18, 'RES-2026-000018', '2026-08-05', '2026-08-09', 3, 4, 1570000.00, 'Solicita habitaciones contiguas','PENDIENTE', '2026-07-06 14:30', 'sistema', 8, NULL),
  (19, 'RES-2026-000019', '2026-07-02', '2026-07-06', 1, 4, 600000.00,  NULL,                            'CHECKOUT',   '2026-06-27 08:50', 'sistema', 9, 3);

INSERT INTO reserva_habitacion (reserva_id, habitacion_id) VALUES
  (1, 4), (2, 1), (3, 7), (4, 9), (5, 3), (6, 6), (7, 5), (8, 8),
  (9, 1), (9, 2), (10, 12), (11, 4), (12, 7), (13, 9), (14, 3),
  (15, 6), (16, 8), (17, 5), (18, 1), (18, 3), (19, 12);

INSERT INTO reserva_servicio (reserva_id, servicio_id) VALUES
  (1, 1),          -- Desayuno
  (3, 3),          -- Spa
  (4, 2),          -- Cena
  (6, 6),          -- Tour (cancelada)
  (9, 4),          -- Transfer
  (11, 1),         -- Desayuno
  (13, 2), (13, 3),-- Cena + Spa
  (15, 1),         -- Desayuno
  (18, 4);         -- Transfer

-- ─── Facturas ─────────────────────────────────────────────────────────
-- total = subtotal × 1.19 (descuento 0 en datos demo)
INSERT INTO facturas (id, numero_factura, subtotal, impuesto_porcentaje, impuesto_valor, descuento, total, estado_factura, fecha_emision, creado_en, creado_por, reserva_id) VALUES
  (1,  'FAC-2026-000001', 915000.00,  19.00, 173850.00, 0.00, 1088850.00, 'PAGADA',              '2026-01-14 12:00', '2026-01-14 12:00', 'sistema', 1),
  (2,  'FAC-2026-000002', 300000.00,  19.00, 57000.00,  0.00, 357000.00,  'PAGADA',              '2026-01-22 11:30', '2026-01-22 11:30', 'sistema', 2),
  (3,  'FAC-2026-000003', 1225000.00, 19.00, 232750.00, 0.00, 1457750.00, 'PAGADA',              '2026-02-08 12:15', '2026-02-08 12:15', 'sistema', 3),
  (4,  'FAC-2026-000004', 1620000.00, 19.00, 307800.00, 0.00, 1927800.00, 'PAGADA',              '2026-02-16 10:45', '2026-02-16 10:45', 'sistema', 4),
  (5,  'FAC-2026-000005', 880000.00,  19.00, 167200.00, 0.00, 1047200.00, 'PAGADA',              '2026-03-05 12:30', '2026-03-05 12:30', 'sistema', 5),
  (6,  'FAC-2026-000006', 880000.00,  19.00, 167200.00, 0.00, 1047200.00, 'PAGADA',              '2026-04-06 11:00', '2026-04-06 11:00', 'sistema', 7),
  (7,  'FAC-2026-000007', 990000.00,  19.00, 188100.00, 0.00, 1178100.00, 'PAGADA',              '2026-05-04 12:00', '2026-05-04 12:00', 'sistema', 9),
  (8,  'FAC-2026-000008', 450000.00,  19.00, 85500.00,  0.00, 535500.00,  'PAGADA',              '2026-05-23 11:15', '2026-05-23 11:15', 'sistema', 10),
  (9,  'FAC-2026-000009', 915000.00,  19.00, 173850.00, 0.00, 1088850.00, 'PAGADA',              '2026-06-14 12:20', '2026-06-14 12:20', 'sistema', 11),
  (10, 'FAC-2026-000010', 1140000.00, 19.00, 216600.00, 0.00, 1356600.00, 'PAGADA',              '2026-06-28 10:30', '2026-06-28 10:30', 'sistema', 12),
  (11, 'FAC-2026-000011', 3205000.00, 19.00, 608950.00, 0.00, 3813950.00, 'PAGADA_PARCIALMENTE', '2026-07-06 16:00', '2026-07-06 16:00', 'sistema', 13),
  (12, 'FAC-2026-000012', 440000.00,  19.00, 83600.00,  0.00, 523600.00,  'PENDIENTE',           '2026-07-07 09:30', '2026-07-07 09:30', 'sistema', 14),
  (13, 'FAC-2026-000013', 600000.00,  19.00, 114000.00, 0.00, 714000.00,  'PAGADA',              '2026-07-06 10:00', '2026-07-06 10:00', 'sistema', 19);

-- ─── Pagos ────────────────────────────────────────────────────────────
INSERT INTO pagos (id, monto, metodo_pago, referencia_transaccion, estado_pago, descripcion, fecha_pago, creado_en, creado_por, reserva_id, factura_id) VALUES
  (1,  1088850.00, 'TARJETA_CREDITO', 'TXN-8842-0114', 'APROBADO',  'Pago total al check-out',       '2026-01-14 12:05', '2026-01-14 12:05', 'sistema', 1,  1),
  (2,  357000.00,  'EFECTIVO',        NULL,            'APROBADO',  'Pago en recepción',             '2026-01-22 11:35', '2026-01-22 11:35', 'sistema', 2,  2),
  (3,  1457750.00, 'PSE',             'PSE-2291-0208', 'APROBADO',  'Pago electrónico',              '2026-02-08 12:20', '2026-02-08 12:20', 'sistema', 3,  3),
  (4,  1000000.00, 'TARJETA_CREDITO', 'TXN-9034-0214', 'APROBADO',  'Abono inicial',                 '2026-02-14 15:00', '2026-02-14 15:00', 'sistema', 4,  4),
  (5,  927800.00,  'TRANSFERENCIA',   'TRF-5521-0216', 'APROBADO',  'Saldo al check-out',            '2026-02-16 10:50', '2026-02-16 10:50', 'sistema', 4,  4),
  (6,  1047200.00, 'TARJETA_DEBITO',  'TXN-7716-0305', 'APROBADO',  'Pago total',                    '2026-03-05 12:35', '2026-03-05 12:35', 'sistema', 5,  5),
  (7,  1047200.00, 'NEQUI',           'NQ-3310-0406',  'APROBADO',  'Pago total',                    '2026-04-06 11:05', '2026-04-06 11:05', 'sistema', 7,  6),
  (8,  1178100.00, 'TARJETA_CREDITO', 'TXN-6127-0504', 'APROBADO',  'Pago total',                    '2026-05-04 12:05', '2026-05-04 12:05', 'sistema', 9,  7),
  (9,  535500.00,  'EFECTIVO',        NULL,            'APROBADO',  'Pago en recepción',             '2026-05-23 11:20', '2026-05-23 11:20', 'sistema', 10, 8),
  (10, 1088850.00, 'PSE',             'PSE-4482-0614', 'APROBADO',  'Pago electrónico',              '2026-06-14 12:25', '2026-06-14 12:25', 'sistema', 11, 9),
  (11, 1356600.00, 'TARJETA_CREDITO', 'TXN-2295-0628', 'APROBADO',  'Pago total',                    '2026-06-28 10:35', '2026-06-28 10:35', 'sistema', 12, 10),
  (12, 2000000.00, 'TRANSFERENCIA',   'TRF-9910-0706', 'APROBADO',  'Abono huésped VIP',             '2026-07-06 16:10', '2026-07-06 16:10', 'sistema', 13, 11),
  (13, 500000.00,  'NEQUI',           'NQ-1204-0628',  'APROBADO',  'Anticipo de reserva',           '2026-06-28 16:20', '2026-06-28 16:20', 'sistema', 15, NULL),
  (14, 570000.00,  'TARJETA_CREDITO', 'TXN-1180-0702', 'RECHAZADO', 'Fondos insuficientes',          '2026-07-02 12:10', '2026-07-02 12:10', 'sistema', 16, NULL),
  (15, 570000.00,  'TARJETA_CREDITO', 'TXN-1181-0702', 'APROBADO',  'Anticipo 50%',                  '2026-07-02 12:15', '2026-07-02 12:15', 'sistema', 16, NULL),
  (16, 714000.00,  'DAVIPLATA',       'DV-7833-0706',  'APROBADO',  'Pago total al check-out',       '2026-07-06 10:05', '2026-07-06 10:05', 'sistema', 19, 13);

-- ─── Sincronizar secuencias tras inserts con IDs explícitos ──────────
SELECT setval('tipos_habitacion_id_seq', (SELECT MAX(id) FROM tipos_habitacion));
SELECT setval('habitaciones_id_seq',     (SELECT MAX(id) FROM habitaciones));
SELECT setval('servicios_id_seq',        (SELECT MAX(id) FROM servicios));
SELECT setval('empleados_id_seq',        (SELECT MAX(id) FROM empleados));
SELECT setval('clientes_id_seq',         (SELECT MAX(id) FROM clientes));
SELECT setval('reservas_id_seq',         (SELECT MAX(id) FROM reservas));
SELECT setval('facturas_id_seq',         (SELECT MAX(id) FROM facturas));
SELECT setval('pagos_id_seq',            (SELECT MAX(id) FROM pagos));
SELECT setval('seq_codigo_reserva', 19);
SELECT setval('seq_numero_factura', 13);
