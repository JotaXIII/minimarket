package com.minimarket.controller;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.security.model.RegisterRequest;
import com.minimarket.security.model.UsuarioResponse;
import com.minimarket.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;

    public UsuarioController(UsuarioService usuarioService,
                             PasswordEncoder passwordEncoder,
                             RolRepository rolRepository) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('GERENTE')")
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioService.findAll()
                .stream()
                .map(UsuarioResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * GET /api/usuarios/{id} (solo GERENTE):
     * Retorna UsuarioResponse (sin password).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<UsuarioResponse> obtenerUsuarioPorId(@PathVariable Long id) {
        return usuarioService.findById(id)
                .map(u -> ResponseEntity.ok(UsuarioResponse.from(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<?> crearUsuario(@Valid @RequestBody RegisterRequest request) {

        if (usuarioService.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("mensaje", "El nombre de usuario ya existe"));
        }

        // El gerente puede asignar cualquier rol; por defecto ROLE_CLIENTE
        String nombreRol = (request.getRol() != null && !request.getRol().isBlank())
                ? request.getRol()
                : "ROLE_CLIENTE";

        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nombreRol));

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        // Hash BCrypt aplicado SIEMPRE antes de persistir
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<Rol> roles = new HashSet<>();
        roles.add(rol);
        usuario.setRoles(roles);

        Usuario guardado = usuarioService.save(usuario);

        // Retorna UsuarioResponse (sin password)
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(guardado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id,
                                               @RequestBody RegisterRequest request) {
        Optional<Usuario> optUsuario = usuarioService.findById(id);

        if (optUsuario.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = optUsuario.get();

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            usuario.setUsername(request.getUsername());
        }

        // Solo actualizar password si se envió uno nuevo
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRol() != null && !request.getRol().isBlank()) {
            Rol rol = rolRepository.findByNombre(request.getRol())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + request.getRol()));

            Set<Rol> roles = new HashSet<>();
            roles.add(rol);
            usuario.setRoles(roles);
        }

        Usuario actualizado = usuarioService.save(usuario);
        return ResponseEntity.ok(UsuarioResponse.from(actualizado));
    }

    /** DELETE /api/usuarios/{id} (solo GERENTE) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        Optional<Usuario> usuario = usuarioService.findById(id);

        if (usuario.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        usuarioService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}