package gift.controller;

import gift.dto.OrdersRequest;
import gift.dto.OrdersResponse;
import gift.login.Login;
import gift.login.LoginMember;
import gift.service.OrderService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    @PostMapping("/api/orders")
    public HttpEntity<OrdersResponse> createOrders(@RequestBody OrdersRequest request, @Login
            LoginMember loginMember) {
        OrdersResponse response = orderService.createOrder(loginMember.id(), request.optionId(),
                request.quantity(),
                request.message());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
