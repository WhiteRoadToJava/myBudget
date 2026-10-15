package com.mybudget.server.dto;

import jakarta.validation.constraints.NotBlank;



import java.util.Set;

import com.mybudget.server.modules.enums.Role;
import jakarta.validation.constraints.Pattern;
import lombok.NoArgsConstructor;



public class RegisterRequest {
    @NotBlank
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Invalid email format"
    )
    private String username;
    @NotBlank
    private String password;
    private Set<Role> roles;
    private String firstName;
    private String lastName;
    private String phone;
    private String expireDate;
    private String language;

    public RegisterRequest(){

    }

    public RegisterRequest(String username, String password, Set<Role> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    public RegisterRequest(String username, String password, Set<Role> roles, String firstName, String lastName, String phone, String expireDate) {
        this.username = username;
        this.password = password;
        this.roles = roles;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.expireDate = expireDate;
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

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}