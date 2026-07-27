package com.daenggo.backend.auth.controller;

import com.daenggo.backend.auth.dto.AuthRequestDto;
import com.daenggo.backend.auth.dto.AuthResponseDto;
import com.daenggo.backend.auth.service.AuthService;
import com.daenggo.backend.auth.service.OAuthFlowCookieService;
import com.daenggo.backend.auth.service.OAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 REST 컨트롤러
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthService oauthService;
    private final OAuthFlowCookieService oauthFlowCookieService;

    /**
     * 로컬 회원가입
     *
     * @param request 회원가입 요청
     * @return 가입 회원 정보 응답
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDto.Signup> signup(
            @Valid @RequestBody final AuthRequestDto.Signup request
    ) {
        final AuthResponseDto.Signup response = authService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 이메일 사용 가능 여부 확인
     *
     * @param email 확인 이메일
     * @return 이메일 사용 가능 여부 응답
     */
    @GetMapping("/check-email")
    public ResponseEntity<AuthResponseDto.Availability> checkEmail(
            @RequestParam
            @NotBlank
            @Email
            @Size(max = 100)
            final String email
    ) {
        return ResponseEntity.ok(authService.checkEmail(email));
    }

    /**
     * 닉네임 사용 가능 여부 확인
     *
     * @param nickname 확인 닉네임
     * @return 닉네임 사용 가능 여부 응답
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<AuthResponseDto.Availability> checkNickname(
            @RequestParam
            @NotBlank
            @Size(max = 50)
            final String nickname
    ) {
        return ResponseEntity.ok(authService.checkNickname(nickname));
    }

    /**
     * 로컬 로그인과 토큰 발급
     *
     * @param request 로그인 요청
     * @return Access Token과 Refresh Token 응답
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto.Token> login(
            @Valid @RequestBody final AuthRequestDto.Login request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * 기존 카카오 회원의 OAuth 흐름 Cookie를 댕고 토큰으로 교환한다.
     *
     * @param flowToken OAuth 로그인 완료 Cookie
     * @param response Cookie 삭제를 위한 HTTP 응답
     * @return Access Token과 Refresh Token
     */
    @PostMapping("/oauth/token")
    public ResponseEntity<AuthResponseDto.Token> oauthToken(
            @CookieValue(
                    name = OAuthFlowCookieService.COOKIE_NAME,
                    required = false
            )
            final String flowToken,
            final HttpServletResponse response
    ) {
        final AuthResponseDto.Token token = oauthService.login(flowToken);
        oauthFlowCookieService.clear(response);
        return ResponseEntity.ok(token);
    }

    /**
     * 최초 카카오 로그인 회원이 닉네임을 정해 가입을 완료한다.
     *
     * @param flowToken OAuth 가입 대기 Cookie
     * @param request 사용자가 정한 닉네임
     * @param response Cookie 삭제를 위한 HTTP 응답
     * @return 신규 회원의 Access Token과 Refresh Token
     */
    @PostMapping("/oauth/signup")
    public ResponseEntity<AuthResponseDto.Token> oauthSignup(
            @CookieValue(
                    name = OAuthFlowCookieService.COOKIE_NAME,
                    required = false
            )
            final String flowToken,
            @Valid @RequestBody final AuthRequestDto.OAuthSignup request,
            final HttpServletResponse response
    ) {
        final AuthResponseDto.Token token = oauthService.signup(flowToken, request);
        oauthFlowCookieService.clear(response);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(token);
    }

    /**
     * Access Token과 Refresh Token 재발급
     *
     * @param request 토큰 재발급 요청
     * @return 회전된 토큰 응답
     */
    @PostMapping("/reissue")
    public ResponseEntity<AuthResponseDto.Token> reissue(
            @Valid @RequestBody final AuthRequestDto.Refresh request
    ) {
        return ResponseEntity.ok(authService.reissue(request));
    }

    /**
     * 로그인 회원 Refresh Token 폐기
     *
     * @param authentication 로그인 회원 인증 정보
     * @param request 로그아웃 요청
     * @return 응답 본문 없는 성공 응답
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            final Authentication authentication,
            @Valid @RequestBody final AuthRequestDto.Refresh request
    ) {
        authService.logout(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}

// 탈퇴 처리를 하고 탈퇴 날짜를 입력해서 탈퇴날짜가 널이 아니면 탈퇴인 것을 확인
// 탈퇴한 이메일로 다시 로그인 할 때 기존에 남아 있는 데이터를 덮어 씌울 것인가
