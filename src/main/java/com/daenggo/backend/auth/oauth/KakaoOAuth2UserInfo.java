package com.daenggo.backend.auth.oauth;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;

/**
 * 카카오 사용자 정보 응답에서 댕고 가입에 필요한 값만 꺼낸 객체
 *
 * @param providerId 카카오 회원번호
 * @param email 카카오계정 이메일
 * @param image 카카오 프로필 이미지 URL
 */
public record KakaoOAuth2UserInfo(
        String providerId,
        String email,
        String image
) {

    private static final String EMAIL_REQUIRED = "email_required";

    /**
     * 카카오 사용자 정보 API의 중첩 JSON을 안전하게 변환한다.
     *
     * @param attributes 카카오 사용자 정보 응답
     * @return 검증된 카카오 사용자 정보
     */
    public static KakaoOAuth2UserInfo from(final Map<String, Object> attributes) {
        final String providerId = stringValue(attributes.get("id"));
        if (!StringUtils.hasText(providerId)) {
            throw oauthError("invalid_kakao_user", "카카오 회원번호를 확인할 수 없습니다.");
        }

        final Object accountValue = attributes.get("kakao_account");
        if (!(accountValue instanceof Map<?, ?> account)) {
            throw oauthError(EMAIL_REQUIRED, "카카오 이메일 제공 동의가 필요합니다.");
        }

        final String email = stringValue(account.get("email"));
        final boolean valid = Boolean.TRUE.equals(account.get("is_email_valid"));
        final boolean verified = Boolean.TRUE.equals(account.get("is_email_verified"));
        if (!StringUtils.hasText(email) || !valid || !verified) {
            throw oauthError(
                    EMAIL_REQUIRED,
                    "인증되고 유효한 카카오 이메일 제공 동의가 필요합니다."
            );
        }

        String image = null;
        final Object profileValue = account.get("profile");
        if (profileValue instanceof Map<?, ?> profile) {
            image = stringValue(profile.get("profile_image_url"));
            if (!StringUtils.hasText(image)) {
                image = stringValue(profile.get("thumbnail_image_url"));
            }
            if (image != null && image.length() > 250) {
                image = null;
            }
        }

        return new KakaoOAuth2UserInfo(
                providerId,
                email.strip().toLowerCase(Locale.ROOT),
                image
        );
    }

    private static String stringValue(final Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static OAuth2AuthenticationException oauthError(
            final String errorCode,
            final String description
    ) {
        return new OAuth2AuthenticationException(
                new OAuth2Error(errorCode),
                description
        );
    }
}
