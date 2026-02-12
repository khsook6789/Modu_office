package com.modu.office.security;

import com.modu.office.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(appProperties.jwt().secret().getBytes());
    }

    public String generateAccessToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + appProperties.jwt().accessTokenExpirationMilliseconds());

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // OAuth2 로그인을 위한 오버로드 메서드
    public String generateAccessToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + appProperties.jwt().accessTokenExpirationMilliseconds());

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken() {
        return java.util.UUID.randomUUID().toString();
    }

    // OAuth2 로그인을 위한 오버로드 메서드
    public String generateRefreshToken(String email) {
        return java.util.UUID.randomUUID().toString();
    }

    public long getRefreshTokenExpirationInMs() {
        return appProperties.jwt().refreshTokenExpirationMilliseconds();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException ex) {
            log.error("유효하지 않은 토큰");
        } catch (ExpiredJwtException ex) {
            log.error("토큰 만료됨");
        } catch (UnsupportedJwtException ex) {
            log.error("지원하지 않는 토큰");
        } catch (IllegalArgumentException ex) {
            log.error("토큰 정보 없음");
        }
        return false;
    }
}
