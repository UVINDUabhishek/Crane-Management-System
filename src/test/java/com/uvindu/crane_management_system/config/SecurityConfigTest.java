package com.uvindu.crane_management_system.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

    @Test
    void shouldCreatePasswordEncoder() {
        // Arrange
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // Assert
        assertThat(passwordEncoder).isNotNull();
    }

    @Test
    void shouldEncodePassword() {
        // Arrange
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "testPassword123";

        // Act
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Assert
        assertThat(encodedPassword).isNotNull();
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
    }

    @Test
    void shouldMatchEncodedPassword() {
        // Arrange
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act & Assert
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("wrongPassword", encodedPassword)).isFalse();
    }

    @Test
    void shouldProduceDifferentEncodingsForSamePassword() {
        // Arrange
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "testPassword123";

        // Act
        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);

        // Assert - BCrypt should produce different hashes for the same password due to salt
        assertThat(encoded1).isNotEqualTo(encoded2);
        assertThat(passwordEncoder.matches(rawPassword, encoded1)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, encoded2)).isTrue();
    }
}
