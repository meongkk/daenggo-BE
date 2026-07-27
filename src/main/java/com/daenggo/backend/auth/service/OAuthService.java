package com.daenggo.backend.auth.service;

import com.daenggo.backend.auth.dto.AuthRequestDto;
import com.daenggo.backend.auth.dto.AuthResponseDto;
import com.daenggo.backend.user.entity.User;
import com.daenggo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * 카카오 로그인 완료와 최초 소셜 회원가입을 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthService {

    private final OAuthFlowTokenService flowTokenService;
    private final UserRepository userRepository;
    private final AuthService authService;

    /**
     * 기존 카카오 회원에게 댕고 Access/Refresh Token을 발급한다.
     */
    @Transactional
    public AuthResponseDto.Token login(final String flowToken) {
        final OAuthFlowTokenService.OAuthLoginClaims claims =
                flowTokenService.verifyLoginToken(flowToken);
        final User user = userRepository
                .findByProviderAndProviderIdAndDeletedAtIsNull(
                        claims.provider(),
                        claims.providerId()
                )
                .orElseThrow(this::invalidOAuthLogin);

        return authService.issueTokenPair(user);
    }

    /**
     * 가입 전용 JWT와 사용자가 정한 닉네임으로 카카오 회원가입을 완료한다.
     */
    @Transactional
    public AuthResponseDto.Token signup(
            final String flowToken,
            final AuthRequestDto.OAuthSignup request
    ) {
        final OAuthFlowTokenService.OAuthSignupClaims claims =
                flowTokenService.verifySignupToken(flowToken);
        final String email = claims.email().strip().toLowerCase(Locale.ROOT);
        final String nickname = request.getNickname().strip();

        if (userRepository
                .findByProviderAndProviderIdAndDeletedAtIsNull(
                        claims.provider(),
                        claims.providerId()
                )
                .isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 가입이 완료된 카카오 계정입니다."
            );
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 가입된 이메일입니다."
            );
        }

        if (userRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 사용 중인 닉네임입니다."
            );
        }

        final User user = User.builder()
                .email(email)
                .password(null)
                .nickname(nickname)
                .image(claims.image())
                .provider(claims.provider())
                .providerId(claims.providerId())
                .build();

        try {
            final User savedUser = userRepository.saveAndFlush(user);
            return authService.issueTokenPair(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 사용 중인 이메일, 닉네임 또는 카카오 계정입니다."
            );
        }
    }

    private ResponseStatusException invalidOAuthLogin() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "가입된 카카오 회원을 찾을 수 없습니다."
        );
    }
}
