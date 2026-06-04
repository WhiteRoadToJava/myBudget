package com.mybudget.server.services;


import com.mybudget.server.dto.user.UserRequset;
import com.mybudget.server.dto.user.UserResponse;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mybudget.server.modules.enums.Role;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.UserRepository;
import com.mybudget.server.util.mapMappers.UserMappers;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserUtils userUtils;
    private final UserMappers userMappers;






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
        return userMappers.mapToUserResponse(currentUser);
    }

    public UserResponse updateUser(UserRequset request) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();


        currentUser.setFirstName(request.getFirstname());
        currentUser.setLastName(request.getLastname());
        currentUser.setPhone(request.getPhone());



    return  userMappers.mapToUserResponse(userRepository.save(currentUser));
    }




    }


















