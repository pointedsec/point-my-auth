package com.pointmyauth.adminbypass;

import com.pointmyauth.annotation.AuthorizeEntity;
import com.pointmyauth.annotation.ConditionalAuthorize;
import com.pointmyauth.cache.AuthorizationCacheSupport;
import com.pointmyauth.config.PointMyAuthAutoConfiguration;
import com.pointmyauth.config.PointMyAuthConfigurer;
import com.pointmyauth.exception.AuthorizationException;
import com.pointmyauth.user.AdminChecker;
import com.pointmyauth.user.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@Import({
    PointMyAuthAutoConfiguration.class,
    AdminBypassIntegrationTest.TestAuthConfig.class,
    AdminBypassIntegrationTest.TestService.class
})
@EnabledForJreRange(
        min = JRE.JAVA_21,
        max = JRE.JAVA_22,
        disabledReason = "Spring Boot 3.2.5 ASM does not support Java 23+ class file version")
class AdminBypassIntegrationTest {

    @Autowired
    private TestService testService;

    @Autowired
    private TestAuthConfig authConfig;

    @Autowired
    private AuthorizationCacheSupport cacheSupport;

    @BeforeEach
    void setUp() {
        authConfig.clearCurrentUser();
        cacheSupport.clear();
    }

    @Nested
    @DisplayName("skipForAdmin=true (default)")
    class AdminBypassEnabled {

        @Test
        @DisplayName("admin user bypasses handler")
        void adminBypassesHandler() {
            authConfig.setCurrentUser(new TestUser(1L, "Alice", true));

            String result = testService.bypassMethod("data");

            assertThat(result).isEqualTo("data");
        }

        @Test
        @DisplayName("non-admin user invokes handler")
        void nonAdminInvokesHandler() {
            authConfig.setCurrentUser(new TestUser(1L, "Bob", false));

            assertThatThrownBy(() -> testService.bypassMethod("data"))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("null user invokes handler")
        void nullUserInvokesHandler() {
            assertThatThrownBy(() -> testService.bypassMethod("data"))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Access denied");
        }
    }

    @Nested
    @DisplayName("skipForAdmin=false")
    class AdminBypassDisabled {

        @Test
        @DisplayName("admin still invokes handler when skipForAdmin=false")
        void adminInvokesHandler() {
            authConfig.setCurrentUser(new TestUser(1L, "Alice", true));

            assertThatThrownBy(() -> testService.noBypassMethod("data"))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Access denied");
        }
    }

    @Nested
    @DisplayName("Repeatable @AuthorizeEntities with admin bypass")
    class RepeatableAdminBypass {

        @Test
        @DisplayName("admin bypasses all handlers in repeatable annotations")
        void adminBypassesAllHandlers() {
            authConfig.setCurrentUser(new TestUser(1L, "Alice", true));

            String result = testService.multiHandlerMethod("data");

            assertThat(result).isEqualTo("data");
        }

        @Test
        @DisplayName("non-admin invokes all handlers")
        void nonAdminInvokesAllHandlers() {
            authConfig.setCurrentUser(new TestUser(1L, "Bob", false));

            assertThatThrownBy(() -> testService.multiHandlerMethod("data"))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Access denied");
        }
    }

    @Nested
    @DisplayName("@ConditionalAuthorize with admin bypass")
    class ConditionalAuthorizeAdminBypass {

        @Test
        @DisplayName("admin bypasses SpEL and handler (skipForAdmin=true default)")
        void adminBypassesConditional() {
            authConfig.setCurrentUser(new TestUser(1L, "Alice", true));

            String result = testService.conditionalMethod(-1L);

            assertThat(result).isEqualTo("order -1");
        }

        @Test
        @DisplayName("non-admin evaluates SpEL and fails before handler")
        void nonAdminEvaluatesConditional() {
            authConfig.setCurrentUser(new TestUser(1L, "Bob", false));

            assertThatThrownBy(() -> testService.conditionalMethod(-1L))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Conditional authorization denied");
        }

        @Test
        @DisplayName("non-admin passes SpEL and handler is invoked")
        void nonAdminPassesSpelReachesHandler() {
            authConfig.setCurrentUser(new TestUser(1L, "Bob", false));

            assertThatThrownBy(() -> testService.conditionalMethod(42L))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("admin still evaluates SpEL when skipForAdmin=false")
        void adminEvaluatesWhenSkipFalse() {
            authConfig.setCurrentUser(new TestUser(1L, "Alice", true));

            assertThatThrownBy(() -> testService.conditionalNoBypassMethod(-1L))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Conditional authorization denied");
        }

        @Test
        @DisplayName("null user evaluates SpEL and fails before handler")
        void nullUserEvaluatesConditional() {
            assertThatThrownBy(() -> testService.conditionalMethod(-1L))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Conditional authorization denied");
        }
    }

    // --- Test infrastructure ---

    record TestUser(Long id, String name, boolean admin) {}

    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    static class TestApp {}

    @Configuration
    static class TestAuthConfig {
        private TestUser currentUser;

        @Bean
        public PointMyAuthConfigurer authConfigurer() {
            return new PointMyAuthConfigurer() {
                @Override
                public CurrentUserProvider<Object> currentUserProvider() {
                    return () -> currentUser;
                }

                @Override
                public AdminChecker<?> adminChecker() {
                    return user -> user instanceof TestUser tu && tu.admin();
                }
            };
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
                authorizationHandler = DenyHandler.class)
        public String bypassMethod(String data) {
            return data;
        }

        @AuthorizeEntity(
                ids = {},
                includeUser = true,
                skipForAdmin = false,
                authorizationHandler = DenyHandler.class)
        public String noBypassMethod(String data) {
            return data;
        }

        @AuthorizeEntity(
                ids = {},
                includeUser = true,
                authorizationHandler = DenyHandler.class)
        @AuthorizeEntity(
                ids = {},
                includeUser = true,
                authorizationHandler = DenyHandler.class)
        public String multiHandlerMethod(String data) {
            return data;
        }

        @ConditionalAuthorize(condition = "#orderId > 0", authorizationHandler = DenyHandler.class)
        public String conditionalMethod(Long orderId) {
            return "order " + orderId;
        }

        @ConditionalAuthorize(
                condition = "#orderId > 0",
                skipForAdmin = false,
                authorizationHandler = DenyHandler.class)
        public String conditionalNoBypassMethod(Long orderId) {
            return "order " + orderId;
        }
    }

    // DenyHandler is a separate top-level class in com.pointmyauth.adminbypass
}
