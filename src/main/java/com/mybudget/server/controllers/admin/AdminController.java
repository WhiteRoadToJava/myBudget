package com.mybudget.server.controllers.admin;


import com.mybudget.server.dto.RegisterRequest;
import com.mybudget.server.dto.RegisterResponse;
import com.mybudget.server.dto.user.UpdateExpireDate;
import com.mybudget.server.dto.user.UserResponse;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.Role;
import com.mybudget.server.services.UserService;
import com.mybudget.server.services.admin.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.naming.AuthenticationException;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminController {
    private final AdminService adminService;
    private final UserService userService;


    @PostMapping("user-register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // check if username already exists
        if (userService.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Username already exists.");
        }

        // map the AuthRequest to a User entity
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());

        // assign roles
        if (registerRequest.getRoles() == null || registerRequest.getRoles().isEmpty()) {
            user.setRoles(Set.of(Role.USER));
        } else {
            user.setRoles(registerRequest.getRoles());
        }

        // register the user using UserService
        adminService.registerUser(user);

        // create respons object
        RegisterResponse response = new RegisterResponse(
                "User registered successfully",
                user.getUsername(),
                user.getRoles()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/update-expired-date/{userId}")
    public ResponseEntity<?> updateExpiredDate( @PathVariable String userId, @RequestBody UpdateExpireDate date) {
        UserResponse response = adminService.updateExpiredDate(userId, date);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/user-details/{userId}")
    public ResponseEntity<?> getUserDetails(@PathVariable String userId){
        UserResponse response = adminService.getUserDetails(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
