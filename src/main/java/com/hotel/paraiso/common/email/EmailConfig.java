package com.hotel.paraiso.common.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Elige la implementación de {@link EmailSender} en tiempo de arranque.
 *
 * <p>La decisión se toma sobre el {@link JavaMailSender} que Boot registra
 * únicamente si existe {@code spring.mail.host}, y se resuelve con un
 * {@link ObjectProvider} a propósito: una condición {@code @ConditionalOnBean}
 * en configuración de usuario se evalúa ANTES de las autoconfiguraciones y
 * nunca vería ese bean.
 */
@Configuration
@Slf4j
public class EmailConfig {

    @Bean
    EmailSender emailSender(ObjectProvider<JavaMailSender> javaMailSender,
                            @Value("${app.email.remitente}") String remitente) {
        JavaMailSender smtp = javaMailSender.getIfAvailable();
        if (smtp == null) {
            log.warn("Sin SMTP configurado (spring.mail.host ausente): los correos se emitirán por el log");
            return new LogEmailSender();
        }
        log.info("SMTP configurado: los correos se enviarán desde {}", remitente);
        return new SmtpEmailSender(smtp, remitente);
    }
}
