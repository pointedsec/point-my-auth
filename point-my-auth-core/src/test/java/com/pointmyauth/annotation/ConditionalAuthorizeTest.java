package com.pointmyauth.annotation;

import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.handler.AuthorizationHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("@ConditionalAuthorize")
class ConditionalAuthorizeTest {

    @Test
    @DisplayName("should be present on annotated method")
    void shouldBePresent() throws Exception {
        Method method = SampleController.class.getMethod("conditionalMethod", Long.class);
        ConditionalAuthorize annotation = method.getDeclaredAnnotation(ConditionalAuthorize.class);

        assertThat(annotation).isNotNull();
    }

    @Test
    @DisplayName("should read condition expression")
    void shouldReadCondition() throws Exception {
        Method method = SampleController.class.getMethod("conditionalMethod", Long.class);
        ConditionalAuthorize annotation = method.getDeclaredAnnotation(ConditionalAuthorize.class);

        assertThat(annotation.condition()).isEqualTo("#orderId > 0");
    }

    @Test
    @DisplayName("should read handler class")
    void shouldReadHandler() throws Exception {
        Method method = SampleController.class.getMethod("conditionalMethod", Long.class);
        ConditionalAuthorize annotation = method.getDeclaredAnnotation(ConditionalAuthorize.class);

        assertThat(annotation.authorizationHandler()).isEqualTo(TestHandler.class);
    }

    // --- Test infrastructure ---

    static class SampleController {

        @ConditionalAuthorize(condition = "#orderId > 0", authorizationHandler = TestHandler.class)
        public String conditionalMethod(Long orderId) {
            return "result";
        }
    }

    static class TestHandler implements AuthorizationHandler<Object> {
        @Override
        public void authorize(AuthorizationContext<Object> context) {
            // No Operation
        }
    }
}
