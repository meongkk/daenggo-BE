package com.daenggo.backend.auth.service;

import com.daenggo.backend.auth.oauth.KakaoOAuth2UserInfo;
import com.daenggo.backend.user.entity.AuthProvider;
import com.daenggo.backend.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * OAuth2 리다이렉트와 프론트 API 호출 사이에서만 사용하는 단기 JWT 서비스.
 *
 * <p>일반 API Access Token과 검증기를 분리하여 가입 토큰으로 보호 API를 호출할 수 없게 한다.</p>
 */
@Service
public class OAuthFlowTokenService {

    private static final String SIGNUP_TOKEN_TYPE = "oauth_signup";
    private static final String LOGIN_TOKEN_TYPE = "oauth_login";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder flowTokenDecoder;
    private final Clock clock;
    private final String issuer;
    private final long expirationSeconds;

    public OAuthFlowTokenService(
            final JwtEncoder jwtEncoder,
            final SecretKey secretKey,
            final Clock clock,
            @Value("${auth.jwt.issuer}") final String issuer,
            @Value("${auth.oauth2.flow-token-expiration-seconds}") final long expirationSeconds
    ) {
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("OAuth 흐름 토큰 만료 시간은 0보다 커야 합니다.");
        }

        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;

        final NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer)
        ));
        this.flowTokenDecoder = decoder;
    }

    public String issueSignupToken(final KakaoOAuth2UserInfo userInfo) {
        return issue(
                SIGNUP_TOKEN_TYPE,
                AuthProvider.KAKAO,
                userInfo.providerId(),
                userInfo.email(),
                userInfo.image()
        );
    }

    public String issueLoginToken(final User user) {
        return issue(
                LOGIN_TOKEN_TYPE,
                user.getProvider(),
                user.getProviderId(),
                null,
                null
        );
    }

    public OAuthSignupClaims verifySignupToken(final String token) {
        final Jwt jwt = decode(token, SIGNUP_TOKEN_TYPE);
        final AuthProvider provider = provider(jwt);
        final String providerId = requiredClaim(jwt, "providerId");
        final String email = requiredClaim(jwt, "email");

        return new OAuthSignupClaims(
                provider,
                providerId,
                email,
                jwt.getClaimAsString("image")
        );
    }

    public OAuthLoginClaims verifyLoginToken(final String token) {
        final Jwt jwt = decode(token, LOGIN_TOKEN_TYPE);
        return new OAuthLoginClaims(
                provider(jwt),
                requiredClaim(jwt, "providerId")
        );
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String issue(
            final String tokenType,
            final AuthProvider provider,
            final String providerId,
            final String email,
            final String image
    ) {
        if (provider == null || !StringUtils.hasText(providerId)) {
            throw new IllegalArgumentException("소셜 로그인 제공자 정보가 필요합니다.");
        }

        final Instant issuedAt = clock.instant();
        final JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(expirationSeconds))
                .subject(provider.name() + ":" + providerId)
                .id(UUID.randomUUID().toString())
                .claim("type", tokenType)
                .claim("provider", provider.name())
                .claim("providerId", providerId);

        if (StringUtils.hasText(email)) {
            claims.claim("email", email);
        }
        if (StringUtils.hasText(image)) {
            claims.claim("image", image);
        }

        final JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims.build())
        ).getTokenValue();
    }

    private Jwt decode(final String token, final String expectedType) {
        if (!StringUtils.hasText(token)) {
            throw invalidToken();
        }

        try {
            final Jwt jwt = flowTokenDecoder.decode(token);
            if (!expectedType.equals(jwt.getClaimAsString("type"))) {
                throw invalidToken();
            }
            return jwt;
        } catch (JwtException exception) {
            throw invalidToken();
        }
    }

    private AuthProvider provider(final Jwt jwt) {
        try {
            return AuthProvider.valueOf(requiredClaim(jwt, "provider"));
        } catch (IllegalArgumentException exception) {
            throw invalidToken();
        }
    }

    private String requiredClaim(final Jwt jwt, final String claimName) {
        final String value = jwt.getClaimAsString(claimName);
        if (!StringUtils.hasText(value)) {
            throw invalidToken();
        }
        return value;
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "카카오 로그인 정보가 만료되었거나 유효하지 않습니다."
        );
    }

    public record OAuthSignupClaims(
            AuthProvider provider,
            String providerId,
            String email,
            String image
    ) {
    }

    public record OAuthLoginClaims(
            AuthProvider provider,
            String providerId
    ) {
    }
}
