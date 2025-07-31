package gift.service;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import gift.domain.AccountType;
import gift.domain.KakaoToken;
import gift.domain.Member;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OptionJpaRepository optionRepository;
    @Mock
    private ProductJpaRepository productRepository;
    @Mock
    private WishJpaRepository wishRepository;
    @Mock
    private KakaoTokenJpaRepository kakaoTokenRepository;
    @Mock
    private KakaoService kakaoService;
    @Mock
    private AesUtil aesUtil;
    @Mock
    private OrdersJpaRepository ordersRepository;
    @InjectMocks
    OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(optionRepository, productRepository, wishRepository, kakaoTokenRepository, ordersRepository, kakaoService, aesUtil);
    }


    @Test
    void 주문_등록_성공() {
        Product product = new Product(1L, "productName", 1000, "url");
        Option option = new Option(1L, "optionName", 10, product);
        Member member = new Member(1L, "email", null, null, AccountType.KAKAO);
        Wish wish = new Wish(1L, member, product, 1);
        KakaoToken kakaoToken = new KakaoToken(1L, member, "accessToken", "refreshToken", LocalDateTime.now());
        Orders savedOrder = new Orders(1L, "message", 1, option);
        OrdersRequest ordersRequest = new OrdersRequest(option.getId(), savedOrder.getQuantity(), savedOrder.getMessage());

        when(optionRepository.findById(option.getId())).thenReturn(Optional.of(option));
        when(productRepository.findByOptions(List.of(option))).thenReturn(List.of(product));
        when(wishRepository.findByMemberIdAndProductId(member.getId(), product.getId())).thenReturn(Optional.of(wish));
        when(kakaoTokenRepository.findByMemberId(member.getId())).thenReturn(Optional.of(kakaoToken));
        when(aesUtil.decrypt(any())).thenReturn("decryptToken");
        when(ordersRepository.save(any())).thenReturn(savedOrder);

        //when
        OrdersResponse response = orderService.createOrder(member.getId(), ordersRequest);

        //then
        assertEquals(savedOrder.getId(), response.id());
        assertEquals(option.getId(), response.optionId());
        assertEquals(ordersRequest.quantity(), response.quantity());
        assertEquals(ordersRequest.message(), response.message());
    }

    @Test
    void 주문_등록_옵션_조회_실패() {
        Product product = new Product(1L, "productName", 1000, "url");
        Option option = new Option(1L, "optionName", 10, product);
        Member member = new Member(1L, "email", null, null, AccountType.KAKAO);
        Wish wish = new Wish(1L, member, product, 1);
        KakaoToken kakaoToken = new KakaoToken(1L, member, "accessToken", "refreshToken", LocalDateTime.now());
        Orders savedOrder = new Orders(1L, "message", 1, option);
        OrdersRequest ordersRequest = new OrdersRequest(option.getId(), savedOrder.getQuantity(), savedOrder.getMessage());

        when(optionRepository.findById(option.getId())).thenThrow(new IllegalArgumentException());

        //then
        assertThatIllegalArgumentException().isThrownBy(() -> orderService.createOrder(member.getId(), ordersRequest));
    }


    @Test
    void 주문_등록_토큰_조회_실패() {
        Product product = new Product(1L, "productName", 1000, "url");
        Option option = new Option(1L, "optionName", 10, product);
        Member member = new Member(1L, "email", null, null, AccountType.KAKAO);
        Wish wish = new Wish(1L, member, product, 1);
        KakaoToken kakaoToken = new KakaoToken(1L, member, "accessToken", "refreshToken", LocalDateTime.now());
        Orders savedOrder = new Orders(1L, "message", 1, option);
        OrdersRequest ordersRequest = new OrdersRequest(option.getId(), savedOrder.getQuantity(), savedOrder.getMessage());

        when(optionRepository.findById(option.getId())).thenReturn(Optional.of(option));
        when(productRepository.findByOptions(List.of(option))).thenReturn(List.of(product));
        when(wishRepository.findByMemberIdAndProductId(member.getId(), product.getId())).thenReturn(Optional.of(wish));
        when(kakaoTokenRepository.findByMemberId(member.getId())).thenThrow(new NoSuchElementException());

        //then
        assertThatThrownBy(() -> orderService.createOrder(member.getId(), ordersRequest));
    }

}