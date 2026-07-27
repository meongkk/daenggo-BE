package com.daenggo.backend.auth.oauth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * 카카오 Access Token으로 사용자 정보를 조회하고 이메일을 검증한다.
 */
@Service
public class KakaoOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String KAKAO_REGISTRATION_ID = "kakao";

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(final OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {
        final String registrationId =
                userRequest.getClientRegistration().getRegistrationId();
        if (!KAKAO_REGISTRATION_ID.equals(registrationId)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "지원하지 않는 소셜 로그인 제공자입니다."
            );
        }

        final OAuth2User oauth2User = delegate.loadUser(userRequest);
        final KakaoOAuth2UserInfo userInfo =
                KakaoOAuth2UserInfo.from(oauth2User.getAttributes());

        return new KakaoOAuth2User(
                oauth2User.getAuthorities(),
                oauth2User.getAttributes(),
                userInfo
        );
    }
}
