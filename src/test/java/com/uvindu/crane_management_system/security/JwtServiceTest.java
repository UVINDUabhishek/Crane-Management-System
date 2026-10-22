package com.uvindu.crane_management_system.security;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.model.UserRole;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest extends BaseUnitTest {

    private static final String TEST_SECRET = "testsecretkeyfortestingpurposesonly123456";

    @Test
    void shouldGenerateToken() {
        // Arrange
        JwtService jwtService = createTestJwtService();
        String testUsername = "test@example.com";

        // Act
        String token = jwtService.generateToken(testUsername);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void shouldExtractUsernameFromToken() {
        // Arrange
        JwtService jwtService = createTestJwtService();
        String testUsername = "test@example.com";
        String token = jwtService.generateToken(testUsername);

        // Act
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertThat(extractedUsername).isEqualTo(testUsername);
    }

    @Test
    void shouldExtractExpirationFromToken() {
        // Arrange
        JwtService jwtService = createTestJwtService();
        String testUsername = "test@example.com";
        String token = jwtService.generateToken(testUsername);

        // Act
        Date expiration = jwtService.extractExpiration(token);

        // Assert
        assertThat(expiration).isNotNull();
        assertThat(expiration.after(new Date())).isTrue();
    }


    @Test
    void shouldValidateValidToken() {
        // Arrange
        JwtService jwtService = createTestJwtService();
        String testUsername = "test@example.com";
        String token = jwtService.generateToken(testUsername);
        UserDetails userDetails = JwtTestUtil.createTestUserDetails(testUsername, UserRole.ADMIN);

        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldNotValidateTokenWithWrongUsername() {
        // Arrange
        JwtService jwtService = createTestJwtService();
        String testUsername = "test@example.com";
        String token = jwtService.generateToken(testUsername);
        UserDetails userDetails = JwtTestUtil.createTestUserDetails("different@example.com", UserRole.ADMIN);

        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertThat(isValid).isFalse();
    }


    @Test
    void shouldExtractClaimsFromToken() {
        // Arrange
        JwtService jwtService = createTestJwtService();
        String testUsername = "test@example.com";
        String token = jwtService.generateToken(testUsername);

        // Act
        String issuer = jwtService.extractClaim(token, Claims::getIssuer);

        // Assert
        assertThat(issuer).isEqualTo("CraneManagementSystem");
    }

    @Test
    void shouldHaveValidExpirationTime() {
        // Arrange
        JwtService jwtService = createTestJwtService();
        String testUsername = "test@example.com";
        String token = jwtService.generateToken(testUsername);

        // Act
        Date expiration = jwtService.extractExpiration(token);
        Date now = new Date();
        long diffInMillis = expiration.getTime() - now.getTime();
        long diffInMinutes = diffInMillis / (1000 * 60);

        // Assert - Token should expire in approximately 60 minutes
        assertThat(diffInMinutes).isGreaterThan(55);
        assertThat(diffInMinutes).isLessThan(65);
    }

    @Test
    void shouldThrowExceptionForExpiredToken() {
        // Arrange
        JwtService jwtService = createTestJwtService();
        String testUsername = "test@example.com";

        String expiredToken = Jwts.builder()
                .subject(testUsername)
                .issuer("CraneManagementSystem")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes()))
                .compact();

        UserDetails userDetails =
                JwtTestUtil.createTestUserDetails(testUsername, UserRole.ADMIN);

        // Act & Assert
        assertThatThrownBy(() ->
                jwtService.isTokenValid(expiredToken, userDetails)
        )
                .isInstanceOf(ExpiredJwtException.class);
    }


    private JwtService createTestJwtService() {
        // Create a test-specific JwtService
        JwtService jwtService = new JwtService();
        // Use reflection to set the secret field
        try {
            java.lang.reflect.Field secretField = JwtService.class.getDeclaredField("SECRET");
            secretField.setAccessible(true);
            secretField.set(jwtService, TEST_SECRET);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set test secret", e);
        }
        return jwtService;
    }
}
