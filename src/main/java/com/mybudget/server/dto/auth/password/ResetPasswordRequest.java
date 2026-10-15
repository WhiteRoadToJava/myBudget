package com.mybudget.server.dto.auth.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String token;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()\\-_=+{};:,<.>])(?=.{8,}).*$",
            message = "Password must be at least 8 characters long and contain at least "
                    + "one uppercase letter, one number, and one special character"
    )
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
