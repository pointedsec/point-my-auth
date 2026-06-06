package com.pointmyauth.annotation;

import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.handler.AuthorizationHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("@AuthorizeEntity")
class AuthorizeEntityTest {

    @Test
    @DisplayName("annotation should be present on annotated method")
    void shouldBePresentOnAnnotatedMethod() throws Exception {
        Method method = SampleService.class.getMethod("getOrder", Long.class);
        AuthorizeEntity annotation = method.getDeclaredAnnotation(AuthorizeEntity.class);

        assertThat(annotation).isNotNull();
    }

    @Test
    @DisplayName("should read ids attribute")
    void shouldReadIds() throws Exception {
        Method method = SampleService.class.getMethod("getOrder", Long.class);
        AuthorizeEntity annotation = method.getDeclaredAnnotation(AuthorizeEntity.class);

        assertThat(annotation.ids()).containsExactly("orderId");
    }

    @Test
    @DisplayName("should read includeUser default")
    void shouldReadIncludeUserDefault() throws Exception {
        Method method = SampleService.class.getMethod("getOrder", Long.class);
        AuthorizeEntity annotation = method.getDeclaredAnnotation(AuthorizeEntity.class);

        assertThat(annotation.includeUser()).isTrue();
    }

    @Test
    @DisplayName("should read includeAuthorizationCase and authorizationCase default")
    void shouldReadAuthCaseDefaults() throws Exception {
        Method method = SampleService.class.getMethod("getOrder", Long.class);
        AuthorizeEntity annotation = method.getDeclaredAnnotation(AuthorizeEntity.class);

        assertThat(annotation.includeAuthorizationCase()).isFalse();
        assertThat(annotation.authorizationCase()).isEmpty();
    }

    @Test
    @DisplayName("should read authorizationHandler attribute")
    void shouldReadAuthHandler() throws Exception {
        Method method = SampleService.class.getMethod("getOrder", Long.class);
        AuthorizeEntity annotation = method.getDeclaredAnnotation(AuthorizeEntity.class);

        assertThat(annotation.authorizationHandler()).isEqualTo(SampleAuthorizationHandler.class);
    }

    @Test
    @DisplayName("should allow authorizationCase with explicit include")
    void shouldAllowExplicitAuthCase() throws Exception {
        Method method = SampleService.class.getMethod("deleteOrder", Long.class);
        AuthorizeEntity annotation = method.getDeclaredAnnotation(AuthorizeEntity.class);

        assertThat(annotation.includeAuthorizationCase()).isTrue();
        assertThat(annotation.authorizationCase()).isEqualTo("DELETE");
    }

    @SuppressWarnings("unused")
    static class SampleService {

        @AuthorizeEntity(
                ids = {"orderId"},
                includeUser = true,
                authorizationHandler = SampleAuthorizationHandler.class)
        public String getOrder(Long orderId) {
            return "order";
        }

        @AuthorizeEntity(
                ids = {"orderId"},
                includeAuthorizationCase = true,
                authorizationCase = "DELETE",
                authorizationHandler = SampleAuthorizationHandler.class)
        public String deleteOrder(Long orderId) {
            return "deleted";
        }
    }

    static class SampleAuthorizationHandler implements AuthorizationHandler<Object> {
        @Override
        public void authorize(AuthorizationContext<Object> context) {
            // no-op for tests
        }
    }
}
