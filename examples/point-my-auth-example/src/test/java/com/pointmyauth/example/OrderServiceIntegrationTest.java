package com.pointmyauth.example;

import com.pointmyauth.exception.AuthorizationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
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
        authConfig.setCurrentUser(new User(1L, "Alice", false));

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
        authConfig.setCurrentUser(new User(1L, "Alice", false));

        assertThatThrownBy(() -> orderService.deleteOrder(100L))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Only admins can delete orders");
    }

    @Test
    @DisplayName("should allow delete for admin")
    void shouldAllowDeleteForAdmin() {
        authConfig.setCurrentUser(new User(1L, "Admin", true));

        orderService.deleteOrder(100L);
    }
}
