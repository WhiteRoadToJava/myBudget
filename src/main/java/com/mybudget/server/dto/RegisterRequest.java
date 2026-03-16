package com.mybudget.server.dto;

import jakarta.validation.constraints.NotBlank;



import java.util.Set;

import com.mybudget.server.modules.Role;

public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private Set<Role> roles;

    public RegisterRequest(String username, String password, Set<Role> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    public @NotBlank String getUsername() {
        return username;
    }

    public @NotBlank String getPassword() {
        return password;
    }

    public Set<Role> getRoles() {
        return roles;
    }
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}