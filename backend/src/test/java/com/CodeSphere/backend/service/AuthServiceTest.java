package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.RegisterRequest;
import com.CodeSphere.backend.model.RefreshToken;
import com.CodeSphere.backend.model.Role;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.security.CustomUserDetailsService;
import com.CodeSphere.backend.security.JwtTokenProvider;
import com.CodeSphere.backend.service.impl.AuthServiceImpl; // 👈 1. IMPORT ADDED
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService; // 2. CONCRETE CLASS INJECTED

    private RegisterRequest registerRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@CodeSphere.com");
        registerRequest.setPassword("rawPassword");

        mockUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@CodeSphere.com")
                .password("encodedPassword")
                .role(Role.ROLE_CANDIDATE)
                .emailVerified(false)
                .build();
    }

    // --- Registration Tests ---

    @Test
    void register_Success() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);
        when(jwtTokenProvider.generateToken(mockAuth)).thenReturn("mockJwt");

        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("mockRefreshToken");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(mockRefreshToken);

        assertDoesNotThrow(() -> authService.register(registerRequest));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals(Role.ROLE_CANDIDATE, savedUser.getRole());
        assertFalse(savedUser.isEmailVerified());
    }

    @Test
    void register_ThrowsException_WhenUsernameExists() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertTrue(exception.getMessage().contains("Username is already taken"));
        verify(userRepository, never()).save(any(User.class));
    }
}