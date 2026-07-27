package com.daenggo.backend.auth.oauth;

import com.daenggo.backend.user.entity.AuthProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * 카카오 계정과 서비스 계정의 연결을 해제한다.
 */
@Service
@RequiredArgsConstructor
public class KakaoUnlinkService {

    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";
    private static final int KAKAO_ALREADY_UNLINKED_ERROR_CODE = -101;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${auth.oauth2.kakao-admin-key:}")
    private String kakaoAdminKey;

    /**
     * 카카오 회원일 때만 카카오 연결 끊기를 요청한다.
     *
     * @param provider 로그인 제공자
     * @param providerId 카카오 회원 번호
     */
    public void unlinkIfKakao(final AuthProvider provider, final String providerId) {
        if (provider != AuthProvider.KAKAO) {
            return;
        }

        validateKakaoConfiguration(providerId);

        final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("target_id_type", "user_id");
        formData.add("target_id", providerId);

        try {
            final String responseBody = restClient.post()
                    .uri(KAKAO_UNLINK_URL)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoAdminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            validateUnlinkResponse(responseBody, providerId);
        } catch (RestClientResponseException exception) {
            if (isAlreadyUnlinked(exception.getResponseBodyAsString())) {
                return;
            }

            throw kakaoUnlinkFailed(exception);
        } catch (RestClientException exception) {
            throw kakaoUnlinkFailed(exception);
        }
    }

    private void validateKakaoConfiguration(final String providerId) {
        if (kakaoAdminKey == null || kakaoAdminKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "카카오 연결 끊기에 필요한 Admin 키가 설정되지 않았습니다."
            );
        }

        if (providerId == null || providerId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "카카오 회원 번호가 없어 연결을 끊을 수 없습니다."
            );
        }
    }

    private void validateUnlinkResponse(
            final String responseBody,
            final String expectedProviderId
    ) {
        try {
            final JsonNode response = objectMapper.readTree(responseBody);
            final String unlinkedProviderId = response.path("id").asText();

            if (!expectedProviderId.equals(unlinkedProviderId)) {
                throw kakaoUnlinkFailed(null);
            }
        } catch (JsonProcessingException exception) {
            throw kakaoUnlinkFailed(exception);
        }
    }

    private boolean isAlreadyUnlinked(final String responseBody) {
        try {
            return objectMapper.readTree(responseBody).path("code").asInt()
                    == KAKAO_ALREADY_UNLINKED_ERROR_CODE;
        } catch (JsonProcessingException exception) {
            return false;
        }
    }

    private ResponseStatusException kakaoUnlinkFailed(final Throwable cause) {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "카카오 계정 연결 끊기에 실패했습니다.",
                cause
        );
    }
}
