package com.daenggo.backend.auth.oauth;

import com.daenggo.backend.auth.service.OAuthFlowCookieService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * 카카오 로그인 실패 사유 코드를 프론트 로그인 화면으로 전달한다.
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final String frontendFailureUrl;
    private final OAuthFlowCookieService cookieService;

    public OAuth2LoginFailureHandler(
            final OAuthFlowCookieService cookieService,
            @Value("${auth.oauth2.frontend-failure-url}") final String frontendFailureUrl
    ) {
        this.cookieService = cookieService;
        this.frontendFailureUrl = frontendFailureUrl;
    }

    @Override
    public void onAuthenticationFailure(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationException exception
    ) throws IOException, ServletException {
        final String errorCode;
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            errorCode = oauthException.getError().getErrorCode();
        } else {
            errorCode = "oauth_login_failed";
        }
        redirect(response, errorCode);
    }

    public void redirect(
            final HttpServletResponse response,
            final String errorCode
    ) throws IOException {
        cookieService.clear(response);
        final String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendFailureUrl)
                .queryParam("oauthError", errorCode)
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}
