package com.daenggo.backend.auth.oauth;

import com.daenggo.backend.auth.service.OAuthFlowCookieService;
import com.daenggo.backend.auth.service.OAuthFlowTokenService;
import com.daenggo.backend.user.entity.AuthProvider;
import com.daenggo.backend.user.entity.User;
import com.daenggo.backend.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

/**
 * 카카오 로그인 성공 후 기존 회원 로그인과 신규 회원 닉네임 입력을 분기한다.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final OAuthFlowTokenService flowTokenService;
    private final OAuthFlowCookieService cookieService;
    private final OAuth2LoginFailureHandler failureHandler;
    private final String frontendLoginSuccessUrl;
    private final String frontendSignupUrl;

    public OAuth2LoginSuccessHandler(
            final UserRepository userRepository,
            final OAuthFlowTokenService flowTokenService,
            final OAuthFlowCookieService cookieService,
            final OAuth2LoginFailureHandler failureHandler,
            @Value("${auth.oauth2.frontend-login-success-url}")
            final String frontendLoginSuccessUrl,
            @Value("${auth.oauth2.frontend-signup-url}")
            final String frontendSignupUrl
    ) {
        this.userRepository = userRepository;
        this.flowTokenService = flowTokenService;
        this.cookieService = cookieService;
        this.failureHandler = failureHandler;
        this.frontendLoginSuccessUrl = frontendLoginSuccessUrl;
        this.frontendSignupUrl = frontendSignupUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof KakaoOAuth2User kakaoUser)) {
            failureHandler.redirect(response, "invalid_oauth_principal");
            return;
        }

        final KakaoOAuth2UserInfo userInfo = kakaoUser.getUserInfo();
        final Optional<User> existingUser = userRepository
                .findByProviderAndProviderIdAndDeletedAtIsNull(
                        AuthProvider.KAKAO,
                        userInfo.providerId()
                );

        if (existingUser.isPresent()) {
            final String flowToken =
                    flowTokenService.issueLoginToken(existingUser.get());
            cookieService.write(response, flowToken);
            response.sendRedirect(withStatus(frontendLoginSuccessUrl, "login_success"));
            return;
        }

        if (userRepository.existsByEmailIgnoreCase(userInfo.email())) {
            failureHandler.redirect(response, "email_already_registered");
            return;
        }

        final String flowToken =
                flowTokenService.issueSignupToken(userInfo);
        cookieService.write(response, flowToken);
        response.sendRedirect(withStatus(frontendSignupUrl, "signup_required"));
    }

    private String withStatus(final String url, final String status) {
        return UriComponentsBuilder.fromUriString(url)
                .queryParam("oauth", status)
                .build()
                .encode()
                .toUriString();
    }
}
