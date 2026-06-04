package com.empresa.chat.service.auth;

import com.empresa.chat.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String gerarToken(UserDetails userDetails) {
        return gerarToken(Map.of(), userDetails);
    }

    public String gerarToken(Map<String, Object> claims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.expiration()))
                .signWith(getSignKey())
                .compact();
    }

    public boolean isTokenValido(String token, UserDetails userDetails) {
        return extrairUsername(token).equals(userDetails.getUsername()) && !isTokenExpirado(token);
    }

    public String extrairUsername(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    private boolean isTokenExpirado(String token) {
        return extrairClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extrairClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token).getPayload());
    }

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
