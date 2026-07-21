package com.hotel.paraiso.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HotelUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // El personal entra con su username; los clientes con su email
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .or(() -> usuarioRepository.findByEmailIgnoreCase(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .roles(usuario.getRol().name())
                // Solo la sanción administrativa deshabilita la cuenta: la
                // verificación del email es opcional y no bloquea el acceso
                .disabled(!usuario.getActivo())
                .build();
    }
}
