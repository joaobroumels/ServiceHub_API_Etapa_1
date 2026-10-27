package com.servicehub.security;

import com.servicehub.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationSeconds;
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds) {
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length < 32) throw new IllegalArgumentException("Chave curta");
            this.key = Keys.hmacShaKeyFor(decoded);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("JWT_SECRET deve conter Base64 de pelo menos 32 bytes aleatórios");
        }
        if (expirationSeconds < 1 || expirationSeconds > 86400) {
            throw new IllegalArgumentException("Expiração JWT deve estar entre 1 e 86400 segundos");
        }
        this.expirationSeconds = expirationSeconds;
    }
    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder().issuer("servicehub-api").subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of("ROLE_" + user.getUserType().name()))
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key, Jwts.SIG.HS256).compact();
    }
    public Claims parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer("servicehub-api").build()
                .parseSignedClaims(token).getPayload();
        if (claims.getExpiration() == null || claims.getIssuedAt() == null) {
            throw new io.jsonwebtoken.MalformedJwtException("Claims obrigatórias ausentes");
        }
        return claims;
    }
    public long expirationSeconds() { return expirationSeconds; }
}
