package com.pointmyauth.processor;

import com.pointmyauth.context.AuthorizationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthorizationPostProcessor")
class AuthorizationPostProcessorTest {

    private AuthorizationPostProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TestPostProcessor();
    }

    @Test
    @DisplayName("should receive success notification")
    void shouldReceiveSuccess() throws Exception {
        Method method = Object.class.getMethod("toString");
        AuthorizationContext<Object> context =
                AuthorizationContext.builder().interceptedMethod(method).build();

        processor.afterAuthorization(context, true);

        List<Boolean> results = ((TestPostProcessor) processor).results;
        assertThat(results).containsExactly(true);
    }

    @Test
    @DisplayName("should receive failure notification")
    void shouldReceiveFailure() throws Exception {
        Method method = Object.class.getMethod("toString");
        AuthorizationContext<Object> context =
                AuthorizationContext.builder().interceptedMethod(method).build();

        processor.afterAuthorization(context, false);

        List<Boolean> results = ((TestPostProcessor) processor).results;
        assertThat(results).containsExactly(false);
    }

    static class TestPostProcessor implements AuthorizationPostProcessor {
        final List<Boolean> results = new ArrayList<>();

        @Override
        public void afterAuthorization(AuthorizationContext<?> context, boolean success) {
            results.add(success);
        }
    }
}
