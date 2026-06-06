package com.pointmyauth.resolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
    @DisplayName("@PathVariable resolution")
    class PathVariableResolution {

        @Test
        @DisplayName("should resolve by value attribute")
        void shouldResolveByValue() throws Exception {
            Method method = StubController.class.getMethod("byValue", Long.class);
            Object result = resolver.resolve("orderId", method, new Object[] {42L});
            assertThat(result).isEqualTo(42L);
        }

        @Test
        @DisplayName("should resolve by name attribute")
        void shouldResolveByName() throws Exception {
            Method method = StubController.class.getMethod("byName", Long.class);
            Object result = resolver.resolve("userId", method, new Object[] {99L});
            assertThat(result).isEqualTo(99L);
        }

        @Test
        @DisplayName("should resolve by Java parameter name when no value/name set")
        void shouldResolveByJavaParamName() throws Exception {
            Method method = StubController.class.getMethod("byJavaName", String.class);
            Object result = resolver.resolve("tenantId", method, new Object[] {"t-001"});
            assertThat(result).isEqualTo("t-001");
        }
    }

    @Nested
    @DisplayName("general parameter resolution (no annotations)")
    class GeneralParameterResolution {

        @Test
        @DisplayName("should resolve String parameter by Java name")
        void shouldResolveString() throws Exception {
            Method method = StubController.class.getMethod("withString", String.class);
            Object result = resolver.resolve("username", method, new Object[] {"alice"});
            assertThat(result).isEqualTo("alice");
        }

        @Test
        @DisplayName("should resolve Integer parameter by Java name")
        void shouldResolveInteger() throws Exception {
            Method method = StubController.class.getMethod("withInteger", Integer.class);
            Object result = resolver.resolve("count", method, new Object[] {Integer.valueOf(10)});
            assertThat(result).isEqualTo(10);
        }

        @Test
        @DisplayName("should resolve Long parameter by Java name")
        void shouldResolveLong() throws Exception {
            Method method = StubController.class.getMethod("withLong", Long.class);
            Object result = resolver.resolve("orderId", method, new Object[] {123L});
            assertThat(result).isEqualTo(123L);
        }

        @Test
        @DisplayName("should resolve primitive int by Java name")
        void shouldResolvePrimitiveInt() throws Exception {
            Method method = StubController.class.getMethod("withPrimitiveInt", int.class);
            Object result = resolver.resolve("quantity", method, new Object[] {5});
            assertThat(result).isEqualTo(5);
        }

        @Test
        @DisplayName("should resolve primitive long by Java name")
        void shouldResolvePrimitiveLong() throws Exception {
            Method method = StubController.class.getMethod("withPrimitiveLong", long.class);
            Object result = resolver.resolve("timestamp", method, new Object[] {999L});
            assertThat(result).isEqualTo(999L);
        }

        @Test
        @DisplayName("should resolve primitive boolean by Java name")
        void shouldResolvePrimitiveBoolean() throws Exception {
            Method method = StubController.class.getMethod("withPrimitiveBoolean", boolean.class);
            Object result = resolver.resolve("active", method, new Object[] {true});
            assertThat(result).isEqualTo(true);
        }

        @Test
        @DisplayName("should resolve primitive double by Java name")
        void shouldResolvePrimitiveDouble() throws Exception {
            Method method = StubController.class.getMethod("withPrimitiveDouble", double.class);
            Object result = resolver.resolve("price", method, new Object[] {19.99d});
            assertThat(result).isEqualTo(19.99d);
        }

        @Test
        @DisplayName("should resolve ArrayList by Java name")
        void shouldResolveArrayList() throws Exception {
            List<Long> ids = new ArrayList<>();
            ids.add(1L);
            ids.add(2L);
            ids.add(3L);
            Method method = StubController.class.getMethod("withArrayList", List.class);
            Object result = resolver.resolve("itemIds", method, new Object[] {ids});
            assertThat(result).isSameAs(ids);
            List<Long> resultList = (List<Long>) result;
            assertThat(resultList).containsExactly(1L, 2L, 3L);
        }

        @Test
        @DisplayName("should resolve Object parameter (POJO) by Java name")
        void shouldResolveObject() throws Exception {
            SampleDto dto = new SampleDto("field1", 42);
            Method method = StubController.class.getMethod("withObject", SampleDto.class);
            Object result = resolver.resolve("dto", method, new Object[] {dto});
            assertThat(result).isSameAs(dto);
            assertThat(((SampleDto) result).name).isEqualTo("field1");
            assertThat(((SampleDto) result).value).isEqualTo(42);
        }

        @Test
        @DisplayName("should resolve multiple parameters by their Java names")
        void shouldResolveMultipleParams() throws Exception {
            Method method = StubController.class.getMethod("withMultiple", String.class, Long.class, Integer.class);

            assertThat(resolver.resolve("name", method, new Object[] {"bob", 100L, 7}))
                    .isEqualTo("bob");
            assertThat(resolver.resolve("id", method, new Object[] {"bob", 100L, 7}))
                    .isEqualTo(100L);
            assertThat(resolver.resolve("page", method, new Object[] {"bob", 100L, 7}))
                    .isEqualTo(7);
        }

        @Test
        @DisplayName("should resolve null argument by Java name")
        void shouldResolveNullArgument() throws Exception {
            Method method = StubController.class.getMethod("withString", String.class);
            Object result = resolver.resolve("username", method, new Object[] {null});
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should return null for unknown parameter name")
        void shouldReturnNullForUnknownName() throws Exception {
            Method method = StubController.class.getMethod("withString", String.class);
            Object result = resolver.resolve("nonexistent", method, new Object[] {"alice"});
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should resolve mixed annotated and non-annotated parameters")
        void shouldResolveMixedParams() throws Exception {
            Method method = StubController.class.getMethod("mixed", Long.class, String.class);
            assertThat(resolver.resolve("orderId", method, new Object[] {42L, "data"}))
                    .isEqualTo(42L);
            assertThat(resolver.resolve("body", method, new Object[] {42L, "data"}))
                    .isEqualTo("data");
        }
    }

    static class SampleDto {
        String name;
        int value;

        SampleDto(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    @SuppressWarnings("unused")
    static class StubController {

        public void byValue(@PathVariable("orderId") Long orderId) {}

        public void byName(@PathVariable(name = "userId") Long userId) {}

        public void byJavaName(@PathVariable String tenantId) {}

        public void mixed(@PathVariable("orderId") Long orderId, String body) {}

        public void withString(String username) {}

        public void withInteger(Integer count) {}

        public void withLong(Long orderId) {}

        public void withPrimitiveInt(int quantity) {}

        public void withPrimitiveLong(long timestamp) {}

        public void withPrimitiveBoolean(boolean active) {}

        public void withPrimitiveDouble(double price) {}

        public void withArrayList(List<Long> itemIds) {}

        public void withObject(SampleDto dto) {}

        public void withMultiple(String name, Long id, Integer page) {}
    }
}
