package com.pointmyauth.aspect;

import com.pointmyauth.annotation.AuthorizeEntity;
import com.pointmyauth.config.PointMyAuthConfigurer;
import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.exception.AuthorizationException;
import com.pointmyauth.handler.AuthorizationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@Import({
    AuthorizeEntityAspectIntegrationTest.TestAuthConfig.class,
    AuthorizeEntityAspectIntegrationTest.TestService.class,
    AuthorizeEntityAspectIntegrationTest.TestHandler.class
})
class AuthorizeEntityAspectIntegrationTest {

    @Autowired
    private TestService testService;

    @Autowired
    private TestAuthConfig authConfig;

    @BeforeEach
    void setUp() {
        authConfig.clearCurrentUser();
    }

    @Test
    @DisplayName("should intercept annotated method and call handler")
    void shouldInterceptAnnotatedMethod() {
        authConfig.setCurrentUser(new TestUser(1L, "Alice"));

        String result = testService.allowedMethod("data");

        assertThat(result).isEqualTo("data");
    }

    @Test
    @DisplayName("should propagate AuthorizationException from handler")
    void shouldPropagateAuthException() {
        authConfig.clearCurrentUser();

        assertThatThrownBy(() -> testService.deniedMethod("data"))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Not authenticated");
    }

    @Test
    @DisplayName("should pass resolved parameters to context")
    void shouldPassResolvedParams() {
        authConfig.setCurrentUser(new TestUser(1L, "Alice"));

        Long result = testService.withParam(42L);

        assertThat(result).isEqualTo(42L);
    }

    @Test
    @DisplayName("should skip aspect for non-annotated method")
    void shouldSkipNonAnnotated() {
        String result = testService.noAnnotation("data");

        assertThat(result).isEqualTo("data");
    }

    // --- Test infrastructure ---

    record TestUser(Long id, String name) {}

    @SpringBootApplication(scanBasePackages = "com.pointmyauth.aspect")
    static class TestApp {}

    @Configuration
    static class TestAuthConfig {
        private TestUser currentUser;

        @Bean
        public PointMyAuthConfigurer authConfigurer() {
            return () -> () -> currentUser;
        }

        public void setCurrentUser(TestUser user) {
            this.currentUser = user;
        }

        public void clearCurrentUser() {
            this.currentUser = null;
        }
    }

    @Service
    static class TestService {

        @AuthorizeEntity(
                ids = {},
                includeUser = true,
                authorizationHandler = TestHandler.class)
        public String allowedMethod(String data) {
            return data;
        }

        @AuthorizeEntity(
                ids = {},
                includeUser = true,
                authorizationHandler = TestHandler.class)
        public String deniedMethod(String data) {
            return data;
        }

        @AuthorizeEntity(
                ids = {"orderId"},
                includeUser = true,
                authorizationHandler = TestHandler.class)
        public Long withParam(Long orderId) {
            return orderId;
        }

        public String noAnnotation(String data) {
            return data;
        }
    }

    @SuppressWarnings("rawtypes")
    static class TestHandler implements AuthorizationHandler {
        @Override
        public void authorize(AuthorizationContext context) {
            Object user = context.getCurrentUser();
            if (user == null) {
                throw new AuthorizationException("Not authenticated");
            }
        }
    }
}
