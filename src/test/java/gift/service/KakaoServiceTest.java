package gift.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gift.config.KakaoAuthException;
import gift.dto.KakaoTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class KakaoServiceTest {

    @Mock
    private RestClient mockRestClient;

    @InjectMocks
    private KakaoService kakaoService;

    @BeforeEach
    void setUp() {
        kakaoService = new KakaoService("dummyKey", "dummyUrl", mockRestClient);
    }


    @Test
    void 엑세스_토큰_정상_획득() {
        // given
        String code = "dummyCode";

        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        KakaoTokenResponse fakeResponse = new KakaoTokenResponse("mockToken", "refreshToken", 200);

        when(mockRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(bodySpec);
        when(bodySpec.body(any(MultiValueMap.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(fakeResponse);


        // when
        String result = kakaoService.getAccessToken(code);

        // then
        assertEquals("mockToken", result);
    }

    @Test
    void 엑세스_토큰_획득_실패() {
        // given
        String code = "dummyCode";

        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        KakaoTokenResponse fakeResponse = new KakaoTokenResponse("mockToken", "refreshToken", 200);

        when(mockRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(bodySpec);
        when(bodySpec.body(any(MultiValueMap.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        // when & then
        KakaoAuthException exception = assertThrows(KakaoAuthException.class, () ->
                kakaoService.getAccessToken(code));

        assertEquals("카카오 인증 실패", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
    }
}