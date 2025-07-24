package gift.service;

import gift.config.KakaoAuthException;
import gift.dto.KakaoTokenResponse;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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

    public KakaoService(
            @Value("${kakao.api.key}") String apiKey,
            @Value("${redirect.url}") String redirectUrl, RestClient kakaoRestClient) {
        this.apiKey = apiKey;
        this.redirectUrl = redirectUrl;
        this.client = kakaoRestClient;
    }


    public String getAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", apiKey);
        body.add("redirect_uri", redirectUrl);
        body.add("code", code);

        try {
            KakaoTokenResponse response = client.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<KakaoTokenResponse>() {
                    });
            return response.accessToken();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new KakaoAuthException("카카오 인증 실패", e, e.getStatusCode());
        } catch (RestClientException e) {
            throw new KakaoAuthException("카카오 API 호출 오류", e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public URI getOauthUri() {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com")
                .path("/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", apiKey)
                .queryParam("redirect_uri", redirectUrl)
                .build().toUri();
    }
}
