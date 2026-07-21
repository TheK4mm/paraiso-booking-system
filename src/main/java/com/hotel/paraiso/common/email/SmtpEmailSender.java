package com.hotel.paraiso.common.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Envío real por SMTP. Activo solo cuando {@code spring.mail.host} está
 * definido (ver {@link EmailConfig}).
 */
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final String remitente;

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        try {
            javaMailSender.send(mensaje);
            log.info("Correo enviado a {}: {}", destinatario, asunto);
        } catch (MailException e) {
            // El correo es un efecto secundario: que falle no debe deshacer
            // el registro ni revelar al usuario si la cuenta existe
            log.error("No se pudo enviar el correo a {}: {}", destinatario, e.getMessage());
        }
    }
}
