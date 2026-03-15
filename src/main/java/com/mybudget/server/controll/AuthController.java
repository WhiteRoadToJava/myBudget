
package com.mybudget.server.controll;


import com.mybudget.server.dto.AuthRequest;
import com.mybudget.server.dto.AuthResponse;
import com.mybudget.server.dto.RegisterRequest;
import com.mybudget.server.dto.RegisterResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
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

import com.mybudget.server.module.Role;
import com.mybudget.server.module.User;
import com.mybudget.server.service.UserService;
import com.mybudget.server.util.JwtUtil;



import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // check if username already exists
        if(userService.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Username already exists.");
        }

        // map the AuthRequest to a User entity
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());

        // assign roles
        if(registerRequest.getRoles() == null || registerRequest.getRoles().isEmpty()) {
            user.setRoles(Set.of(Role.USER));
        } else {
            user.setRoles(registerRequest.getRoles());
        }

        // register the user using UserService
        userService.registerUser(user);

        // create respons object
        RegisterResponse response = new RegisterResponse(
                "User registered successfully",
                user.getUsername(),
                user.getRoles()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login (@Valid @RequestBody AuthRequest authRequest, HttpServletResponse response) {

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
                    "Login successful",
                    userDetails.getUsername(),
                    userService.findByUsername(userDetails.getUsername()).getRoles()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(authResponse);

            // return response with cookie-header and body
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



















}
