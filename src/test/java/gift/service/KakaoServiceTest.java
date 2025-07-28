package gift.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gift.config.JwtTokenProvider;
import gift.config.KakaoAuthException;
import gift.domain.KakaoToken;
import gift.domain.Member;
import gift.dto.KakaoTokenResponse;
import gift.repository.KakaoTokenJpaRepository;
import gift.repository.MemberJpaRepository;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
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
    @Mock
    private MemberJpaRepository memberRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private KakaoTokenJpaRepository kakaoTokenRepository;

    @InjectMocks
    private KakaoService kakaoService;

    @BeforeEach
    void setUp() {
        kakaoService = new KakaoService("dummyKey", "dummyUrl", mockRestClient, memberRepository, jwtTokenProvider, kakaoTokenRepository);
    }


    @Test
    void jwt_토큰_정상_획득() {
        // given
        String code = "dummyCode";

        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        KakaoTokenResponse fakeResponse = new KakaoTokenResponse("mockToken", "refreshToken", "null", 200);
        Member member = new Member(1L, "email", null, null);
        KakaoToken kakaoToken = new KakaoToken(1L, member, "mockToken", "refreshToken", LocalDateTime.now().plusSeconds(200));

        // when
        when(mockRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(bodySpec);
        when(bodySpec.body(any(MultiValueMap.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(fakeResponse);

        when(memberRepository.save(any())).thenReturn(member);
        when(kakaoTokenRepository.save(any())).thenReturn(kakaoToken);

        when(jwtTokenProvider.extractEmail(fakeResponse.id_token())).thenReturn("email");
        when(jwtTokenProvider.createToken("email")).thenReturn("jwtToken");

        String result = kakaoService.getAccessToken(code);

        // then
        assertEquals("jwtToken", result);
    }

    @Test
    void 엑세스_토큰_획득_실패_인가_코드_불일치() {
        // given
        String code = "dummyCode";

        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        //when
        when(mockRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(bodySpec);
        when(bodySpec.body(any(MultiValueMap.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        //then
        KakaoAuthException exception = assertThrows(KakaoAuthException.class, () ->
                kakaoService.getAccessToken("notMatchCode"));

        assertEquals("카카오 인증 실패", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
    }

    @Test
    void 엑세스_토큰_획득_실패_email_추출_실패() {
        // given
        String code = "dummyCode";

        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        KakaoTokenResponse fakeResponse = new KakaoTokenResponse("mockToken", "refreshToken", "null", 200);
        Member member = new Member(1L, "email", null, null);
        KakaoToken kakaoToken = new KakaoToken(1L, member, "mockToken", "refreshToken", LocalDateTime.now().plusSeconds(200));

        // when
        when(mockRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(bodySpec);
        when(bodySpec.body(any(MultiValueMap.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(fakeResponse);
        when(jwtTokenProvider.extractEmail(fakeResponse.id_token())).thenThrow(new IllegalArgumentException());

        // then
        Assertions.assertThatIllegalArgumentException().isThrownBy(() -> kakaoService.getAccessToken(code));
    }


}