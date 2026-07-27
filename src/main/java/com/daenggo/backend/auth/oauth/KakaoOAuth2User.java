package com.daenggo.backend.auth.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Security 인증 정보와 검증된 카카오 사용자 정보를 함께 보관한다.
 */
public class KakaoOAuth2User implements OAuth2User {

    private final List<GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final KakaoOAuth2UserInfo userInfo;

    public KakaoOAuth2User(
            final Collection<? extends GrantedAuthority> authorities,
            final Map<String, Object> attributes,
            final KakaoOAuth2UserInfo userInfo
    ) {
        this.authorities = List.copyOf(authorities);
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        this.userInfo = userInfo;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return userInfo.providerId();
    }

    public KakaoOAuth2UserInfo getUserInfo() {
        return userInfo;
    }
}
