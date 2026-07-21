package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.ForgotPasswordRequest;
import com.CodeSphere.backend.dto.RegisterRequest;
import com.CodeSphere.backend.dto.ResetPasswordRequest;
import com.CodeSphere.backend.model.Role;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

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
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Mock
    private com.CodeSphere.backend.security.JwtTokenProvider jwtTokenProvider;

    @Mock
    private com.CodeSphere.backend.service.RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

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
        
        org.springframework.security.core.Authentication mockAuth = mock(org.springframework.security.core.Authentication.class);
        when(authenticationManager.authenticate(any(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);
        when(jwtTokenProvider.generateToken(mockAuth)).thenReturn("mockJwt");
        
        com.CodeSphere.backend.model.RefreshToken mockRefreshToken = new com.CodeSphere.backend.model.RefreshToken();
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

    // --- Forgot Password Tests ---
    /*
    @Test
    void processForgotPassword_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@CodeSphere.com");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(mockUser));

        authService.processForgotPassword(request);

        assertNotNull(mockUser.getResetPasswordToken());
        assertNotNull(mockUser.getResetPasswordTokenExpiry());
        verify(userRepository).save(mockUser);
    }
    */
    // --- Reset Password Tests ---

    /*
    @Test
    void processResetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newRawPassword");

        mockUser.setResetPasswordToken("valid-token");
        mockUser.setResetPasswordTokenExpiry(Instant.now().plusSeconds(600)); // 10 mins in future

        when(userRepository.findByResetPasswordToken(request.getToken())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("newEncodedPassword");

        authService.processResetPassword(request);

        assertEquals("newEncodedPassword", mockUser.getPassword());
        internalNullCheckAfterReset(mockUser);
        verify(userRepository).save(mockUser);
    }

    @Test
    void processResetPassword_ThrowsException_WhenTokenExpired() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");

        mockUser.setResetPasswordToken("expired-token");
        mockUser.setResetPasswordTokenExpiry(Instant.now().minusSeconds(600)); // 10 mins in past

        when(userRepository.findByResetPasswordToken(request.getToken())).thenReturn(Optional.of(mockUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.processResetPassword(request));
        assertTrue(exception.getMessage().contains("expired"));
        internalNullCheckAfterReset(mockUser);
        verify(userRepository).save(mockUser);
    }

    // --- Email Verification Tests ---

    @Test
    void verifyEmail_Success() {
        String token = "verification-token";
        mockUser.setEmailVerificationToken(token);
        mockUser.setEmailVerified(false);

        when(userRepository.findByEmailVerificationToken(token)).thenReturn(Optional.of(mockUser));

        authService.verifyEmail(token);

        assertTrue(mockUser.isEmailVerified());
        assertNull(mockUser.getEmailVerificationToken());
        verify(userRepository).save(mockUser);
    }
    */

    private void internalNullCheckAfterReset(User user) {
        assertNull(user.getResetPasswordToken());
        assertNull(user.getResetPasswordTokenExpiry());
    }
}