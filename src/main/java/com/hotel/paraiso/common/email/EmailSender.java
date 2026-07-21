package com.hotel.paraiso.common.email;

/**
 * Envío de correo transaccional (verificación de email, restablecimiento
 * de contraseña). La implementación efectiva la elige {@link EmailConfig}
 * según haya o no SMTP configurado.
 */
public interface EmailSender {

    /**
     * Envía un correo de texto plano. Nunca propaga fallos de transporte:
     * un SMTP caído no debe tumbar el registro ni la solicitud de
     * recuperación, que son operaciones transaccionales de negocio.
     */
    void enviar(String destinatario, String asunto, String cuerpo);
}
