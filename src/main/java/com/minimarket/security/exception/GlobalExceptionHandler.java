package com.minimarket.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> buildError(int status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        return body;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex) {
        // Mensaje genérico para no revelar si es el usuario o la contraseña
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildError(401, "No autorizado", "Credenciales incorrectas"));
    }

    /**
     * Alias explícito para BadCredentialsException (subclase de AuthenticationException).
     * BadCredentialsException es la excepción específica que lanza
     * DaoAuthenticationProvider cuando la contraseña no coincide con el hash.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildError(401, "No autorizado", "Credenciales incorrectas"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildError(403, "Acceso denegado",
                        "No tienes permisos para acceder a este recurso"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // Recopilar todos los errores de campo en un mapa {campo → mensaje}
        Map<String, String> erroresCampo = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            erroresCampo.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> body = buildError(400, "Datos de entrada inválidos",
                "Hay campos con errores de validación");
        body.put("errores", erroresCampo);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        // Solo logueamos el detalle; el cliente recibe un mensaje genérico
        System.err.println("[ERROR] Excepción no controlada: " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(500, "Error interno",
                        "Ocurrió un error inesperado. Por favor intente más tarde."));
    }
}
