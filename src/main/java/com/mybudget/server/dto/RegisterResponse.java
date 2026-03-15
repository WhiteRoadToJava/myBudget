package com.mybudget.server.dto;

import com.mybudget.server.module.Role;


import java.util.Set;

public class RegisterResponse {
    private String message;
    private String username;
    private String email;
    private Set<Role> roles;

    public RegisterResponse(String message, String username, String email, Set<Role> roles) {
        this.message = message;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }





    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}