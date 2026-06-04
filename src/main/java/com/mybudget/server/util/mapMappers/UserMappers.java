package com.mybudget.server.util.mapMappers;

import com.mybudget.server.dto.user.UserResponse;
import com.mybudget.server.modules.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMappers {


    public UserResponse mapToUserResponse(User user) {
        String expireDate = user.getExpireDate();
        if(expireDate == null) expireDate = "Not selected...";
        return new UserResponse(
                user.getUsername(),
                user.getFirstName() + " " + user.getLastName(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                expireDate
        );
    }

}
