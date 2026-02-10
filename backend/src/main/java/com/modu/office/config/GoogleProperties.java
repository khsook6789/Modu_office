package com.modu.office.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google 서비스 설정 (google.*)
 */
@ConfigurationProperties(prefix = "google")
public record GoogleProperties(Maps maps) {

    /**
     * Google Maps 관련 설정 (google.maps.*)
     */
    public record Maps(
            String apiKey) {
    }
}
