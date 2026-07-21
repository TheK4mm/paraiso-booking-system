package com.hotel.paraiso.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * Configuración de seguridad en tres zonas:
 *  - Pública: landing, flujo de reservas de huéspedes, consulta por
 *    código y los endpoints de autenticación. Sin sesión requerida.
 *    La autenticación NO tiene páginas propias: ocurre en el modal del
 *    portal, y /login, /registro y /recuperar solo redirigen allí
 *    (ver {@link RutasAuth} y {@link PortalAuthController}).
 *  - Cliente: /mi-cuenta/** exclusivo del rol CLIENTE.
 *  - Back-office: todo lo demás exige personal (ADMIN/RECEPCIONISTA) —
 *    default cerrado: una ruta interna nueva queda protegida sin tocar
 *    esta clase, y una pública nueva exige un permitAll consciente.
 *  - HTTP Basic para la API REST (/api/**), que responde 401 en JSON
 *    en lugar de redirigir al login; CSRF exento por ser API programática.
 *    La API es de back-office: CLIENTE recibe 403.
 *  - Method security (@PreAuthorize) como defensa en profundidad.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.security.remember-me-key}")
    private String rememberMeKey;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    RolAuthenticationSuccessHandler successHandler,
                                    RolAuthenticationFailureHandler failureHandler,
                                    PortalAuthenticationEntryPoint portalEntryPoint,
                                    PortalAccessDeniedHandler accessDeniedHandler,
                                    RequestCache requestCache) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .requestCache(cache -> cache.requestCache(requestCache))
                .authorizeHttpRequests(auth -> auth
                        // ── zona pública: landing, reservas de huéspedes, auth y estáticos ──
                        // favicon y .well-known incluidos a propósito: sin ellos el
                        // navegador genera peticiones denegadas que ensucian el
                        // RequestCache y acaban siendo el destino tras el login
                        .requestMatchers("/", "/reservar/**", "/consulta-reserva",
                                "/login", "/registro", "/recuperar", "/restablecer",
                                "/verificar-email", "/verificar-email/**",
                                "/css/**", "/js/**", "/img/**", "/fonts/**", "/vendor/**",
                                "/favicon.ico", "/.well-known/**", "/error").permitAll()
                        // ── zona cliente ──
                        .requestMatchers("/mi-cuenta/**").hasRole("CLIENTE")
                        // ── administración exclusiva ──
                        .requestMatchers("/usuarios/**", "/actividad/**", "/empleados/**").hasRole("ADMIN")
                        .requestMatchers("/api/empleados/**").hasRole("ADMIN")
                        // catálogos: escritura solo ADMIN (lectura para el personal)
                        .requestMatchers(HttpMethod.POST,
                                "/habitaciones/**", "/tipos-habitacion/**", "/servicios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/habitaciones/**", "/api/tipos-habitacion/**", "/api/servicios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/habitaciones/**", "/api/tipos-habitacion/**", "/api/servicios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/habitaciones/**", "/api/tipos-habitacion/**", "/api/servicios/**").hasRole("ADMIN")
                        // ── todo lo demás es back-office: solo personal ──
                        .anyRequest().hasAnyRole("ADMIN", "RECEPCIONISTA"))
                .formLogin(form -> form
                        // GET /login redirige al portal; el POST lo procesa el filtro
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        // Los tres roles vuelven al portal, nunca a un login aparte
                        .logoutSuccessUrl(RutasAuth.PORTAL + "?logout"))
                .rememberMe(remember -> remember.key(rememberMeKey))
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                        // La API responde 401 en JSON; el resto va al modal del portal.
                        // Ambos como mappings (y en este orden): fijar
                        // authenticationEntryPoint() descartaría estos mappings y
                        // /api/** acabaría redirigiendo en vez de responder 401.
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/**"))
                        .defaultAuthenticationEntryPointFor(
                                portalEntryPoint, AnyRequestMatcher.INSTANCE)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }
}
