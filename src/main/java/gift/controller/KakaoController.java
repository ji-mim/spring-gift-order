package gift.controller;

import gift.dto.LoginMemberResponse;
import gift.service.KakaoService;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KakaoController {

    private final KakaoService service;

    public KakaoController(KakaoService service) {
        this.service = service;
    }

    @GetMapping("/oauth/login")
    public ResponseEntity<Void> redirectToKakao() {
        URI uri = service.getOauthUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(uri)
                .build();
    }

    @GetMapping
    public ResponseEntity<LoginMemberResponse> getAccessToken(@RequestParam("code") String code) {
        String token = service.getAccessToken(code);
        return new ResponseEntity<>(new LoginMemberResponse(token), HttpStatus.OK);
    }
}
