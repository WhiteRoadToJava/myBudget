package com.mybudget.server.dto.admin;

import lombok.Data;

// email, name and the language of the invitation email
@Data
public class InviteAdminRequest {
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String language;
}
