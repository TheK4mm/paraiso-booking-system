package com.hotel.paraiso.common.email;

import lombok.extern.slf4j.Slf4j;

/**
 * Respaldo para entornos sin SMTP (dev): el correo se emite por el log,
 * de modo que los enlaces de verificación y restablecimiento siguen
 * siendo utilizables sin credenciales de correo.
 */
@Slf4j
public class LogEmailSender implements EmailSender {

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        log.warn("""
                [SIN SMTP] Correo no enviado — configura MAIL_HOST para el envío real.
                  Para:    {}
                  Asunto:  {}
                {}""", destinatario, asunto, cuerpo);
    }
}
