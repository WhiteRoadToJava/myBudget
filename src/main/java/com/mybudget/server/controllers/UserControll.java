package com.mybudget.server.controllers;

import com.mybudget.server.dto.RegisterRequest;
import com.mybudget.server.dto.RegisterResponse;
import com.mybudget.server.dto.user.UserRequset;
import com.mybudget.server.dto.user.UserResponse;
import com.mybudget.server.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserControll {
private final UserService userService;



    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo() {
    UserResponse response = userService.getUserInfo();
    return ResponseEntity.ok(response);
    }

@PatchMapping("update-user")
    public  ResponseEntity<UserResponse> updateUser(@RequestBody UserRequset requset){
        UserResponse response = userService.updateUser(requset);
        return  ResponseEntity.status(HttpStatus.OK).body(response);
}




}