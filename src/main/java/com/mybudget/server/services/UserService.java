package com.mybudget.server.services;


import com.mybudget.server.dto.user.UserResponse;
import com.mybudget.server.util.UserUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mybudget.server.modules.enums.Role;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.UserRepository;

import java.util.Set;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserUtils userUtils;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserUtils userUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userUtils = userUtils;
    }

    // register user
    public void registerUser(User user) {
        // hash password
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // ensure the user has at least default role USER
        if(user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(Set.of(Role.USER));
        }

        userRepository.save(user);
    }

    // find user by username
    public User findByUsername(String userName) {
        return userRepository.findByUsername(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // check if username already exists
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }


    public UserResponse getUserInfo() {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        return new UserResponse(
                currentUser.getUsername(),
                currentUser.getFirstName() + " " + currentUser.getLastName()
        );
    }



















}