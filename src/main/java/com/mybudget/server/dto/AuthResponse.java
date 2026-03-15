package com.mybudget.server.dto;

import com.mybudget.server.module.Role;


import java.util.Set;

public class AuthResponse {
    private String jwtToken;
    private String email;
    private Set<Role> roles;

    public AuthResponse(String jwtToken, String email, Set<Role> roles) {
        this.jwtToken = jwtToken;
        this.email = email;
        this.roles = roles;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}