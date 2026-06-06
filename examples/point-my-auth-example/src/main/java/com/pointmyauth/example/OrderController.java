package com.pointmyauth.example;

import com.pointmyauth.annotation.AuthorizeEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller demonstrating {@code @AuthorizeEntity} usage in various
 * real-world scenarios.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 1. Simple GET with String parameter — always checks existence
    @GetMapping("/{orderId}")
    @AuthorizeEntity(
            ids = {"orderId"},
            includeUser = false,
            authorizationCase = "READ",
            authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<OrderService.OrderDto> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrder(Long.valueOf(orderId)));
    }

    // 2. GET with int parameter — no user, just validates the ID
    @GetMapping("/count/{quantity}")
    @AuthorizeEntity(
            ids = {"quantity"},
            includeUser = false,
            authorizationCase = "COUNT",
            authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<Integer> getOrderCount(@PathVariable int quantity) {
        return ResponseEntity.ok(quantity);
    }

    // 3. POST with Object (@RequestBody) — checks companyId from body
    @PostMapping
    @AuthorizeEntity(
            ids = {"request.companyId"},
            includeUser = true,
            authorizationCase = "CREATE",
            authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<OrderService.OrderDto> createOrder(@RequestBody OrderService.CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    // 4. DELETE with user check — only admins can delete
    @DeleteMapping("/{orderId}")
    @AuthorizeEntity(
            ids = {"orderId"},
            includeUser = true,
            authorizationCase = "DELETE",
            authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    // 5. GET with HTTP header — tenant-based authorization
    @GetMapping("/tenant")
    @AuthorizeEntity(
            ids = {"#header:X-Tenant-Id"},
            includeUser = false,
            authorizationCase = "TENANT_READ",
            authorizationHandler = TenantAuthorizationHandler.class)
    public ResponseEntity<List<String>> getOrdersByTenant(@RequestHeader("X-Tenant-Id") String tenantId) {
        return ResponseEntity.ok(List.of("order-1", "order-2"));
    }

    // 6. GET with multiple params — orderId + userId
    @GetMapping("/{orderId}/user/{userId}")
    @AuthorizeEntity(
            ids = {"orderId", "userId"},
            includeUser = true,
            authorizationCase = "OWNERSHIP_CHECK",
            authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<Map<String, Object>> getOrderForUser(@PathVariable Long orderId, @PathVariable Long userId) {
        return ResponseEntity.ok(Map.of("orderId", orderId, "userId", userId));
    }

    // 7. Simple GET with RequestParam — no ids, just handler check
    @GetMapping("/public")
    @AuthorizeEntity(
            ids = {},
            includeUser = false,
            authorizationCase = "PUBLIC",
            authorizationHandler = AllowAllAuthorizationHandler.class)
    public ResponseEntity<String> getPublicOrders() {
        return ResponseEntity.ok("public orders list");
    }

    // 8. GET with mixed params — String from path, int from query
    @GetMapping("/search/{category}")
    @AuthorizeEntity(
            ids = {"category", "limit"},
            includeUser = false,
            authorizationCase = "SEARCH",
            authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<List<String>> searchOrders(@PathVariable String category, @RequestParam int limit) {
        return ResponseEntity.ok(List.of("order-1", "order-2"));
    }

    // 9. GET with full body object (no field extraction)
    @PostMapping("/bulk")
    @AuthorizeEntity(
            ids = {"request"},
            includeUser = true,
            authorizationCase = "BULK_CREATE",
            authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<String> bulkCreate(@RequestBody OrderService.CreateOrderRequest request) {
        return ResponseEntity.ok("bulk created");
    }

    // 10. Simple always-allowed — just logs, no real auth
    @GetMapping("/health")
    @AuthorizeEntity(
            ids = {},
            includeUser = false,
            authorizationCase = "HEALTH",
            authorizationHandler = AllowAllAuthorizationHandler.class)
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
