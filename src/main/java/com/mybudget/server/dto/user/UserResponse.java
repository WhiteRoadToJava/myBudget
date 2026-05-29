package com.mybudget.server.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private  String username;
    private  String fullname;
    private String firstname;
    private String lastname;
    private  String phone;
}
