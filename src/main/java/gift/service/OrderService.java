package gift.service;

import gift.domain.KakaoToken;
import gift.domain.Option;
import gift.domain.Orders;
import gift.domain.Product;
import gift.domain.Wish;
import gift.dto.OrdersRequest;
import gift.dto.OrdersResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OptionJpaRepository optionRepository;
    private final ProductJpaRepository productRepository;
    private final WishJpaRepository wishRepository;
    private final KakaoTokenJpaRepository kakaoTokenRepository;
    private final OrdersJpaRepository ordersRepository;
    private final KakaoService kakaoService;
    private final AesUtil aesUtil;


    public OrderService(OptionJpaRepository optionRepository,
            ProductJpaRepository productRepository, WishJpaRepository wishRepository,
            KakaoTokenJpaRepository kakaoTokenRepository, OrdersJpaRepository ordersRepository,
            KakaoService kakaoService,
            AesUtil aesUtil) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
        this.wishRepository = wishRepository;
        this.kakaoTokenRepository = kakaoTokenRepository;
        this.ordersRepository = ordersRepository;
        this.kakaoService = kakaoService;
        this.aesUtil = aesUtil;
    }

    @Transactional
    public OrdersResponse createOrder(Long memberId, OrdersRequest request) {
        Option option = optionRepository.findById(request.optionId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 상품입니다."));
        option.subtractQuantity(request.quantity());

        List<Product> productList = productRepository.findByOptions(List.of(option));
        for (Product product : productList) {
            Optional<Wish> wishByProduct = wishRepository.findByMemberIdAndProductId(memberId,product.getId());
            wishByProduct.ifPresent(wishRepository::delete);
        }
        KakaoToken kakaoToken = kakaoTokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new NoSuchElementException("조회할 수 없는 토큰입니다"));

        kakaoService.renewKakaToken(kakaoToken);
        String accessToken = aesUtil.decrypt(kakaoToken.getAccessToken());
        kakaoService.sendMessageToMe(request.message(), accessToken);

        Orders savedOrder = ordersRepository.save(new Orders(null, request.message(), request.quantity(), option));

        return new OrdersResponse(savedOrder.getId(), request.optionId(), request.quantity(), LocalDateTime.now(), request.message());
    }
}
