package com.mybudget.server.dto.auth;

import lombok.Data;

@Data
public class PasswordRequest {
    private String currentPassword;
    private String newPassword;
    private  String confirmPassword;
}
