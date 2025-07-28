package gift.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gift.domain.KakaoToken;
import gift.domain.Option;
import gift.domain.Orders;
import gift.domain.Product;
import gift.domain.Wish;
import gift.dto.KakaoTextTemplate;
import gift.dto.OrdersResponse;
import gift.dto.RenewKakaoToken;
import gift.repository.KakaoTokenJpaRepository;
import gift.repository.OptionJpaRepository;
import gift.repository.OrdersJpaRepository;
import gift.repository.ProductJpaRepository;
import gift.repository.WishJpaRepository;
import gift.util.AesUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class OrderService {

    private final OptionJpaRepository optionRepository;
    private final ProductJpaRepository productRepository;
    private final WishJpaRepository wishRepository;
    private final KakaoTokenJpaRepository kakaoTokenRepository;
    private final OrdersJpaRepository ordersRepository;
    private final AesUtil aesUtil;
    private final RestClient client;
    private final String apiKey;


    public OrderService(OptionJpaRepository optionRepository,
            ProductJpaRepository productRepository, WishJpaRepository wishRepository,
            KakaoTokenJpaRepository kakaoTokenRepository, OrdersJpaRepository ordersRepository,
            AesUtil aesUtil, RestClient client, @Value("${kakao.api.key}") String apiKey) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
        this.wishRepository = wishRepository;
        this.kakaoTokenRepository = kakaoTokenRepository;
        this.ordersRepository = ordersRepository;
        this.aesUtil = aesUtil;
        this.apiKey = apiKey;
        this.client = client;
    }

    @Transactional
    public OrdersResponse createOrder(Long memberId, Long optionId, int quantity, String message) {
        Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 상품입니다."));
        option.subtractQuantity(quantity);

        List<Product> productList = productRepository.findByOptions(List.of(option));
        for (Product product : productList) {
            Optional<Wish> wishByProduct = wishRepository.findByMemberIdAndProductId(memberId,product.getId());
            wishByProduct.ifPresent(wishRepository::delete);
        }
        KakaoToken kakaoToken = kakaoTokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new NoSuchElementException("조회할 수 없는 토큰입니다"));

        renewKakaToken(kakaoToken);
        String accessToken = aesUtil.decrypt(kakaoToken.getAccessToken());
        sendMessageToMe(message, accessToken);

        Orders savedOrder = ordersRepository.save(new Orders(null, message, quantity, option));

        return new OrdersResponse(savedOrder.getId(), optionId, quantity, LocalDateTime.now(), message);
    }

    private void sendMessageToMe(String message, String accessToken) {
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

    private void renewKakaToken(KakaoToken kakaoToken) {
        if (isExpired(kakaoToken)) {

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", apiKey);
            body.add("redirect_uri", kakaoToken.getRefreshToken());

            RenewKakaoToken renewKakaoToken = client.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<RenewKakaoToken>() {
                    });

            if (renewKakaoToken.access_token() != null && renewKakaoToken.refresh_token() != null ) {
                kakaoToken.renewAccessToken(aesUtil.encrypt(renewKakaoToken.access_token()));
                kakaoToken.renewRefreshToken(aesUtil.encrypt(renewKakaoToken.refresh_token()));
                kakaoToken.renewAccessTokenExpiresAt(LocalDateTime.now().plusSeconds(renewKakaoToken.expires_in()));
            }
        }
    }

    private static boolean isExpired(KakaoToken kakaoToken) {
        return LocalDateTime.now().isAfter(kakaoToken.getAccessTokenExpiresAt());
    }


}
