package com.minimarket.security.model;

import com.minimarket.entity.Usuario;
import lombok.Data;

import java.util.Set;
import java.util.stream.Collectors;

@Data
public class UsuarioResponse {

    private Long id;
    private String username;
    private Set<String> roles;

    public static UsuarioResponse from(Usuario usuario) {
        UsuarioResponse dto = new UsuarioResponse();
        dto.setId(usuario.getId());
        dto.setUsername(usuario.getUsername());
        // Solo se envían los nombres de los roles, no los objetos Rol completos
        dto.setRoles(
            usuario.getRoles().stream()
                .map(rol -> rol.getNombre())
                .collect(Collectors.toSet())
        );
        return dto;
    }
}
