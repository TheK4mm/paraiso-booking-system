package com.hotel.paraiso.security;

import com.hotel.paraiso.common.audit.ActividadEvent;
import com.hotel.paraiso.common.exception.BadRequestException;
import com.hotel.paraiso.common.exception.BusinessException;
import com.hotel.paraiso.common.exception.ResourceNotFoundException;
import com.hotel.paraiso.common.web.SortWhitelist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    // El registro público vive en VerificacionEmailService (crea cuentas
    // CLIENTE tras verificar el email); aquí solo queda la administración.

    // ─── Administración (solo ADMIN, protegido por ruta /usuarios/**) ──

    public Page<UsuarioResponse> buscar(String q, Pageable pageable) {
        Specification<Usuario> spec = !StringUtils.hasText(q) ? null
                : (root, query, cb) -> {
                    String patron = "%" + q.trim().toLowerCase() + "%";
                    return cb.or(
                            cb.like(cb.lower(root.get("username")), patron),
                            cb.like(cb.lower(root.get("email")), patron),
                            cb.like(cb.lower(root.get("nombreCompleto")), patron));
                };
        Pageable saneado = SortWhitelist.sanitize(pageable,
                Set.of("id", "username", "email", "rol", "creadoEn"), Sort.by("id"));
        return usuarioRepository.findAll(spec, saneado).map(UsuarioResponse::from);
    }

    @Transactional
    public Usuario crearPorAdmin(UsuarioAdminRequest request) {
        if (request.getRol() == Rol.CLIENTE) {
            throw new BadRequestException(
                    "Las cuentas de cliente se crean únicamente desde el registro público");
        }
        if (usuarioRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new BadRequestException("El nombre de usuario ya está en uso: " + request.getUsername());
        }
        if (usuarioRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("El email ya está registrado: " + request.getEmail());
        }
        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .nombreCompleto(request.getNombreCompleto())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(request.getRol())
                .build();
        Usuario guardado = usuarioRepository.save(usuario);
        eventPublisher.publishEvent(new ActividadEvent(
                "USUARIO_CREADO", "Usuario", guardado.getId(),
                guardado.getUsername() + " (" + guardado.getRol() + ")"));
        return guardado;
    }

    @Transactional
    public boolean toggleActivo(Long id, String usuarioActual) {
        Usuario usuario = getOrThrow(id);
        if (usuario.getUsername().equalsIgnoreCase(usuarioActual)) {
            throw new BusinessException("No puedes desactivar tu propia cuenta");
        }
        usuario.setActivo(!usuario.getActivo());
        usuarioRepository.save(usuario);
        eventPublisher.publishEvent(new ActividadEvent(
                usuario.getActivo() ? "USUARIO_ACTIVADO" : "USUARIO_DESACTIVADO",
                "Usuario", id, usuario.getUsername()));
        return usuario.getActivo();
    }

    @Transactional
    public void cambiarRol(Long id, Rol nuevoRol, String usuarioActual) {
        Usuario usuario = getOrThrow(id);
        if (usuario.getUsername().equalsIgnoreCase(usuarioActual)) {
            throw new BusinessException("No puedes cambiar el rol de tu propia cuenta");
        }
        if (nuevoRol == Rol.CLIENTE || usuario.getRol() == Rol.CLIENTE) {
            // El vínculo con la ficha de cliente solo nace del registro público;
            // convertir cuentas hacia/desde CLIENTE lo rompería
            throw new BusinessException("El rol CLIENTE no puede asignarse ni retirarse desde la administración");
        }
        usuario.setRol(nuevoRol);
        usuarioRepository.save(usuario);
        eventPublisher.publishEvent(new ActividadEvent(
                "USUARIO_ROL_CAMBIADO", "Usuario", id, usuario.getUsername() + " → " + nuevoRol));
    }

    private Usuario getOrThrow(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    @Transactional
    public void cambiarPassword(Long usuarioId, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", usuarioId));
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        eventPublisher.publishEvent(new ActividadEvent(
                "PASSWORD_RESTABLECIDA", "Usuario", usuario.getId(), usuario.getUsername()));
    }
}
