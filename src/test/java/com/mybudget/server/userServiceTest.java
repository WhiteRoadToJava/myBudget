package com.mybudget.server;


import com.mybudget.server.dto.user.UserResponse;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.UserRepository;
import com.mybudget.server.services.UserService;
import com.mybudget.server.util.UserUtils;
import com.mybudget.server.util.mapMappers.UserMappers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class userServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserUtils userUtils;

    @Mock
    private UserMappers userMappers;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private UserResponse mockUserResponse;
    private final String userName = "msaabbas6@gmail.com";

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUsername(userName);

        mockUserResponse = new UserResponse();
        mockUserResponse.setUsername(userName);
    }

    @Test
    void findByUsernameTest() {
        when(userRepository.findByUsername(userName)).thenReturn(Optional.of(mockUser));

        User result = userService.findByUsername(userName);

        assertNotNull(result);
        assertEquals(userName, result.getUsername());
    }

    @Test
    void findyUsernameToFoundTest() {
        when(userRepository.findByUsername(userName)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.findByUsername(userName);
        });
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void existeByUsernameTest(){
        when(userRepository.findByUsername(userName)).thenReturn(Optional.of(mockUser));

        boolean result = userService.existsByUsername(userName);

        assertTrue(result);
        assertEquals(userName, mockUser.getUsername());
    }

    @Test
    void getUserInfoTest(){
        when(userRepository.findByUsername(userName)).thenReturn(Optional.of(mockUser));
        when(userMappers.mapToUserResponse(mockUser)).thenReturn(mockUserResponse);

        UserResponse result = userService.getUserInfo();

        assertNotNull(result);
        assertEquals(userName, result.getUsername());

        verify(userUtils, times(1)).getCurrentAuthenticatedUser();
        verify(userMappers, times(1)).mapToUserResponse(mockUser);
    }
}
