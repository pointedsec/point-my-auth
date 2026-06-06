package com.pointmyauth.resolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PathVariableResolver")
class PathVariableResolverTest {

    private final PathVariableResolver resolver = new PathVariableResolver();

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("should support simple parameter names")
        void shouldSupportSimpleNames() {
            assertThat(resolver.supports("orderId")).isTrue();
            assertThat(resolver.supports("userId")).isTrue();
        }

        @Test
        @DisplayName("should not support header-prefixed names")
        void shouldNotSupportHeaderNames() {
            assertThat(resolver.supports("#header:X-Tenant-Id")).isFalse();
        }

        @Test
        @DisplayName("should not support dotted names")
        void shouldNotSupportDottedNames() {
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
        @DisplayName("should resolve @PathVariable by value attribute")
        void shouldResolveByValue() throws Exception {
            Method method = StubController.class.getMethod("byValue", Long.class);
            Object result = resolver.resolve("orderId", method, new Object[]{42L});

            assertThat(result).isEqualTo(42L);
        }

        @Test
        @DisplayName("should resolve @PathVariable by name attribute")
        void shouldResolveByName() throws Exception {
            Method method = StubController.class.getMethod("byName", Long.class);
            Object result = resolver.resolve("userId", method, new Object[]{99L});

            assertThat(result).isEqualTo(99L);
        }

        @Test
        @DisplayName("should resolve @PathVariable by Java parameter name when no value/name set")
        void shouldResolveByJavaParamName() throws Exception {
            Method method = StubController.class.getMethod("byJavaName", String.class);
            Object result = resolver.resolve("tenantId", method, new Object[]{"t-001"});

            assertThat(result).isEqualTo("t-001");
        }

        @Test
        @DisplayName("should return null when no @PathVariable matches")
        void shouldReturnNullWhenNoMatch() throws Exception {
            Method method = StubController.class.getMethod("byValue", Long.class);
            Object result = resolver.resolve("otherId", method, new Object[]{42L});

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should skip parameters without @PathVariable")
        void shouldSkipNonPathVariableParams() throws Exception {
            Method method = StubController.class.getMethod("mixed", Long.class, String.class);
            Object result = resolver.resolve("body", method, new Object[]{1L, "data"});

            assertThat(result).isNull();
        }
    }

    @SuppressWarnings("unused")
    static class StubController {

        public void byValue(@PathVariable("orderId") Long orderId) {
        }

        public void byName(@PathVariable(name = "userId") Long userId) {
        }

        public void byJavaName(@PathVariable String tenantId) {
        }

        public void mixed(@PathVariable("orderId") Long orderId, String body) {
        }
    }
}
