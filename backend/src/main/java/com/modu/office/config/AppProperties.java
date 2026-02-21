package com.modu.office.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

/**
 * 애플리케이션 전역 설정 (app.*)
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors) {

    /**
     * JWT 관련 설정 (app.jwt.*)
     */
    public record Jwt(
            String secret,
            long accessTokenExpirationMilliseconds,
            long refreshTokenExpirationMilliseconds) {
    }

    /**
     * CORS 관련 설정 (app.cors.*)
     * Why: 환경별(local/stage/prod)로 허용 Origin이 달라지므로 외부 설정으로 분리.
     */
    public record Cors(List<String> allowedOrigins) {
    }
}
