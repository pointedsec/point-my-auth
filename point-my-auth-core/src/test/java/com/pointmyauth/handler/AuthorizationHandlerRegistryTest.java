package com.pointmyauth.handler;

import com.pointmyauth.context.AuthorizationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AuthorizationHandlerRegistry")
class AuthorizationHandlerRegistryTest {

    private AuthorizationHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AuthorizationHandlerRegistry();
    }

    @Nested
    @DisplayName("resolve via reflection")
    class ResolveViaReflection {

        @Test
        @DisplayName("should instantiate handler with no-arg constructor")
        void shouldInstantiateViaReflection() {
            TestHandler handler = registry.resolve(TestHandler.class);

            assertThat(handler).isNotNull();
            assertThat(handler).isInstanceOf(TestHandler.class);
        }

        @Test
        @DisplayName("should return same cached instance on second call")
        void shouldReturnCachedInstance() {
            TestHandler first = registry.resolve(TestHandler.class);
            TestHandler second = registry.resolve(TestHandler.class);

            assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("should throw for class without no-arg constructor")
        void shouldThrowForNoNoArgConstructor() {
            assertThatThrownBy(() -> registry.resolve(NoNoArgConstructorHandler.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have a no-arg constructor");
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Test
        @DisplayName("should throw for class not implementing AuthorizationHandler")
        void shouldThrowForNonHandlerClass() {
            assertThatThrownBy(() -> ((AuthorizationHandlerRegistry) registry).resolve((Class) NotAHandler.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("does not implement AuthorizationHandler");
        }
    }

    @Nested
    @DisplayName("resolve via Spring ApplicationContext")
    class ResolveViaApplicationContext {

        @Test
        @DisplayName("should prefer Spring bean over reflection")
        void shouldPreferSpringBean() {
            ApplicationContext ctx = mock(ApplicationContext.class);
            TestHandler bean = new TestHandler();
            when(ctx.getBean(TestHandler.class)).thenReturn(bean);
            registry.setApplicationContext(ctx);

            TestHandler resolved = registry.resolve(TestHandler.class);

            assertThat(resolved).isSameAs(bean);
        }

        @Test
        @DisplayName("should fall back to reflection when bean not found")
        void shouldFallBackToReflection() {
            ApplicationContext ctx = mock(ApplicationContext.class);
            when(ctx.getBean(TestHandler.class))
                    .thenThrow(new org.springframework.beans.factory.NoSuchBeanDefinitionException("not found"));
            registry.setApplicationContext(ctx);

            TestHandler resolved = registry.resolve(TestHandler.class);

            assertThat(resolved).isNotNull();
            assertThat(resolved).isNotSameAs(new TestHandler());
        }

        @Test
        @DisplayName("should work without ApplicationContext set")
        void shouldWorkWithoutApplicationContext() {
            AuthorizationHandlerRegistry noCtx = new AuthorizationHandlerRegistry();

            TestHandler handler = noCtx.resolve(TestHandler.class);

            assertThat(handler).isNotNull();
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register pre-built handler")
        void shouldRegisterPreBuilt() {
            TestHandler custom = new TestHandler();
            registry.register(TestHandler.class, custom);

            TestHandler resolved = registry.resolve(TestHandler.class);

            assertThat(resolved).isSameAs(custom);
        }

        @Test
        @DisplayName("should override previously resolved handler")
        void shouldOverride() {
            TestHandler first = registry.resolve(TestHandler.class);
            TestHandler custom = new TestHandler();
            registry.register(TestHandler.class, custom);

            TestHandler resolved = registry.resolve(TestHandler.class);

            assertThat(resolved).isSameAs(custom);
            assertThat(resolved).isNotSameAs(first);
        }
    }

    @Nested
    @DisplayName("isResolved")
    class IsResolved {

        @Test
        @DisplayName("should return false for unresolved class")
        void shouldReturnFalseForUnresolved() {
            assertThat(registry.isResolved(TestHandler.class)).isFalse();
        }

        @Test
        @DisplayName("should return true for resolved class")
        void shouldReturnTrueForResolved() {
            registry.resolve(TestHandler.class);

            assertThat(registry.isResolved(TestHandler.class)).isTrue();
        }

        @Test
        @DisplayName("should return true for registered handler")
        void shouldReturnTrueForRegistered() {
            registry.register(TestHandler.class, new TestHandler());

            assertThat(registry.isResolved(TestHandler.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("clearCache")
    class ClearCache {

        @Test
        @DisplayName("should clear all cached handlers")
        void shouldClearCache() {
            registry.resolve(TestHandler.class);
            assertThat(registry.isResolved(TestHandler.class)).isTrue();

            registry.clearCache();

            assertThat(registry.isResolved(TestHandler.class)).isFalse();
        }

        @Test
        @DisplayName("should allow re-resolution after clearing")
        void shouldAllowReResolutionAfterClear() {
            TestHandler first = registry.resolve(TestHandler.class);
            registry.clearCache();
            TestHandler second = registry.resolve(TestHandler.class);

            assertThat(first).isNotSameAs(second);
        }
    }

    @Nested
    @DisplayName("thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("should handle concurrent resolution safely")
        void shouldHandleConcurrentResolution() throws Exception {
            int threadCount = 20;
            Thread[] threads = new Thread[threadCount];
            AuthorizationHandler<?>[] results = new AuthorizationHandler<?>[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                threads[i] = new Thread(() -> results[idx] = registry.resolve(TestHandler.class));
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            for (AuthorizationHandler<?> result : results) {
                assertThat(result).isNotNull();
            }

            for (int i = 1; i < threadCount; i++) {
                assertThat(results[0]).isSameAs(results[i]);
            }
        }
    }

    // --- Test handler stubs ---

    static class TestHandler implements AuthorizationHandler<Object> {
        @Override
        public void authorize(AuthorizationContext<Object> context) {
            // no-op for tests
        }
    }

    static class NoNoArgConstructorHandler implements AuthorizationHandler<Object> {
        private final String value;

        NoNoArgConstructorHandler(String value) {
            this.value = value;
        }

        @Override
        public void authorize(AuthorizationContext<Object> context) {
            // no-op
        }
    }

    static class NotAHandler {
        // Does not implement AuthorizationHandler
    }
}
