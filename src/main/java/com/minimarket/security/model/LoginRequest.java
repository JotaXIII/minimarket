package com.minimarket.security.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    /**
     * Nombre de usuario. No puede estar en blanco ni ser menor a 3 caracteres.
     * El mensaje personalizado reemplaza el mensaje genérico de Jakarta.
     */
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    private String username;

    /**
     * Contraseña en texto plano enviada por el cliente.
     * Se valida que tenga al menos 8 caracteres antes de procesarla.
     * NUNCA se almacena ni se loguea; se compara con el hash BCrypt en BD.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
}
