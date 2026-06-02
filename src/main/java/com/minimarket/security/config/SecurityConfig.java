package com.minimarket.security.config;

import com.minimarket.security.filter.JwtAuthFilter;
import com.minimarket.security.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthFilter jwtAuthFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Deshabilitar CSRF (no aplica en APIs REST stateless)
            .csrf(csrf -> csrf.disable())

            // 2. Configurar CORS con la política definida en corsConfigurationSource()
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .headers(headers -> headers
                .contentTypeOptions(ct -> {}) // X-Content-Type-Options: nosniff
                .frameOptions(frame -> frame.deny()) // X-Frame-Options: DENY
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'self'; frame-ancestors 'none'")
                )
            )

            // 3. Reglas de autorización por URL
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos: login y registro no requieren token JWT
                .requestMatchers("/auth/**").permitAll()
                // Consola H2 (solo entorno de desarrollo)
                .requestMatchers("/h2-console/**").permitAll()
                // Endpoint de prueba público
                .requestMatchers("/public/**").permitAll()
                // Cualquier otro endpoint requiere autenticación válida (JWT)
                .anyRequest().authenticated()
            )

            // 4. Sesión completamente stateless: no se crean ni usan HttpSessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 5. Proveedor de autenticación personalizado (BCrypt + UserDetailsService)
            .authenticationProvider(authenticationProvider())

            // 6. Filtro JWT insertado antes del filtro estándar de credenciales
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Orígenes permitidos: solo el frontend de la aplicación
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080"));
        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Cabeceras permitidas en los requests
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // No se usan cookies/credenciales cross-origin (usamos JWT en header)
        config.setAllowCredentials(false);
        // Tiempo de caché del preflight OPTIONS (segundos)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expone el AuthenticationManager como Bean para inyectarlo en AuthController.
     * Lo usa para disparar el proceso de autenticación en el endpoint /auth/login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
