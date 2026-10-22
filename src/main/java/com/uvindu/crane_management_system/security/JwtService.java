package com.uvindu.crane_management_system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    // In production, keep this in application.properties or environment variables.
    // private final String SECRET = "myverysecretkeymyverysecretkey123456";

    @Value("${jwt.secret}")
    private String SECRET;

    private SecretKey getKey() {

        return Keys.hmacShaKeyFor(
                SECRET.getBytes()
        );

    }

    // Generate JWT
    public String generateToken(String email) {

        return Jwts.builder()
                .subject(email)
                .issuer("CraneManagementSystem")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getKey())
                .compact();

    }

    // Extract email (subject)
    public String extractUsername(String token) {

        return extractClaim(token, Claims::getSubject);

    }

    // Generic claim extractor
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {

        Claims claims = extractAllClaims(token);

        return claimsResolver.apply(claims);

    }

    // Parse JWT
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extract expiration
    public Date extractExpiration(String token) {

        return extractClaim(token, Claims::getExpiration);

    }

    // Check expiration
    private boolean isTokenExpired(String token) {

        return extractExpiration(token).before(new Date());

    }

    // Validate token
    public boolean isTokenValid(String token, UserDetails userDetails) {

        final String username = extractUsername(token);

        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);

    }
}
