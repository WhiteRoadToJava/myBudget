
package com.mybudget.server.dto;


import jakarta.validation.constraints.NotBlank;

public class AuthRequest {
    @NotBlank
    private String email;
    @NotBlank
    private String password;

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public @NotBlank String getEmail() {
        return email;
    }

    public @NotBlank String getPassword() {
        return password;
    }
}
