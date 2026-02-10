package com.modu.office.dto;

import java.util.Map;

/**
 * 네이버 OAuth2 사용자 정보 구현체
 * 네이버 API 응답 형식: { "resultcode": "00", "message": "success", "response": {...}
 * }
 */
public class NaverOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        // 네이버는 사용자 정보를 "response" 키 안에 포함
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        this.attributes = response;
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }
}
