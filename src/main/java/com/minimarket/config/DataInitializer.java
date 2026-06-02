package com.minimarket.config;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner init(RolRepository rolRepository,
                                  UsuarioRepository usuarioRepository,
                                  PasswordEncoder passwordEncoder) {
        return args -> {

            // 1. Crear los tres roles del sistema RBAC
            crearRolSiNoExiste("ROLE_CLIENTE", rolRepository);
            crearRolSiNoExiste("ROLE_EMPLEADO", rolRepository);
            crearRolSiNoExiste("ROLE_GERENTE", rolRepository);

            // 2. Usuario GERENTE de prueba
            if (usuarioRepository.findByUsername("gerente").isEmpty()) {
                Rol rolGerente = rolRepository.findByNombre("ROLE_GERENTE").orElseThrow();
                Usuario gerente = new Usuario();
                gerente.setUsername("gerente");
                gerente.setPassword(passwordEncoder.encode("G3r3nt3!MMP2026"));
                gerente.setRoles(Set.of(rolGerente));
                usuarioRepository.save(gerente);
            }

            // 3. Usuario EMPLEADO de prueba
            if (usuarioRepository.findByUsername("empleado1").isEmpty()) {
                Rol rolEmpleado = rolRepository.findByNombre("ROLE_EMPLEADO").orElseThrow();
                Usuario empleado = new Usuario();
                empleado.setUsername("empleado1");
                empleado.setPassword(passwordEncoder.encode("3mpl34d0!MMP2026"));
                empleado.setRoles(Set.of(rolEmpleado));
                usuarioRepository.save(empleado);
            }

            // 4. Usuario CLIENTE de prueba
            if (usuarioRepository.findByUsername("cliente1").isEmpty()) {
                Rol rolCliente = rolRepository.findByNombre("ROLE_CLIENTE").orElseThrow();
                Usuario cliente = new Usuario();
                cliente.setUsername("cliente1");
                cliente.setPassword(passwordEncoder.encode("Cl13nt3!MMP2026"));
                cliente.setRoles(Set.of(rolCliente));
                usuarioRepository.save(cliente);
            }

            System.out.println("==============================================");
            System.out.println("MiniMarket Plus - Datos inicializados");
            System.out.println("   POST /auth/login    → Obtener JWT");
            System.out.println("   POST /auth/register → Registrar cliente");
            System.out.println("   Usuarios de prueba:");
            System.out.println("   - gerente  / G3r3nt3!MMP2026  (ROLE_GERENTE)");
            System.out.println("   - empleado1 / 3mpl34d0!MMP2026 (ROLE_EMPLEADO)");
            System.out.println("   - cliente1  / Cl13nt3!MMP2026  (ROLE_CLIENTE)");
            System.out.println("==============================================");
        };
    }

    private void crearRolSiNoExiste(String nombre, RolRepository rolRepository) {
        if (rolRepository.findByNombre(nombre).isEmpty()) {
            Rol rol = new Rol();
            rol.setNombre(nombre);
            rolRepository.save(rol);
        }
    }
}
