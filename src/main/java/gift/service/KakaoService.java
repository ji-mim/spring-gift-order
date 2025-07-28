package gift.service;

import gift.config.JwtTokenProvider;
import gift.config.KakaoAuthException;
import gift.domain.AccountType;
import gift.domain.KakaoToken;
import gift.domain.Member;
import gift.dto.KakaoTokenResponse;
import gift.repository.KakaoTokenJpaRepository;
import gift.repository.MemberJpaRepository;
import gift.util.AesUtil;
import java.net.URI;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class KakaoService {

    private final String apiKey;
    private final String redirectUrl;
    private final RestClient client;
    private final AesUtil aesUtil;
    private final MemberJpaRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoTokenJpaRepository kakaoTokenRepository;

    public KakaoService(
            @Value("${kakao.api.key}") String apiKey,
            @Value("${redirect.url}") String redirectUrl,
            RestClient kakaoRestClient, AesUtil aesUtil, MemberJpaRepository memberRepository,
            JwtTokenProvider jwtTokenProvider, KakaoTokenJpaRepository kakaoTokenRepository
    ) {
        this.apiKey = apiKey;
        this.redirectUrl = redirectUrl;
        this.client = kakaoRestClient;
        this.aesUtil = aesUtil;
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.kakaoTokenRepository = kakaoTokenRepository;
    }


    @Transactional
    public String getAccessToken(String code) {

        try {
            KakaoTokenResponse response = requestKakaoToken(code);
            String userEmail = extractEmailOrThrow(response);

            Member member = new Member(null, userEmail, null, null, AccountType.KAKAO);
            memberRepository.save(member);
            kakaoTokenRepository.save(new KakaoToken(null, member, aesUtil.encrypt(response.access_token()), aesUtil.encrypt(response.refresh_token()), LocalDateTime.now().plusSeconds(response.expires_in())));

            return jwtTokenProvider.createToken(userEmail);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new KakaoAuthException("카카오 인증 실패", e, e.getStatusCode());
        } catch (RestClientException e) {
            throw new KakaoAuthException("카카오 API 호출 오류", e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private KakaoTokenResponse requestKakaoToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", apiKey);
        body.add("redirect_uri", redirectUrl);
        body.add("code", code);
        KakaoTokenResponse response = client.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<KakaoTokenResponse>() {
                });
        return response;
    }

    private String extractEmailOrThrow(KakaoTokenResponse response) {
        String userEmail = jwtTokenProvider.extractEmail(response.id_token());
        if (userEmail == null) {
            throw new IllegalArgumentException("Email 값을 얻을 수 없습니다.");
        }
        return userEmail;
    }

    public URI getOauthUri() {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com")
                .path("/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", apiKey)
                .queryParam("redirect_uri", redirectUrl)
                .queryParam("scope", "account_email openid")
                .build().toUri();
    }
}
