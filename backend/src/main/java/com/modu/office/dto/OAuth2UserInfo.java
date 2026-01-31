package com.modu.office.dto;

/**
 * OAuth2 제공자별 사용자 정보를 추상화하는 인터페이스
 */
public interface OAuth2UserInfo {
    String getProviderId();

    String getProvider();

    String getEmail();

    String getName();
}
