package com.modu.office.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 토스페이먼츠 WebClient 설정
 *
 * <p>
 * 인증 방식: HTTP Basic Auth
 * 형식: Authorization: Basic {Base64(secretKey + ":")}
 * Why: 토스 API는 시크릿 키를 user ID로, 비밀번호는 빈 값(콜론 이후 없음)으로 사용하는 Basic Auth 방식
 * </p>
 */
@Configuration
@RequiredArgsConstructor
public class TossPaymentsConfig {

    private final AppProperties appProperties;

    @Bean(name = "tossWebClient")
    public WebClient tossWebClient() {
        AppProperties.TossPayments toss = appProperties.tossPayments();
        String baseUrl = toss != null && toss.baseUrl() != null
                ? toss.baseUrl()
                : "https://api.tosspayments.com";
        String secretKey = toss != null ? toss.secretKey() : "";

        // Basic 인증 헤더: Base64("secretKey:")
        String encoded = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
