package com.modu.office.service;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.modu.office.config.GoogleProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Google Maps Geocoding API를 사용하여 주소를 좌표로 변환하는 서비스
 */
@Slf4j
@Service
public class GeocodingService {

    private final GeoApiContext context;

    public GeocodingService(GoogleProperties googleProperties) {
        this.context = new GeoApiContext.Builder()
                .apiKey(googleProperties.maps().apiKey())
                .build();
    }

    /**
     * 주소를 위도/경도로 변환
     *
     * @param address 변환할 주소
     * @return 위도/경도 객체 (실패 시 Empty)
     */
    public Optional<LatLng> geocode(String address) {
        try {
            GeocodingResult[] results = GeocodingApi.geocode(context, address).await();
            if (results != null && results.length > 0) {
                return Optional.of(results[0].geometry.location);
            }
        } catch (Exception e) {
            log.error("Geocoding failed for address: {}", address, e);
        }
        return Optional.empty();
    }
}
