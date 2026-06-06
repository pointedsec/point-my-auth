package com.pointmyauth.example;

import com.pointmyauth.exception.AuthorizationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = ExampleApplication.class)
@EnabledForJreRange(
        min = JRE.JAVA_21,
        max = JRE.JAVA_22,
        disabledReason = "Spring Boot 3.2.5 ASM does not support Java 23+ class file version")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AuthConfig authConfig;

    @BeforeEach
    void setUp() {
        authConfig.clearCurrentUser();
    }

    @Test
    @DisplayName("should allow access with valid user")
    void shouldAllowWithValidUser() {
        authConfig.setCurrentUser(new PointitUser(1L, "Alice", "alice@example.com", "USER"));

        OrderService.OrderDto result = orderService.getOrder(100L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("should deny access without user")
    void shouldDenyWithoutUser() throws Throwable {
        // Test 1: direct call
        try {
            orderService.getOrder(100L);
            throw new AssertionError("direct call - expected exception");
        } catch (AuthorizationException e) {
            System.out.println("TEST1 (direct): " + e.getMessage());
        }

        // Test 2: through Executable anonymous class
        try {
            new org.junit.jupiter.api.function.Executable() {
                @Override
                public void execute() throws Throwable {
                    orderService.getOrder(100L);
                }
            }.execute();
            throw new AssertionError("anon class - expected exception");
        } catch (AuthorizationException e) {
            System.out.println("TEST2 (anon class): " + e.getMessage());
        }

        // Test 3: through lambda
        try {
            ((org.junit.jupiter.api.function.Executable) () -> orderService.getOrder(100L)).execute();
            throw new AssertionError("lambda - expected exception");
        } catch (AuthorizationException e) {
            System.out.println("TEST3 (lambda): " + e.getMessage());
        }
    }

    @Test
    @DisplayName("should deny delete for non-admin")
    void shouldDenyDeleteForNonAdmin() {
        authConfig.setCurrentUser(new PointitUser(1L, "Alice", "alice@example.com", "USER"));

        assertThrows(AuthorizationException.class, () -> orderService.deleteOrder(100L));
    }

    @Test
    @DisplayName("should allow delete for admin")
    void shouldAllowDeleteForAdmin() {
        authConfig.setCurrentUser(new PointitUser(1L, "Admin", "admin@example.com", "ADMIN"));

        orderService.deleteOrder(100L);
    }
}
