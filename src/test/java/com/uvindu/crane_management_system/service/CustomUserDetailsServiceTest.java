package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.model.User;
import com.uvindu.crane_management_system.model.UserRole;
import com.uvindu.crane_management_system.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldLoadUserByEmailSuccessfully() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPassword("$2a$10$hashedPassword");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);

        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername("admin@example.com");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("admin@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");

        verify(userRepository).findByEmail("admin@example.com");
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                customUserDetailsService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByEmail("unknown@example.com");
    }

    @Test
    void shouldDisableUserWhenUserIsInactive() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("inactive@example.com");
        user.setPassword("$2a$10$hashedPassword");
        user.setRole(UserRole.SALES);
        user.setActive(false);

        when(userRepository.findByEmail("inactive@example.com"))
                .thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername("inactive@example.com");

        // Assert
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_SALES");

        verify(userRepository).findByEmail("inactive@example.com");
    }
}