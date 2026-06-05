package vn.springboot.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import vn.springboot.security.JwtProperties;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * Issues and validates stateless access tokens (HS512 signed JWTs).
 * Refresh tokens are handled separately by the auth service (DB-backed).
 */
@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_AUTHORITIES = "authorities";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    }

    public String generateAccessToken(UserDetails userDetails) {
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(userDetails.getUsername())
                .claim(CLAIM_AUTHORITIES, authorities)
                .issuedAt(new Date(now))
                .expiration(new Date(now + properties.getAccessTokenExpiration()))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Returns true only if the token is well-formed, unexpired and belongs to the given user. */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            return userDetails.getUsername().equals(claims.getSubject())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT: {}", ex.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
