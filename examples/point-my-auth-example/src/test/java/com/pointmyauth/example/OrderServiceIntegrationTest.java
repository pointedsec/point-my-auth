package com.pointmyauth.example;

import com.pointmyauth.cache.AuthorizationCacheSupport;
import com.pointmyauth.exception.AuthorizationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    private AuthorizationCacheSupport cacheSupport;

    @BeforeEach
    void setUp() {
        authConfig.clearCurrentUser();
        cacheSupport.clear();
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
    void shouldDenyWithoutUser() {
        assertThatThrownBy(() -> orderService.getOrder(100L))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("User not authenticated");
    }

    @Test
    @DisplayName("should deny delete for non-admin")
    void shouldDenyDeleteForNonAdmin() {
        authConfig.setCurrentUser(new PointitUser(1L, "Alice", "alice@example.com", "USER"));

        assertThatThrownBy(() -> orderService.deleteOrder(100L))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Only admins can delete orders");
    }

    @Test
    @DisplayName("should allow delete for admin")
    void shouldAllowDeleteForAdmin() {
        authConfig.setCurrentUser(new PointitUser(1L, "Admin", "admin@example.com", "ADMIN"));

        orderService.deleteOrder(100L);
    }
}
