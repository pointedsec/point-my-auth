package com.pointmyauth.test;

import com.pointmyauth.annotation.AuthorizeEntity;
import com.pointmyauth.config.PointMyAuthConfigurer;
import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.exception.AuthorizationException;
import com.pointmyauth.user.CurrentUserProvider;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Test utility for building authorization contexts and configuring
 * the test environment without requiring a full Spring context.
 *
 * <p><strong>Usage example:</strong></p>
 * <pre>{@code
 * // Build a test context
 * AuthorizationContext<User> ctx = AuthorizationTestSupport.context()
 *         .resolvedId("orderId", 42L)
 *         .user(new User(1L, "Alice"))
 *         .authCase("DELETE")
 *         .method(OrderService.class, "deleteOrder")
 *         .build();
 *
 * // Create a PointMyAuthConfigurer for testing
 * PointMyAuthConfigurer configurer = AuthorizationTestSupport.configurer(() -> testUser);
 *
 * // Create a CurrentUserProvider for testing
 * CurrentUserProvider<User> provider = AuthorizationTestSupport.userProvider(() -> testUser);
 * }</pre>
 */
public final class AuthorizationTestSupport {

    private AuthorizationTestSupport() {}

    /**
     * Creates a new context builder pre-configured for testing.
     *
     * @return a builder instance
     */
    public static TestContextBuilder context() {
        return new TestContextBuilder();
    }

    /**
     * Creates a {@link CurrentUserProvider} that returns the given user.
     *
     * @param user the user to return
     * @param <U>  the user type
     * @return a functional provider
     */
    public static <U> CurrentUserProvider<U> userProvider(U user) {
        return () -> user;
    }

    /**
     * Creates a {@link CurrentUserProvider} that returns {@code null} (unauthenticated).
     *
     * @param <U> the user type
     * @return a functional provider returning null
     */
    public static <U> CurrentUserProvider<U> unauthenticatedProvider() {
        return () -> null;
    }

    /**
     * Creates a {@link PointMyAuthConfigurer} backed by the given provider.
     *
     * @param provider the user provider
     * @return a configurer suitable for registration as a Spring bean
     */
    public static PointMyAuthConfigurer configurer(CurrentUserProvider<Object> provider) {
        return () -> provider;
    }

    /**
     * Creates a {@link PointMyAuthConfigurer} that returns the given user.
     *
     * @param user the user to provide
     * @return a configurer suitable for registration as a Spring bean
     */
    public static PointMyAuthConfigurer configurer(Object user) {
        return () -> () -> user;
    }

    /**
     * Creates a {@link PointMyAuthConfigurer} for an unauthenticated state.
     *
     * @return a configurer returning null user
     */
    public static PointMyAuthConfigurer unauthenticatedConfigurer() {
        return () -> () -> null;
    }

    /**
     * Builds a test {@link AuthorizationContext}.
     */
    public static final class TestContextBuilder {

        private final Map<String, Object> resolvedIds = new HashMap<>();
        private Object currentUser;
        private String authorizationCase;
        private Method interceptedMethod;

        private TestContextBuilder() {}

        /**
         * Adds a resolved parameter value.
         *
         * @param name  the parameter name
         * @param value the resolved value
         * @return this builder
         */
        public TestContextBuilder resolvedId(String name, Object value) {
            resolvedIds.put(name, value);
            return this;
        }

        /**
         * Adds all resolved parameter values.
         *
         * @param entries parameter name-value pairs
         * @return this builder
         */
        public TestContextBuilder resolvedIds(Map<String, Object> entries) {
            resolvedIds.putAll(entries);
            return this;
        }

        /**
         * Sets the current user.
         *
         * @param user the user (may be null)
         * @return this builder
         */
        public TestContextBuilder user(Object user) {
            this.currentUser = user;
            return this;
        }

        /**
         * Sets the authorization case label.
         *
         * @param authCase the authorization case (e.g., "CREATE", "DELETE")
         * @return this builder
         */
        public TestContextBuilder authCase(String authCase) {
            this.authorizationCase = authCase;
            return this;
        }

        /**
         * Sets the intercepted method by class and method name.
         *
         * @param clazz      the class containing the method
         * @param methodName the method name
         * @return this builder
         */
        public TestContextBuilder method(Class<?> clazz, String methodName) {
            this.interceptedMethod = findMethod(clazz, methodName);
            return this;
        }

        /**
         * Sets the intercepted method directly.
         *
         * @param method the method
         * @return this builder
         */
        public TestContextBuilder method(Method method) {
            this.interceptedMethod = method;
            return this;
        }

        /**
         * Sets the intercepted method from an {@link AuthorizeEntity} annotation's
         * handler class (for finding the annotated method by handler reference).
         *
         * @param clazz the class containing the annotated method
         * @param methodName the method name
         * @return this builder
         */
        public TestContextBuilder annotatedMethod(Class<?> clazz, String methodName) {
            this.interceptedMethod = findMethod(clazz, methodName);
            return this;
        }

        /**
         * Builds the authorization context.
         * <p>If no method was set, a synthetic method is created.</p>
         *
         * @return the built context
         */
        @SuppressWarnings("unchecked")
        public <U> AuthorizationContext<U> build() {
            if (interceptedMethod == null) {
                interceptedMethod = defaultMethod();
            }

            AuthorizationContext.Builder<U> builder = AuthorizationContext.builder();
            builder.resolvedIds(resolvedIds);
            builder.currentUser((U) currentUser);
            builder.authorizationCase(authorizationCase);
            builder.interceptedMethod(interceptedMethod);
            return builder.build();
        }

        private static Method findMethod(Class<?> clazz, String name) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(name)) {
                    return m;
                }
            }
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(name)) {
                    return m;
                }
            }
            throw new IllegalArgumentException("No method named '" + name + "' found in " + clazz.getName());
        }

        @SuppressWarnings("unchecked")
        private Method defaultMethod() {
            try {
                return TestContextBuilder.class.getDeclaredMethod("defaultMethod");
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Asserts that the given {@link AuthorizationException} has the expected message.
     *
     * @param exception       the caught exception
     * @param expectedMessage the expected message substring
     */
    public static void assertAuthorizationDenied(AuthorizationException exception, String expectedMessage) {
        if (exception == null) {
            throw new AssertionError("Expected AuthorizationException but no exception was thrown");
        }
        if (expectedMessage != null && !exception.getMessage().contains(expectedMessage)) {
            throw new AssertionError(
                    "Expected message containing '" + expectedMessage + "' but got '" + exception.getMessage() + "'");
        }
    }

    /**
     * Asserts that the given exception is an {@link AuthorizationException}.
     *
     * @param exception the caught exception
     */
    public static void assertAuthorizationDenied(RuntimeException exception) {
        if (!(exception instanceof AuthorizationException)) {
            throw new AssertionError("Expected AuthorizationException but got "
                    + exception.getClass().getName());
        }
        assertAuthorizationDenied((AuthorizationException) exception, null);
    }
}
