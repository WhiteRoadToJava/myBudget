package com.mybudget.server.controllers;

import com.mybudget.server.dto.user.UserResponse;
import com.mybudget.server.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserControll {
private final UserService userService;
    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo() {
    UserResponse response = userService.getUserInfo();
    return ResponseEntity.ok(response);
    }


}