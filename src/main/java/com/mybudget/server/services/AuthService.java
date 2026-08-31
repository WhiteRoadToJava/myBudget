package com.mybudget.server.services;

import com.mybudget.server.dto.auth.PasswordRequest;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.Role;
import com.mybudget.server.repositories.UserRepository;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserUtils userUtils;
    private final UserRepository userRepository;

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


    public void updatePassword(PasswordRequest request){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        if(!request.getNewPassword().equals(request.getConfirmPassword())){
            throw new ResourceNotFoundException("password is not match");
        }

        if(!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())){
            throw  new ResourceNotFoundException("Password is not correnct...");
        };
        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
    }
}
