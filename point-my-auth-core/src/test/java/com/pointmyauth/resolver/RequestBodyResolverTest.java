package com.pointmyauth.resolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RequestBodyResolver")
class RequestBodyResolverTest {

    private final RequestBodyResolver resolver = new RequestBodyResolver();

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("should support dotted field-access names")
        void shouldSupportDottedNames() {
            assertThat(resolver.supports("dto.field")).isTrue();
            assertThat(resolver.supports("request.companyId")).isTrue();
        }

        @Test
        @DisplayName("should not support simple names")
        void shouldNotSupportSimpleNames() {
            assertThat(resolver.supports("orderId")).isFalse();
        }

        @Test
        @DisplayName("should not support header-prefixed names")
        void shouldNotSupportHeaderNames() {
            assertThat(resolver.supports("#header:X-Tenant-Id")).isFalse();
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
        @DisplayName("should resolve whole @RequestBody object by parameter name")
        void shouldResolveWholeBody() throws Exception {
            TestDto dto = new TestDto("corp-1", "Andres");
            Method method = StubController.class.getMethod("create", TestDto.class);
            Object result = resolver.resolve("dto", method, new Object[] {dto});

            assertThat(result).isSameAs(dto);
        }

        @Test
        @DisplayName("should resolve nested field via dot notation (depth 1)")
        void shouldResolveNestedField() throws Exception {
            TestDto dto = new TestDto("corp-1", "Andres");
            Method method = StubController.class.getMethod("create", TestDto.class);
            Object result = resolver.resolve("dto.companyId", method, new Object[] {dto});

            assertThat(result).isEqualTo("corp-1");
        }

        @Test
        @DisplayName("should resolve nested object fields (depth 2)")
        void shouldResolveDepth2Field() throws Exception {
            InnerDto inner = new InnerDto("inner-42");
            OuterDto outer = new OuterDto(inner);
            Method method = StubController.class.getMethod("outer", OuterDto.class);
            Object result = resolver.resolve("outer.inner.id", method, new Object[] {outer});

            assertThat(result).isEqualTo("inner-42");
        }

        @Test
        @DisplayName("should return null for depth > 2")
        void shouldReturnNullForDepthOver2() throws Exception {
            TestDto dto = new TestDto("x", "y");
            Method method = StubController.class.getMethod("create", TestDto.class);
            Object result = resolver.resolve("dto.companyId.field.deep", method, new Object[] {dto});

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null when @RequestBody is null")
        void shouldReturnNullWhenBodyIsNull() throws Exception {
            Method method = StubController.class.getMethod("create", TestDto.class);
            Object result = resolver.resolve("dto.companyId", method, new Object[] {null});

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null when field not found")
        void shouldReturnNullWhenFieldNotFound() throws Exception {
            TestDto dto = new TestDto("corp-1", "Andres");
            Method method = StubController.class.getMethod("create", TestDto.class);
            Object result = resolver.resolve("dto.nonexistent", method, new Object[] {dto});

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should resolve field from parent class")
        void shouldResolveFieldFromParentClass() throws Exception {
            ChildDto child = new ChildDto("child-name", 42);
            Method method = StubController.class.getMethod("child", ChildDto.class);
            Object result = resolver.resolve("child.code", method, new Object[] {child});

            assertThat(result).isEqualTo(42);
        }

        @Test
        @DisplayName("should return null when no @RequestBody param matches")
        void shouldReturnNullWhenNoMatch() throws Exception {
            TestDto dto = new TestDto("x", "y");
            Method method = StubController.class.getMethod("create", TestDto.class);
            Object result = resolver.resolve("other.field", method, new Object[] {dto});

            assertThat(result).isNull();
        }
    }

    static class TestDto {
        String companyId;
        String userName;

        TestDto(String companyId, String userName) {
            this.companyId = companyId;
            this.userName = userName;
        }
    }

    static class InnerDto {
        String id;

        InnerDto(String id) {
            this.id = id;
        }
    }

    static class OuterDto {
        InnerDto inner;

        OuterDto(InnerDto inner) {
            this.inner = inner;
        }
    }

    static class BaseDto {
        int code;

        BaseDto(int code) {
            this.code = code;
        }
    }

    static class ChildDto extends BaseDto {
        String name;

        ChildDto(String name, int code) {
            super(code);
            this.name = name;
        }
    }

    @SuppressWarnings("unused")
    static class StubController {

        public void create(@RequestBody TestDto dto) {}

        public void outer(@RequestBody OuterDto outer) {}

        public void child(@RequestBody ChildDto child) {}
    }
}
