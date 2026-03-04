package com.modu.office.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class ValidImageUrlValidator implements ConstraintValidator<ValidImageUrl, String> {

    // 내부망 IP (10.x.x.x, 172.16-31.x.x, 192.168.x.x, 127.x.x.x 등) 차단용 정규식
    private static final Pattern INTERNAL_IP_PATTERN = Pattern.compile(
            "^(127\\.\\d+\\.\\d+\\.\\d+)|" +
                    "(10\\.\\d+\\.\\d+\\.\\d+)|" +
                    "(192\\.168\\.\\d+\\.\\d+)|" +
                    "(172\\.(1[6-9]|2[0-9]|3[0-1])\\.\\d+\\.\\d+)$");

    @Override
    public boolean isValid(String url, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(url)) {
            return false; // 필수값 체크는 별도 어노테이션(@NotBlank)에서 수행하지만, 비어있으면 유효하지 않은 것으로 간주
        }

        try {
            URI uri = new URI(url);

            // 1. 프로토콜 검사 (http 또는 https 만 허용)
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }

            // 2. 호스트 검사
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                return false;
            }

            // localhost 차단
            if (host.equalsIgnoreCase("localhost")) {
                return false;
            }

            // 3. 내부 IP 대역 차단
            if (INTERNAL_IP_PATTERN.matcher(host).matches()) {
                return false;
            }

            return true;

        } catch (URISyntaxException e) {
            return false;
        }
    }
}
