package gift.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.dto.CreateMemberRequest;
import gift.dto.CreateMemberResponse;
import gift.dto.CreateOptionRequest;
import gift.dto.CreateProductRequest;
import gift.dto.CreateProductResponse;
import gift.dto.CreateWishRequest;
import gift.dto.CreateWishResponse;
import gift.dto.LoginMemberRequest;
import gift.dto.LoginMemberResponse;
import gift.dto.UpdateWishRequest;
import gift.repository.MemberJpaRepository;
import gift.repository.WishJpaRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WishRestControllerTest {

    @Autowired
    private MemberJpaRepository memberRepository;
    @Autowired
    private WishJpaRepository wishRepository;

    @LocalServerPort
    private int port;
    private RestClient client = RestClient.create();

    @BeforeEach
    void celar() {
        wishRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("로그인 멤버 상품 조회")
    void getProducts() {
        String url = "http://localhost:" + port + "/api/members/register";
        ResponseEntity<CreateMemberResponse> registerResponse = client.post()
                .uri(url)
                .body(new CreateMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(CreateMemberResponse.class);

        url = "http://localhost:" + port + "/api/members/login";
        ResponseEntity<LoginMemberResponse> loginResponse = client.post()
                .uri(url)
                .body(new LoginMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(LoginMemberResponse.class);

        String body = loginResponse.getBody().toString();
        int start = body.indexOf("token=") + "token=".length();
        int end = body.indexOf("]", start);
        String token = body.substring(start, end);

        url = "http://localhost:" + port + "/api/wishes/products";
        ResponseEntity<Void> response = client.get()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("위시 리스트에 상품 추가")
    void 위시리스트_상품_추가() {
        //상품 추가
        String url = "http://localhost:" + port + "/api/products";
        ResponseEntity<CreateProductResponse> productResponse = client.post()
                .uri(url)
                .body(new CreateProductRequest("product1", 1000, "exam.url", List.of(new CreateOptionRequest("옵션1", 10))))
                .retrieve()
                .toEntity(CreateProductResponse.class);
        assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        url = "http://localhost:" + port + "/api/members/register";

        // 회원가입 및 로그인
        ResponseEntity<CreateMemberResponse> registerResponse = client.post()
                .uri(url)
                .body(new CreateMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(CreateMemberResponse.class);

        url = "http://localhost:" + port + "/api/members/login";
        ResponseEntity<LoginMemberResponse> loginResponse = client.post()
                .uri(url)
                .body(new LoginMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(LoginMemberResponse.class);

        String body = loginResponse.getBody().toString();
        int start = body.indexOf("token=") + "token=".length();
        int end = body.indexOf("]", start);
        String token = body.substring(start, end);


        //위시 리스트에 상품 추가
        url = "http://localhost:" + port + "/api/wishes";
        ResponseEntity<Void> response = client.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .body(new CreateWishRequest(productResponse.getBody().id(), 1))
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("위시 리스트에 상품 추가 실패(수량 음수)")
    void 위시리스트_상품_추가_실패_수량() {
        //상품 추가
        String url = "http://localhost:" + port + "/api/products";
        ResponseEntity<CreateProductResponse> productResponse = client.post()
                .uri(url)
                .body(new CreateProductRequest("product1", 1000, "exam.url", List.of(new CreateOptionRequest("옵션1", 10))))
                .retrieve()
                .toEntity(CreateProductResponse.class);
        assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        url = "http://localhost:" + port + "/api/members/register";

        // 회원가입 및 로그인
        ResponseEntity<CreateMemberResponse> registerResponse = client.post()
                .uri(url)
                .body(new CreateMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(CreateMemberResponse.class);

        url = "http://localhost:" + port + "/api/members/login";
        ResponseEntity<LoginMemberResponse> loginResponse = client.post()
                .uri(url)
                .body(new LoginMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(LoginMemberResponse.class);

        String body = loginResponse.getBody().toString();
        int start = body.indexOf("token=") + "token=".length();
        int end = body.indexOf("]", start);
        String token = body.substring(start, end);


        assertThatThrownBy(() ->
                client.post()
                        .uri("http://localhost:" + port + "/api/wishes")
                        .header("Authorization", "Bearer " + token)
                        .body(new CreateWishRequest(productResponse.getBody().id(), -10))
                        .retrieve()
                        .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.BadRequest.class);

    }

    @Test
    @DisplayName("위시 리스트에 등록된 상품 조회 성공")
    void 위시리스트에_등록된_상품_조회() {
        //상품 추가
        String url = "http://localhost:" + port + "/api/products";
        ResponseEntity<CreateProductResponse> productResponse = client.post()
                .uri(url)
                .body(new CreateProductRequest("product1", 1000, "exam.url", List.of(new CreateOptionRequest("옵션1", 10))))
                .retrieve()
                .toEntity(CreateProductResponse.class);
        assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        url = "http://localhost:" + port + "/api/members/register";

        // 회원가입 및 로그인
        ResponseEntity<CreateMemberResponse> registerResponse = client.post()
                .uri(url)
                .body(new CreateMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(CreateMemberResponse.class);

        url = "http://localhost:" + port + "/api/members/login";
        ResponseEntity<LoginMemberResponse> loginResponse = client.post()
                .uri(url)
                .body(new LoginMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(LoginMemberResponse.class);

        String body = loginResponse.getBody().toString();
        int start = body.indexOf("token=") + "token=".length();
        int end = body.indexOf("]", start);
        String token = body.substring(start, end);


        //위시 리스트에 등록된 상품 조회
        url = "http://localhost:" + port + "/api/wishes";
        ResponseEntity<Void> response = client.get()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    @DisplayName("위시 리스트에 등록된 상품 삭제 성공")
    void 위시리스트에_등록된_상품_삭제() {
        //상품 추가
        String url = "http://localhost:" + port + "/api/products";
        ResponseEntity<CreateProductResponse> productResponse = client.post()
                .uri(url)
                .body(new CreateProductRequest("product1", 1000, "exam.url", List.of(new CreateOptionRequest("옵션1", 10))))
                .retrieve()
                .toEntity(CreateProductResponse.class);
        assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        url = "http://localhost:" + port + "/api/members/register";

        // 회원가입 및 로그인
        ResponseEntity<CreateMemberResponse> registerResponse = client.post()
                .uri(url)
                .body(new CreateMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(CreateMemberResponse.class);

        url = "http://localhost:" + port + "/api/members/login";
        ResponseEntity<LoginMemberResponse> loginResponse = client.post()
                .uri(url)
                .body(new LoginMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(LoginMemberResponse.class);

        String body = loginResponse.getBody().toString();
        int start = body.indexOf("token=") + "token=".length();
        int end = body.indexOf("]", start);
        String token = body.substring(start, end);

        url = "http://localhost:" + port + "/api/wishes";
        ResponseEntity<CreateWishResponse> addWishResponse = client.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .body(new CreateWishRequest(productResponse.getBody().id(), 1))
                .retrieve()
                .toEntity(CreateWishResponse.class);

        url = "http://localhost:" + port + "/api/wishes/{wishId}";
        ResponseEntity<Void> response = client.delete()
                .uri(url, addWishResponse.getBody().id())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    @DisplayName("위시 리스트에 등록된 상품 삭제 실패(조회하는 위시리스트 상품이 없음)")
    void 위시리스트에_등록된_상품_삭제_실패_존재하지않는_위시상품() {
        //상품 추가
        String url = "http://localhost:" + port + "/api/products";
        ResponseEntity<CreateProductResponse> productResponse = client.post()
                .uri(url)
                .body(new CreateProductRequest("product1", 1000, "exam.url", List.of(new CreateOptionRequest("옵션1", 10))))
                .retrieve()
                .toEntity(CreateProductResponse.class);
        assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        url = "http://localhost:" + port + "/api/members/register";

        // 회원가입 및 로그인
        ResponseEntity<CreateMemberResponse> registerResponse = client.post()
                .uri(url)
                .body(new CreateMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(CreateMemberResponse.class);

        url = "http://localhost:" + port + "/api/members/login";
        ResponseEntity<LoginMemberResponse> loginResponse = client.post()
                .uri(url)
                .body(new LoginMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(LoginMemberResponse.class);

        String body = loginResponse.getBody().toString();
        int start = body.indexOf("token=") + "token=".length();
        int end = body.indexOf("]", start);
        String token = body.substring(start, end);

        url = "http://localhost:" + port + "/api/wishes";
        ResponseEntity<CreateWishResponse> addWishResponse = client.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .body(new CreateWishRequest(productResponse.getBody().id(), 1))
                .retrieve()
                .toEntity(CreateWishResponse.class);

        url = "http://localhost:" + port + "/api/wishes/{wishId}";
        ResponseEntity<Void> response = client.delete()
                .uri(url, addWishResponse.getBody().id())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();

        assertThatThrownBy(() ->
                client.delete()
                        .uri("http://localhost:" + port + "/api/wishes/{wishId}", addWishResponse.getBody().id())
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    @Test
    @DisplayName("위시 리스트에 등록된 상품 수량 수정 성공")
    void 위시리스트에_등록된_상품_수정() {
        //상품 추가
        String url = "http://localhost:" + port + "/api/products";
        ResponseEntity<CreateProductResponse> productResponse = client.post()
                .uri(url)
                .body(new CreateProductRequest("product1", 1000, "exam.url", List.of(new CreateOptionRequest("옵션1", 10))))
                .retrieve()
                .toEntity(CreateProductResponse.class);
        assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        url = "http://localhost:" + port + "/api/members/register";

        // 회원가입 및 로그인
        ResponseEntity<CreateMemberResponse> registerResponse = client.post()
                .uri(url)
                .body(new CreateMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(CreateMemberResponse.class);

        url = "http://localhost:" + port + "/api/members/login";
        ResponseEntity<LoginMemberResponse> loginResponse = client.post()
                .uri(url)
                .body(new LoginMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(LoginMemberResponse.class);

        String body = loginResponse.getBody().toString();
        int start = body.indexOf("token=") + "token=".length();
        int end = body.indexOf("]", start);
        String token = body.substring(start, end);

        url = "http://localhost:" + port + "/api/wishes";
        ResponseEntity<CreateWishResponse> addWishResponse = client.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .body(new CreateWishRequest(productResponse.getBody().id(), 1))
                .retrieve()
                .toEntity(CreateWishResponse.class);

        url = "http://localhost:" + port + "/api/wishes/{wishId}";
        ResponseEntity<Void> response = client.patch()
                .uri(url, addWishResponse.getBody().id())
                .header("Authorization", "Bearer " + token)
                .body(new UpdateWishRequest(5))
                .retrieve()
                .toBodilessEntity();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }


    @Test
    @DisplayName("위시 리스트에 등록된 상품 수량 수정 실패(수량 음수)")
    void 위시리스트에_등록된_상품_수정_실패_수량() {
        //상품 추가
        String url = "http://localhost:" + port + "/api/products";
        ResponseEntity<CreateProductResponse> productResponse = client.post()
                .uri(url)
                .body(new CreateProductRequest("product1", 1000, "exam.url", List.of(new CreateOptionRequest("옵션1", 10))))
                .retrieve()
                .toEntity(CreateProductResponse.class);
        assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        url = "http://localhost:" + port + "/api/members/register";

        // 회원가입 및 로그인
        ResponseEntity<CreateMemberResponse> registerResponse = client.post()
                .uri(url)
                .body(new CreateMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(CreateMemberResponse.class);

        url = "http://localhost:" + port + "/api/members/login";
        ResponseEntity<LoginMemberResponse> loginResponse = client.post()
                .uri(url)
                .body(new LoginMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(LoginMemberResponse.class);

        String body = loginResponse.getBody().toString();
        int start = body.indexOf("token=") + "token=".length();
        int end = body.indexOf("]", start);
        String token = body.substring(start, end);

        url = "http://localhost:" + port + "/api/wishes";
        ResponseEntity<CreateWishResponse> addWishResponse = client.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .body(new CreateWishRequest(productResponse.getBody().id(), 1))
                .retrieve()
                .toEntity(CreateWishResponse.class);

        assertThatThrownBy(() ->
                client.patch()
                        .uri("http://localhost:" + port + "/api/wishes/{wishId}", addWishResponse.getBody().id())
                        .header("Authorization", "Bearer " + token)
                        .body(new UpdateWishRequest(-10))
                        .retrieve()
                        .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    @DisplayName("위시 리스트에 등록된 상품 수량 수정 실패(없는 위시 상품)")
    void 위시리스트에_등록된_상품_수정_실패_상품부재() {
        //상품 추가
        String url = "http://localhost:" + port + "/api/products";
        ResponseEntity<CreateProductResponse> productResponse = client.post()
                .uri(url)
                .body(new CreateProductRequest("product1", 1000, "exam.url", List.of(new CreateOptionRequest("옵션1", 10))))
                .retrieve()
                .toEntity(CreateProductResponse.class);
        assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        url = "http://localhost:" + port + "/api/members/register";

        // 회원가입 및 로그인
        ResponseEntity<CreateMemberResponse> registerResponse = client.post()
                .uri(url)
                .body(new CreateMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(CreateMemberResponse.class);

        url = "http://localhost:" + port + "/api/members/login";
        ResponseEntity<LoginMemberResponse> loginResponse = client.post()
                .uri(url)
                .body(new LoginMemberRequest("test@exam.com", "1234"))
                .retrieve()
                .toEntity(LoginMemberResponse.class);

        String body = loginResponse.getBody().toString();
        int start = body.indexOf("token=") + "token=".length();
        int end = body.indexOf("]", start);
        String token = body.substring(start, end);

        url = "http://localhost:" + port + "/api/wishes";
        ResponseEntity<CreateWishResponse> addWishResponse = client.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .body(new CreateWishRequest(productResponse.getBody().id(), 1))
                .retrieve()
                .toEntity(CreateWishResponse.class);

        assertThatThrownBy(() ->
                client.patch()
                        .uri("http://localhost:" + port + "/api/wishes/{wishId}", addWishResponse.getBody().id()-500)
                        .header("Authorization", "Bearer " + token)
                        .body(new UpdateWishRequest(10))
                        .retrieve()
                        .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.NotFound.class);
    }
}