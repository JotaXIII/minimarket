package com.minimarket.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String tokenType = "Bearer";

    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }
}
