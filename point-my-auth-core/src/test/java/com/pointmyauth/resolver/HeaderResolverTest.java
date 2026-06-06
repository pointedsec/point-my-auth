package com.pointmyauth.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("HeaderResolver")
class HeaderResolverTest {

    private HttpServletRequest request;
    private HeaderResolver resolver;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        resolver = new HeaderResolver(request);
    }

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("should support #header: prefixed names")
        void shouldSupportHeaderPrefix() {
            assertThat(resolver.supports("#header:X-Tenant-Id")).isTrue();
            assertThat(resolver.supports("#header:Authorization")).isTrue();
        }

        @Test
        @DisplayName("should not support names without #header: prefix")
        void shouldNotSupportNonHeaderNames() {
            assertThat(resolver.supports("orderId")).isFalse();
            assertThat(resolver.supports("dto.field")).isFalse();
        }

        @Test
        @DisplayName("should not support null")
        void shouldNotSupportNull() {
            assertThat(resolver.supports(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("should resolve header value")
        void shouldResolveHeaderValue() throws Exception {
            when(request.getHeader("X-Tenant-Id")).thenReturn("tenant-123");
            Method method = StubController.class.getMethod("anyMethod");
            Object result = resolver.resolve("#header:X-Tenant-Id", method, new Object[]{});

            assertThat(result).isEqualTo("tenant-123");
        }

        @Test
        @DisplayName("should return null when header not present")
        void shouldReturnNullForMissingHeader() throws Exception {
            when(request.getHeader("X-Missing")).thenReturn(null);
            Method method = StubController.class.getMethod("anyMethod");
            Object result = resolver.resolve("#header:X-Missing", method, new Object[]{});

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for non-header param name")
        void shouldReturnNullForNonHeaderName() throws Exception {
            Method method = StubController.class.getMethod("anyMethod");
            Object result = resolver.resolve("orderId", method, new Object[]{});

            assertThat(result).isNull();
        }
    }

    @SuppressWarnings("unused")
    static class StubController {

        public void anyMethod() {
        }
    }
}
