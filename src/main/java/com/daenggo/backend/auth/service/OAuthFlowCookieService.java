package com.daenggo.backend.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * OAuth 흐름 JWT를 JavaScript에서 읽을 수 없는 HttpOnly Cookie로 전달한다.
 */
@Service
public class OAuthFlowCookieService {

    public static final String COOKIE_NAME = "oauth_flow";
    private static final String COOKIE_PATH = "/api/auth/oauth";

    private final boolean secure;
    private final long expirationSeconds;

    public OAuthFlowCookieService(
            @Value("${auth.oauth2.cookie-secure}") final boolean secure,
            @Value("${auth.oauth2.flow-token-expiration-seconds}")
            final long expirationSeconds
    ) {
        this.secure = secure;
        this.expirationSeconds = expirationSeconds;
    }

    public void write(final HttpServletResponse response, final String token) {
        final ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofSeconds(expirationSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(final HttpServletResponse response) {
        final ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
