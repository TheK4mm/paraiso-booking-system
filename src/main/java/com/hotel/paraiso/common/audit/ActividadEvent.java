package com.hotel.paraiso.common.audit;

/**
 * Evento de negocio publicado por los servicios en operaciones relevantes
 * (crear reserva, transiciones de estado, pagos, facturas, usuarios).
 * Se persiste en activity_log solo si la transacción confirma.
 */
public record ActividadEvent(String accion, String tipoEntidad, Long entidadId, String detalle) {
}
