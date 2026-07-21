-- ─────────────────────────────────────────────────────────────────────
-- La verificación de email pasa a ser OPCIONAL.
--
-- Antes, una cuenta CLIENTE nacía sin verificar y HotelUserDetailsService
-- la trataba como deshabilitada: sin SMTP configurado el enlace nunca
-- llegaba y la cuenta quedaba inservible para siempre. Ahora la cuenta
-- nace activa y la verificación solo desbloquea la vinculación de una
-- ficha de cliente preexistente.
--
-- Consecuencia de esquema: la ficha ya no se difiere hasta la
-- verificación, así que el token deja de transportar su payload.
-- ─────────────────────────────────────────────────────────────────────

-- 1) Rescata las cuentas que quedaron bloqueadas sin verificar creando la
--    ficha que su token todavía guarda (última solicitud por email, y solo
--    si ni el email ni el documento chocan con una ficha ya existente).
INSERT INTO clientes (nombre, apellido, tipo_documento, numero_documento,
                      telefono, email, activo, creado_por)
SELECT DISTINCT ON (LOWER(u.email))
       t.nombre, t.apellido, t.tipo_documento, t.numero_documento,
       t.telefono, u.email, TRUE, 'migracion-v5'
  FROM tokens_verificacion_email t
  JOIN usuarios u ON u.id = t.usuario_id
 WHERE u.cliente_id IS NULL
   AND NOT EXISTS (SELECT 1 FROM clientes c WHERE LOWER(c.email) = LOWER(u.email))
   AND NOT EXISTS (SELECT 1 FROM clientes c WHERE c.numero_documento = t.numero_documento)
 ORDER BY LOWER(u.email), t.creado_en DESC;

-- 2) Vincula cada cuenta huérfana con la ficha de su email, siempre que
--    esa ficha no pertenezca ya a otra cuenta (uq_usuarios_cliente).
UPDATE usuarios u
   SET cliente_id = c.id
  FROM clientes c
 WHERE u.cliente_id IS NULL
   AND LOWER(c.email) = LOWER(u.email)
   AND NOT EXISTS (SELECT 1 FROM usuarios o WHERE o.cliente_id = c.id);

-- 3) El payload de la ficha pendiente ya no tiene sentido: la ficha se
--    crea en el registro o se vincula por email al verificar.
ALTER TABLE tokens_verificacion_email
    DROP COLUMN nombre,
    DROP COLUMN apellido,
    DROP COLUMN tipo_documento,
    DROP COLUMN numero_documento,
    DROP COLUMN telefono;
