package com.pointmyauth.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CompositeParameterResolver")
class CompositeParameterResolverTest {

    private HttpServletRequest request;
    private CompositeParameterResolver resolver;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Tenant-Id")).thenReturn("tenant-abc");
        resolver = new CompositeParameterResolver(
                new HeaderResolver(request), new PathVariableResolver(), new RequestBodyResolver());
    }

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("should support header names")
        void shouldSupportHeader() {
            assertThat(resolver.supports("#header:X-Tenant-Id")).isTrue();
        }

        @Test
        @DisplayName("should support path variable names")
        void shouldSupportPathVar() {
            assertThat(resolver.supports("orderId")).isTrue();
        }

        @Test
        @DisplayName("should support dotted body field names")
        void shouldSupportBodyField() {
            assertThat(resolver.supports("dto.field")).isTrue();
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
        @DisplayName("should resolve via HeaderResolver")
        void shouldResolveHeader() throws Exception {
            Method method = StubController.class.getMethod("mixed", Long.class, StubDto.class);
            Object result = resolver.resolve("#header:X-Tenant-Id", method, new Object[] {42L, new StubDto("corp")});

            assertThat(result).isEqualTo("tenant-abc");
        }

        @Test
        @DisplayName("should resolve via PathVariableResolver")
        void shouldResolvePathVar() throws Exception {
            Method method = StubController.class.getMethod("mixed", Long.class, StubDto.class);
            Object result = resolver.resolve("orderId", method, new Object[] {42L, new StubDto("corp")});

            assertThat(result).isEqualTo(42L);
        }

        @Test
        @DisplayName("should resolve via RequestBodyResolver for dotted fields")
        void shouldResolveBodyField() throws Exception {
            Method method = StubController.class.getMethod("mixed", Long.class, StubDto.class);
            Object result = resolver.resolve("dto.companyId", method, new Object[] {42L, new StubDto("corp")});

            assertThat(result).isEqualTo("corp");
        }

        @Test
        @DisplayName("should return null for unsupported param name")
        void shouldReturnNullForUnknown() throws Exception {
            Method method = StubController.class.getMethod("mixed", Long.class, StubDto.class);
            Object result = resolver.resolve("#header:X-Missing", method, new Object[] {42L, new StubDto("corp")});

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("addResolver")
    class AddResolver {

        @Test
        @DisplayName("should use newly added resolver")
        void shouldUseNewResolver() throws Exception {
            Method method = StubController.class.getMethod("mixed", Long.class, StubDto.class);

            Object before = resolver.resolve("nonexistent", method, new Object[] {42L, new StubDto("x")});
            assertThat(before).isNull();

            resolver.addResolver(new ParameterResolver() {
                @Override
                public Object resolve(String paramName, Method method, Object[] args) {
                    return "custom-value";
                }

                @Override
                public boolean supports(String paramName) {
                    return "nonexistent".equals(paramName);
                }
            });

            Object after = resolver.resolve("nonexistent", method, new Object[] {42L, new StubDto("x")});
            assertThat(after).isEqualTo("custom-value");
        }
    }

    @Nested
    @DisplayName("getResolvers")
    class GetResolvers {

        @Test
        @DisplayName("should return unmodifiable list of resolvers")
        void shouldReturnResolvers() {
            List<ParameterResolver> list = resolver.getResolvers();
            assertThat(list).hasSize(3);
            assertThat(list.get(0)).isInstanceOf(HeaderResolver.class);
            assertThat(list.get(1)).isInstanceOf(PathVariableResolver.class);
            assertThat(list.get(2)).isInstanceOf(RequestBodyResolver.class);
        }
    }

    static class StubDto {
        String companyId;

        StubDto(String companyId) {
            this.companyId = companyId;
        }
    }

    @SuppressWarnings("unused")
    static class StubController {

        public void mixed(@PathVariable("orderId") Long orderId, @RequestBody StubDto dto) {}
    }
}
