package gift.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gift.config.JwtTokenProvider;
import gift.config.KakaoAuthException;
import gift.domain.AccountType;
import gift.domain.KakaoToken;
import gift.domain.Member;
import gift.dto.KakaoTextTemplate;
import gift.dto.KakaoTokenResponse;
import gift.dto.RenewKakaoToken;
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
            RestClient kakaoRestClient,
            AesUtil aesUtil,
            MemberJpaRepository memberRepository,
            JwtTokenProvider jwtTokenProvider,
            KakaoTokenJpaRepository kakaoTokenRepository
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

            if (memberRepository.findByEmail(userEmail).isPresent()) {
                Member member = memberRepository.findByEmail(userEmail).get();
                renewKakaToken(kakaoTokenRepository.findByMemberId(member.getId()).get());
                return jwtTokenProvider.createToken(userEmail);
            }

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

    public void renewKakaToken(KakaoToken kakaoToken) {
        if (kakaoToken.isExpired()) {

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", apiKey);
            body.add("fresh_token", kakaoToken.getRefreshToken());

            RenewKakaoToken renewKakaoToken = client.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<RenewKakaoToken>() {
                    });

            if (renewKakaoToken.access_token() != null && renewKakaoToken.refresh_token() != null ) {
                kakaoToken.renewAccessToken(aesUtil.encrypt(renewKakaoToken.access_token()), LocalDateTime.now().plusSeconds(renewKakaoToken.expires_in()));
                kakaoToken.renewRefreshToken(aesUtil.encrypt(renewKakaoToken.refresh_token()));
            }
        }
    }

    public void sendMessageToMe(String message, String accessToken) {
        ObjectMapper objectMapper = new ObjectMapper();
        KakaoTextTemplate template = new KakaoTextTemplate(message, "kakao.com");
        String templateJson;

        try {
            templateJson = objectMapper.writeValueAsString(template);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 직렬화 실패", e);
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("template_object", templateJson);

        client.post()
                .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity();
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
