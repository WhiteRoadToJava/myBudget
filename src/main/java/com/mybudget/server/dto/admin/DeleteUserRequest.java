package com.mybudget.server.dto.admin;

import lombok.Data;

// the admin types the user's email again, so a wrong click deletes nothing
@Data
public class DeleteUserRequest {
    private String confirmEmail;
}
