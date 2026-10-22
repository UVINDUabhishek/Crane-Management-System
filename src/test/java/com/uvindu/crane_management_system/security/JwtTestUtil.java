package com.uvindu.crane_management_system.security;

import com.uvindu.crane_management_system.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class JwtTestUtil {

    private static final String TEST_SECRET = "testsecretkeyfortestingpurposesonly123456";
    private static final SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    public static String generateTestToken(String username, UserRole role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 3600000); // 1 hour

        return Jwts.builder()
                .subject(username)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public static String generateExpiredToken(String username, UserRole role) {
        Date expiryDate = new Date(System.currentTimeMillis() - 3600000);
        Date issuedAt = new Date(expiryDate.getTime() - 3600000);

        return Jwts.builder()
                .subject(username)
                .claim("role", role.name())
                .issuedAt(issuedAt)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public static UserDetails createTestUserDetails(String username, UserRole role) {
        return new org.springframework.security.core.userdetails.User(
                username,
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    public static Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
