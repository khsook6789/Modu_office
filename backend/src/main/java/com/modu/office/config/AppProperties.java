package com.modu.office.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 애플리케이션 전역 설정 (app.*)
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt) {

    /**
     * JWT 관련 설정 (app.jwt.*)
     */
    public record Jwt(
            String secret,
            long accessTokenExpirationMilliseconds,
            long refreshTokenExpirationMilliseconds) {
    }
}
