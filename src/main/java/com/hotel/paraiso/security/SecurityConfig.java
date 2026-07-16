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
 * Configuración de seguridad:
 *  - Form login con sesiones para la interfaz web (CSRF activo).
 *  - HTTP Basic para la API REST (/api/**), que responde 401 en JSON
 *    en lugar de redirigir al login; CSRF exento por ser API programática.
 *  - Autorización por rutas + method security (@PreAuthorize) como
 *    defensa en profundidad en los servicios.
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
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        // público: autenticación y estáticos
                        .requestMatchers("/login", "/registro", "/recuperar", "/restablecer",
                                "/css/**", "/js/**", "/img/**", "/fonts/**", "/vendor/**", "/error").permitAll()
                        // administración exclusiva
                        .requestMatchers("/usuarios/**", "/actividad/**", "/empleados/**").hasRole("ADMIN")
                        .requestMatchers("/api/empleados/**").hasRole("ADMIN")
                        // catálogos: escritura solo ADMIN (lectura para todos los autenticados)
                        .requestMatchers(HttpMethod.POST,
                                "/habitaciones/**", "/tipos-habitacion/**", "/servicios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/habitaciones/**", "/api/tipos-habitacion/**", "/api/servicios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/habitaciones/**", "/api/tipos-habitacion/**", "/api/servicios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/habitaciones/**", "/api/tipos-habitacion/**", "/api/servicios/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
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
