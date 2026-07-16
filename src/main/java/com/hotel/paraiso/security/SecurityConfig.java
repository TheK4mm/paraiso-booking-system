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
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Configuración de seguridad en tres zonas:
 *  - Pública: landing, flujo de reservas de huéspedes, consulta por
 *    código y autenticación. Sin sesión requerida.
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
                                    RolAuthenticationSuccessHandler successHandler) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        // ── zona pública: landing, reservas de huéspedes, auth y estáticos ──
                        .requestMatchers("/", "/reservar/**", "/consulta-reserva",
                                "/login", "/registro", "/recuperar", "/restablecer",
                                "/verificar-email/**",
                                "/css/**", "/js/**", "/img/**", "/fonts/**", "/vendor/**", "/error").permitAll()
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
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout"))
                .rememberMe(remember -> remember.key(rememberMeKey))
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")));
        return http.build();
    }
}
