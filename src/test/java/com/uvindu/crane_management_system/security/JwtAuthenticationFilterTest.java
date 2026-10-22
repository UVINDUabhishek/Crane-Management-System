package com.uvindu.crane_management_system.security;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.model.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest extends BaseUnitTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldContinueFilterChainWhenNoAuthorizationHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    void shouldContinueFilterChainWhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    void shouldAuthenticateWhenValidTokenProvided() throws ServletException, IOException {
        // Arrange
        String validToken = "valid.jwt.token";
        String username = "test@example.com";

        SecurityContextHolder.clearContext();

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + validToken);

        when(jwtService.extractUsername(validToken))
                .thenReturn(username);

        UserDetails userDetails =
                JwtTestUtil.createTestUserDetails(username, UserRole.ADMIN);

        when(userDetailsService.loadUserByUsername(username))
                .thenReturn(userDetails);

        when(jwtService.isTokenValid(validToken, userDetails))
                .thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // Assert
        verify(jwtService).extractUsername(validToken);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtService).isTokenValid(validToken, userDetails);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);

        verify(filterChain).doFilter(request, response);

        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotAuthenticateWhenInvalidTokenProvided() throws ServletException, IOException {

        // Arrange
        String invalidToken = "invalid.jwt.token";
        String username = "test@example.com";

        SecurityContextHolder.clearContext();

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + invalidToken);

        when(jwtService.extractUsername(invalidToken))
                .thenReturn(username);

        UserDetails userDetails =
                JwtTestUtil.createTestUserDetails(username, UserRole.ADMIN);

        when(userDetailsService.loadUserByUsername(username))
                .thenReturn(userDetails);

        when(jwtService.isTokenValid(invalidToken, userDetails))
                .thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // Assert
        verify(jwtService).extractUsername(invalidToken);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtService).isTokenValid(invalidToken, userDetails);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        // Invalid JWT must be rejected with HTTP 401
        verify(response).sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Invalid or expired JWT"
        );

        // The request must not continue through the filter chain
        verify(filterChain, never()).doFilter(request, response);

        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotAuthenticateWhenTokenUsernameIsNull() throws ServletException, IOException {
        // Arrange
        String token = "some.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtService).extractUsername(token);
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenUserAlreadyAuthenticated() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        String username = "test@example.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(username);

        // Simulate already authenticated context
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtService).extractUsername(token);
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);

        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldExtractTokenFromAuthorizationHeader() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");

        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(JwtTestUtil.createTestUserDetails("test@example.com", UserRole.ADMIN));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtService).extractUsername(token);
    }

    @Test
    void shouldRejectDisabledUserBeforeAuthenticating() throws Exception {
        SecurityContextHolder.clearContext();
        UserDetails disabled = org.springframework.security.core.userdetails.User
                .withUsername("disabled@test.local").password("encoded").roles("SALES").disabled(true).build();
        when(request.getHeader("Authorization")).thenReturn("Bearer existing-token");
        when(jwtService.extractUsername("existing-token")).thenReturn(disabled.getUsername());
        when(userDetailsService.loadUserByUsername(disabled.getUsername())).thenReturn(disabled);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT");
        verifyNoInteractions(filterChain);
        verify(jwtService, never()).isTokenValid(anyString(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
