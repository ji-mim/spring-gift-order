package gift.controller;

import gift.service.KakaoService;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KakaoController {

    private final KakaoService service;

    @Value("${kakao.api.key}")
    private String apiKey;

    @Value("${redirect.url}")
    private String redirectUrl;

    public KakaoController(KakaoService service) {
        this.service = service;
    }

    @GetMapping("/oauth/login")
    public ResponseEntity<Void> redirectToKakao() {
        URI uri = URI.create("https://kauth.kakao.com/oauth/authorize" +
                "?response_type=code" +
                "&client_id=" + apiKey +
                "&redirect_uri="+ redirectUrl);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(uri)
                .build();
    }

    @GetMapping
    public ResponseEntity<Void> getAccessToken(@RequestParam("code") String code) {
        service.getAccessToken(code);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
