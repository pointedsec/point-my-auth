package com.pointmyauth.test;

import com.pointmyauth.config.PointMyAuthConfigurer;
import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.exception.AuthorizationException;
import com.pointmyauth.user.CurrentUserProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationTestSupportTest {

    @Test
    @DisplayName("should build context with all fields")
    void shouldBuildContext() {
        AuthorizationContext<String> ctx = AuthorizationTestSupport.context()
                .resolvedId("orderId", 42L)
                .resolvedId("name", "test")
                .user("alice")
                .authCase("DELETE")
                .method(AuthorizationTestSupportTest.class, "shouldBuildContext")
                .build();

        assertThat(ctx.getCurrentUser()).isEqualTo("alice");
        assertThat(ctx.getAuthorizationCase()).isEqualTo("DELETE");
        assertThat(ctx.getLongId("orderId")).isEqualTo(42L);
        assertThat(ctx.getStringId("name")).isEqualTo("test");
        assertThat(ctx.getInterceptedMethod().getName()).isEqualTo("shouldBuildContext");
    }

    @Test
    @DisplayName("should build context with default method when none set")
    void shouldBuildContextWithDefaultMethod() {
        AuthorizationContext<Object> ctx =
                AuthorizationTestSupport.context().user("alice").build();

        assertThat(ctx.getCurrentUser()).isEqualTo("alice");
        assertThat(ctx.getInterceptedMethod()).isNotNull();
    }

    @Test
    @DisplayName("should create user provider")
    void shouldCreateUserProvider() {
        CurrentUserProvider<String> provider = AuthorizationTestSupport.userProvider("alice");

        assertThat(provider.getCurrentUser()).isEqualTo("alice");
    }

    @Test
    @DisplayName("should create unauthenticated provider")
    void shouldCreateUnauthenticatedProvider() {
        CurrentUserProvider<Object> provider = AuthorizationTestSupport.unauthenticatedProvider();

        assertThat(provider.getCurrentUser()).isNull();
    }

    @Test
    @DisplayName("should create configurer from provider")
    void shouldCreateConfigurerFromProvider() {
        CurrentUserProvider<Object> userProvider = AuthorizationTestSupport.userProvider("alice");
        PointMyAuthConfigurer configurer = AuthorizationTestSupport.configurer(userProvider);

        assertThat(configurer.currentUserProvider().getCurrentUser()).isEqualTo("alice");
    }

    @Test
    @DisplayName("should create configurer from user")
    void shouldCreateConfigurerFromUser() {
        PointMyAuthConfigurer configurer = AuthorizationTestSupport.configurer("alice");

        assertThat(configurer.currentUserProvider().getCurrentUser()).isEqualTo("alice");
    }

    @Test
    @DisplayName("should create unauthenticated configurer")
    void shouldCreateUnauthenticatedConfigurer() {
        PointMyAuthConfigurer configurer = AuthorizationTestSupport.unauthenticatedConfigurer();

        assertThat(configurer.currentUserProvider().getCurrentUser()).isNull();
    }

    @Test
    @DisplayName("should resolve typed id")
    void shouldResolveTypedId() {
        AuthorizationContext<Object> ctx =
                AuthorizationTestSupport.context().resolvedId("count", 42).build();

        Integer count = ctx.getId("count", Integer.class);
        assertThat(count).isEqualTo(42);
    }

    @Test
    @DisplayName("should support multiple resolved ids from map")
    void shouldSupportMultipleResolvedIds() {
        Map<String, Object> ids = Map.of("orderId", 100L, "tenantId", "T1");

        AuthorizationContext<Object> ctx =
                AuthorizationTestSupport.context().resolvedIds(ids).build();

        assertThat(ctx.getLongId("orderId")).isEqualTo(100L);
        assertThat(ctx.getStringId("tenantId")).isEqualTo("T1");
    }

    @Test
    @DisplayName("should build context with method directly")
    void shouldBuildContextWithMethodDirectly() throws NoSuchMethodException {
        java.lang.reflect.Method method =
                AuthorizationTestSupportTest.class.getDeclaredMethod("shouldBuildContextWithMethodDirectly");

        AuthorizationContext<Object> ctx =
                AuthorizationTestSupport.context().method(method).build();

        assertThat(ctx.getInterceptedMethod()).isEqualTo(method);
    }

    @Test
    @DisplayName("should assert authorization denied with message")
    void shouldAssertAuthorizationDenied() {
        AuthorizationException ex = new AuthorizationException("Access denied for user bob");

        AuthorizationTestSupport.assertAuthorizationDenied(ex, "user bob");
    }

    @Test
    @DisplayName("should throw AssertionError when expected message not found")
    void shouldThrowOnWrongMessage() {
        AuthorizationException ex = new AuthorizationException("Access denied");

        assertThatThrownBy(() -> AuthorizationTestSupport.assertAuthorizationDenied(ex, "user bob"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected message containing");
    }

    @Test
    @DisplayName("should throw AssertionError when exception is null")
    void shouldThrowOnNullException() {
        assertThatThrownBy(() -> AuthorizationTestSupport.assertAuthorizationDenied(null, "msg"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("no exception was thrown");
    }

    @Test
    @DisplayName("should build context with null user")
    void shouldBuildContextWithNullUser() {
        AuthorizationContext<Object> ctx =
                AuthorizationTestSupport.context().user(null).build();

        assertThat(ctx.getCurrentUser()).isNull();
    }
}
