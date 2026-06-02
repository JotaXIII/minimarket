package com.minimarket.controller;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.security.model.AuthResponse;
import com.minimarket.security.model.LoginRequest;
import com.minimarket.security.model.RegisterRequest;
import com.minimarket.security.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

       @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        // Verificar unicidad del username
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(buildMensaje("El nombre de usuario ya está registrado"));
        }

        // Obtener el rol ROLE_CLIENTE desde la BD
        Rol rolCliente = rolRepository.findByNombre("ROLE_CLIENTE")
                .orElseThrow(() -> new RuntimeException(
                        "Rol ROLE_CLIENTE no encontrado. ¿Se ejecutó DataInitializer?"));

        // Crear usuario con contraseña hasheada y rol CLIENTE fijo
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setUsername(request.getUsername());

        nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));

        // Rol asignado por el servidor, nunca por el cliente
        Set<Rol> roles = new HashSet<>();
        roles.add(rolCliente);
        nuevoUsuario.setRoles(roles);

        usuarioRepository.save(nuevoUsuario);

        // Generar JWT para acceso inmediato post-registro
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new AuthResponse(token, userDetails.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        /*
         * authenticate() lanza BadCredentialsException si las credenciales son
         * incorrectas. GlobalExceptionHandler la convierte en 401 con mensaje
         * genérico (no revela si falló username o password).
         */
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token, userDetails.getUsername()));
    }

    /** Método auxiliar para construir respuestas de error simples. */
    private java.util.Map<String, String> buildMensaje(String mensaje) {
        return java.util.Map.of("mensaje", mensaje);
    }
}
