package com.hotel.paraiso.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;
import java.util.Set;

/**
 * Qué petición merece recordarse para volver a ella tras el login.
 *
 * <p>Por defecto {@link HttpSessionRequestCache} guarda CUALQUIER petición
 * denegada, incluidas las que el navegador emite por su cuenta
 * (favicon, sondas de DevTools, imágenes) y las llamadas AJAX. Ese ruido
 * terminaba siendo el destino post-login y provocaba un 403 inmediato.
 * Aquí se restringe a navegaciones de verdad: GET de documentos HTML.
 */
@Configuration
public class PortalCacheConfig {

    /** Rutas que nunca son un destino de navegación válido. */
    private static final String[] NO_NAVEGABLES = {
            "/favicon.ico", "/error", "/.well-known/**",
            "/css/**", "/js/**", "/img/**", "/fonts/**", "/vendor/**"
    };

    @Bean
    RequestCache requestCache() {
        MediaTypeRequestMatcher aceptaHtml =
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        // Sin esto, un Accept: */* (el de fetch por defecto) contaría como HTML
        aceptaHtml.setIgnoredMediaTypes(Set.of(MediaType.ALL));

        RequestMatcher navegacion = new AndRequestMatcher(
                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/**"),
                aceptaHtml,
                new NegatedRequestMatcher(
                        new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest")),
                new NegatedRequestMatcher(new OrRequestMatcher(
                        Arrays.stream(NO_NAVEGABLES)
                                .map(ruta -> (RequestMatcher) PathPatternRequestMatcher
                                        .withDefaults().matcher(ruta))
                                .toList())));

        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        cache.setRequestMatcher(navegacion);
        return cache;
    }
}
