package com.pointmyauth.example;

import com.pointmyauth.annotation.AuthorizeEntity;
import com.pointmyauth.annotation.ConditionalAuthorize;
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
 * REST controller demonstrating {@code @AuthorizeEntity} and
 * {@code @ConditionalAuthorize} usage in various real-world scenarios.
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

    // =====================================================================
    // @ConditionalAuthorize examples — SpEL-based conditional authorization
    // =====================================================================

    // 11. Simple numeric comparison
    @GetMapping("/conditional/positive/{orderId}")
    @ConditionalAuthorize(condition = "#orderId > 0", authorizationHandler = ConditionalOrderHandler.class)
    public ResponseEntity<String> getPositiveOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok("order " + orderId);
    }

    // 12. Check string is not null/empty
    @GetMapping("/conditional/valid-name")
    @ConditionalAuthorize(
            condition = "#name != null and #name.length() > 0",
            authorizationHandler = ConditionalOrderHandler.class)
    public ResponseEntity<String> getByName(@RequestParam String name) {
        return ResponseEntity.ok("found: " + name);
    }

    // 13. Check object attribute
    @PostMapping("/conditional/valid-company")
    @ConditionalAuthorize(
            condition = "#request.companyId != null and #request.companyId.length() >= 3",
            authorizationHandler = ConditionalOrderHandler.class)
    public ResponseEntity<String> createValidCompanyOrder(@RequestBody OrderService.CreateOrderRequest request) {
        return ResponseEntity.ok("created for " + request.companyId());
    }

    // 14. Check array/list size
    @PostMapping("/conditional/batch-limit")
    @ConditionalAuthorize(condition = "#orderIds.size() <= 10", authorizationHandler = ConditionalOrderHandler.class)
    public ResponseEntity<String> batchProcess(@RequestBody List<Long> orderIds) {
        return ResponseEntity.ok("processed " + orderIds.size() + " orders");
    }

    // 15. Multiple conditions combined with AND
    @GetMapping("/conditional/combined/{orderId}")
    @ConditionalAuthorize(
            condition = "#orderId > 0 and #orderId < 1000000",
            authorizationHandler = ConditionalOrderHandler.class)
    public ResponseEntity<String> getValidRangeOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok("valid order " + orderId);
    }

    // 16. Check enum or string equality
    @PostMapping("/conditional/allowed-status")
    @ConditionalAuthorize(
            condition = "#status == 'ACTIVE' or #status == 'PENDING'",
            authorizationHandler = ConditionalOrderHandler.class)
    public ResponseEntity<String> getByStatus(@RequestParam String status) {
        return ResponseEntity.ok("orders with status: " + status);
    }

    // 17. Check nested object attribute
    @PostMapping("/conditional/nested")
    @ConditionalAuthorize(
            condition = "#request != null and #request.name != null and #request.name.length() > 2",
            authorizationHandler = ConditionalOrderHandler.class)
    public ResponseEntity<String> createWithValidName(@RequestBody OrderService.CreateOrderRequest request) {
        return ResponseEntity.ok("created: " + request.name());
    }

    // 18. Boolean check
    @GetMapping("/conditional/flag/{orderId}")
    @ConditionalAuthorize(condition = "#orderId != null", authorizationHandler = ConditionalOrderHandler.class)
    public ResponseEntity<String> getNonNullOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok("order " + orderId);
    }

    // =====================================================================
    // Admin bypass examples — skipForAdmin controls whether admins
    // bypass the authorization handler.
    // =====================================================================

    // 19. Default (skipForAdmin=true) — admins bypass, non-admins go through handler
    @GetMapping("/admin-bypass/{orderId}")
    @AuthorizeEntity(
            ids = {"orderId"},
            includeUser = true,
            authorizationCase = "READ",
            authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<String> adminBypassEndpoint(@PathVariable String orderId) {
        return ResponseEntity.ok("order " + orderId);
    }

    // 20. skipForAdmin=false — handler always runs, even for admins
    @DeleteMapping("/admin-no-bypass/{orderId}")
    @AuthorizeEntity(
            ids = {"orderId"},
            includeUser = true,
            skipForAdmin = false,
            authorizationCase = "DELETE",
            authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<String> adminNoBypassEndpoint(@PathVariable String orderId) {
        return ResponseEntity.ok("deleted " + orderId);
    }

    // 21. @ConditionalAuthorize with admin bypass — admins skip SpEL + handler
    @GetMapping("/conditional/admin-bypass/{orderId}")
    @ConditionalAuthorize(
            condition = "#orderId > 0",
            authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<String> conditionalAdminBypass(@PathVariable Long orderId) {
        return ResponseEntity.ok("conditional order " + orderId);
    }

    // 22. @ConditionalAuthorize with skipForAdmin=false — always evaluates SpEL
    @GetMapping("/conditional/admin-no-bypass/{orderId}")
    @ConditionalAuthorize(
            condition = "#orderId > 0",
            skipForAdmin = false,
            authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<String> conditionalAdminNoBypass(@PathVariable Long orderId) {
        return ResponseEntity.ok("conditional no-bypass order " + orderId);
    }
}
