package com.servicehub.security;

import com.servicehub.model.User;
import com.servicehub.model.UserType;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private final JwtService jwt = new JwtService(SECRET, 86400);

    @Test
    void shouldSignTokenWithSubjectRoleAndExactly24HourLifetime() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 42L);
        user.setUserType(UserType.PROVIDER);
        var claims = jwt.parse(jwt.generateToken(user));
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo("servicehub-api");
        assertThat(claims.get("roles", List.class)).containsExactly("ROLE_PROVIDER");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(86_400_000);
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void shouldRejectExpiredTokenEvenWithValidSignature() {
        String expired = Jwts.builder().issuer("servicehub-api").subject("42")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), Jwts.SIG.HS256).compact();
        assertThatThrownBy(() -> jwt.parse(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectShortKeysInvalidExpirationAndWrongIssuer() {
        assertThatThrownBy(() -> new JwtService("c2hvcnQ=", 86400)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(SECRET, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(SECRET, 86401)).isInstanceOf(IllegalArgumentException.class);
        String wrongIssuer = Jwts.builder().issuer("another-api").subject("42")
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), Jwts.SIG.HS256).compact();
        assertThatThrownBy(() -> jwt.parse(wrongIssuer)).isInstanceOf(JwtException.class);
    }
}
