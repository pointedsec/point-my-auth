package com.pointmyauth.example;

import com.pointmyauth.annotation.AuthorizeEntity;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @AuthorizeEntity(
            ids = {"orderId"},
            includeUser = true,
            authorizationHandler = OrderAuthorizationHandler.class)
    public OrderDto getOrder(Long orderId) {
        return new OrderDto(orderId, "ORD-" + orderId, "active");
    }

    @AuthorizeEntity(
            ids = {"orderId"},
            includeUser = true,
            authorizationCase = "DELETE",
            authorizationHandler = OrderAuthorizationHandler.class)
    public void deleteOrder(Long orderId) {
        // delete logic
    }

    @AuthorizeEntity(
            ids = {"request.companyId"},
            includeUser = true,
            authorizationHandler = OrderAuthorizationHandler.class)
    public OrderDto createOrder(CreateOrderRequest request) {
        return new OrderDto(1L, "ORD-NEW", request.companyId());
    }

    public record OrderDto(Long id, String code, String status) {}

    public record CreateOrderRequest(String companyId, String name) {}
}
