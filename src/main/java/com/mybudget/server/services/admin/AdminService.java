package com.mybudget.server.services.admin;

import com.mybudget.server.dto.user.UpdateExpireDate;
import com.mybudget.server.dto.user.UserResponse;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.exeptions.UnauthorizedException;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.Role;
import com.mybudget.server.repositories.UserRepository;
import com.mybudget.server.util.UserUtils;
import com.mybudget.server.util.mapMappers.UserMappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.util.Date;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserUtils userUtils;
    private final UserRepository userRepository;
    public final UserMappers userMappers;
    private final PasswordEncoder passwordEncoder;




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



    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateExpiredDate( String userId, UpdateExpireDate date){
        // authentication user
        User currentAuthenticatedUser = userUtils.getCurrentAuthenticatedUser();
        // find the user you want to update the expired date
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // check it the user has the admin role
                user.setExpireDate(date.getExpireDate());
                 User updatedUser = userRepository.save(user);

        return userMappers.mapToUserResponse(updatedUser);
    }


    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserDetails(String userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMappers.mapToUserResponse(user);
    }
}
