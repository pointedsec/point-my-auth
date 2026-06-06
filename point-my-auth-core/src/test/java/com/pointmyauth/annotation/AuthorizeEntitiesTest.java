package com.pointmyauth.annotation;

import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.handler.AuthorizationHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("@AuthorizeEntities (repeatable)")
class AuthorizeEntitiesTest {

    @Test
    @DisplayName("should be repeatable on same method")
    void shouldBeRepeatable() throws Exception {
        Method method = SampleController.class.getMethod("multiAuth", Long.class);
        AuthorizeEntities container = method.getAnnotation(AuthorizeEntities.class);

        assertThat(container).isNotNull();
        assertThat(container.value()).hasSize(2);
    }

    @Test
    @DisplayName("should contain multiple AuthorizeEntity annotations")
    void shouldContainMultiple() throws Exception {
        Method method = SampleController.class.getMethod("multiAuth", Long.class);
        AuthorizeEntities container = method.getAnnotation(AuthorizeEntities.class);

        AuthorizeEntity first = container.value()[0];
        AuthorizeEntity second = container.value()[1];

        assertThat(first.authorizationHandler()).isEqualTo(HandlerA.class);
        assertThat(first.authorizationCase()).isEqualTo("READ");

        assertThat(second.authorizationHandler()).isEqualTo(HandlerB.class);
        assertThat(second.authorizationCase()).isEqualTo("WRITE");
    }

    @Test
    @DisplayName("single annotation should still be readable as AuthorizeEntity")
    void singleAnnotationReadable() throws Exception {
        Method method = SampleController.class.getMethod("singleAuth", Long.class);
        AuthorizeEntity annotation = method.getAnnotation(AuthorizeEntity.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.authorizationHandler()).isEqualTo(HandlerA.class);
    }

    // --- Test infrastructure ---

    static class SampleController {

        @AuthorizeEntities({
            @AuthorizeEntity(
                    ids = {"id"},
                    authorizationCase = "READ",
                    authorizationHandler = HandlerA.class),
            @AuthorizeEntity(
                    ids = {"id"},
                    authorizationCase = "WRITE",
                    authorizationHandler = HandlerB.class)
        })
        public String multiAuth(Long id) {
            return "multi";
        }

        @AuthorizeEntity(
                ids = {"id"},
                authorizationHandler = HandlerA.class)
        public String singleAuth(Long id) {
            return "single";
        }
    }

    static class HandlerA implements AuthorizationHandler<Object> {
        @Override
        public void authorize(AuthorizationContext<Object> context) {
            // No Operation
        }
    }

    static class HandlerB implements AuthorizationHandler<Object> {
        @Override
        public void authorize(AuthorizationContext<Object> context) {
            // No Operation
        }
    }
}
