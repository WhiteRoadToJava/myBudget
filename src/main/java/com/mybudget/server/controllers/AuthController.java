
package com.mybudget.server.controllers;


import com.mybudget.server.dto.auth.*;
import com.mybudget.server.dto.RegisterRequest;
import com.mybudget.server.dto.RegisterResponse;
import com.mybudget.server.dto.auth.password.ResetPasswordRequest;
import com.mybudget.server.modules.enums.Role;
import com.mybudget.server.modules.User;
import com.mybudget.server.services.AccountVerificationService;
import com.mybudget.server.services.AuthService;
import com.mybudget.server.services.UserService;

import com.mybudget.server.services.auth.PasswordResetService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mybudget.server.util.JwtUtil;


import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final AuthService authService;
    private final AccountVerificationService accountVerificationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // check if username already exists
        if (userService.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Username already exists.");
        }

        // map the AuthRequest to a User entity
        User user = mapRegisterRequestToUser(registerRequest);
        user.setRoles(Set.of(Role.USER));
        user.setEnabled(false);
        authService.registerUser(user);

        accountVerificationService.sendActivationEmail(user, registerRequest.getLanguage());

        // create respons object
        RegisterResponse response = new RegisterResponse(
                "User registered successfully",
                user.getUsername(),
                user.getRoles()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/activate")
    public ResponseEntity<?> activate(@Valid @RequestBody ActivationRequest request) {
        accountVerificationService.activate(request.getToken());
        return ResponseEntity.ok(Map.of("Message", "Account activated"));
    }


    @PostMapping("/resend-activation")
    public ResponseEntity<?> resendActivation(@Valid @RequestBody EmailRequest request) {
        accountVerificationService.resendActivation(request.getUsername(), request.getLanguage());

        return ResponseEntity.ok(
                Map.of(
                        "Message",
                        "If the account exists and is not activated, mew email has been sent"
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login (@Valid @RequestBody AuthRequest authRequest, HttpServletResponse response){

        try {
            // authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            // set authentication in the security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // get UserDetails
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // generate JWT token
            String jwt = jwtUtil.generateToken(userDetails);
            ResponseCookie jwtCookie = ResponseCookie.from("jwt", jwt)
                    .httpOnly(true) // prevents javascript to get cookie
                    .secure(false) //IMPORTANT TO CHANGE IN PRODUCTION TO TRUE
                    .path("/")  // cookies is available in all application
                    .maxAge(10 * 60 * 60) // valid for 10h
                    .sameSite("Strict") // Lax & None
                    .build();

            // create response object
            AuthResponse authResponse = new AuthResponse(
                    jwt,
                    userDetails.getUsername(),
                    userService.findByUsername(userDetails.getUsername()).getRoles()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(authResponse);

            // return response with cookie-header and body
        } catch (DisabledException e) {
            // Spring checks "enabled" BEFORE the password. Only reveal that the account is not
            // activated when the password is correct, otherwise anyone could probe emails.
            if (authService.passwordMatches(authRequest.getUsername(), authRequest.getPassword())) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "code", "ACCOUNT_NOT_ACTIVATED",
                                "message", "Account is not activated. Check your email."));
            }
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Incorrect username or password");
        } catch (AuthenticationException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Incorrect username or password");
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // create cookie with same name and empty value to delete it
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(false) //IMPORTANT TO CHANGE IN PRODUCTION TO TRUE
                .path("/")
                .maxAge(0) // expire immediately
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body("Logout successful");
    }

    // kolla om en user är authenticated
    @GetMapping("/check")
    public ResponseEntity<?> checkAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // kontrollera om användaren är authenticated
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.ok().body(null);
        }

        // returnera user info om authentication
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());

        // added getter for everything i can to return in the respond to the client
        // also had to add them to login since we use the same DTO..
        // a better solution would be to create two separate DTOs
        return ResponseEntity.ok(new AuthResponse(
                "Authenticated",
                user.getUsername(),
                user.getRoles()
        ));
    }


    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassord(@Valid @RequestBody PasswordRequest requset) {
        authService.updatePassword(requset);
        return ResponseEntity.ok("Password updated successfully");
    }


    private User mapRegisterRequestToUser(RegisterRequest registerRequest) {
        return new User(
                registerRequest.getUsername(),
                registerRequest.getPassword(),
                registerRequest.getFirstName(),
                registerRequest.getLastName(),
                registerRequest.getPhone(),
                registerRequest.getExpireDate()
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody EmailRequest request) {
        passwordResetService.requestReset(request.getUsername(), request.getLanguage());
        return ResponseEntity.ok(Map.of("message",
                "If an account exists for this email, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
