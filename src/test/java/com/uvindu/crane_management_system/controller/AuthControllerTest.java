package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.LoginRequest;
import com.uvindu.crane_management_system.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;


    // ---------------------------------------------------------
    // Login - Success
    // ---------------------------------------------------------

    @Test
    void shouldLoginSuccessfullyAndReturnToken() {

        // Arrange
        LoginRequest request = new LoginRequest();

        request.setEmail("test@example.com");
        request.setPassword("password123");

        String expectedToken = "generated.jwt.token";

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(authentication);

        when(jwtService.generateToken(request.getEmail()))
                .thenReturn(expectedToken);

        // Act
        ResponseEntity<?> response =
                authController.login(request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(expectedToken);

        verify(authenticationManager).authenticate(
                argThat(auth ->
                        auth instanceof UsernamePasswordAuthenticationToken
                                && auth.getName().equals("test@example.com")
                                && auth.getCredentials().equals("password123")
                )
        );

        verify(jwtService).generateToken("test@example.com");
    }


    // ---------------------------------------------------------
    // Login - Authentication Failure
    // ---------------------------------------------------------

    @Test
    void shouldNotGenerateTokenWhenAuthenticationFails() {

        // Arrange
        LoginRequest request = new LoginRequest();

        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(response.getBody())
                .isEqualTo("Invalid email or password");

        // JWT must never be generated when authentication fails
        verify(jwtService, never()).generateToken(anyString());
    }
}