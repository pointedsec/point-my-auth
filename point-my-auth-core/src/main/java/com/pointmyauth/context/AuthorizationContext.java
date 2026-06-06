package com.pointmyauth.context;

import jakarta.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable context object populated by the
 * {@link com.pointmyauth.aspect.AuthorizeEntityAspect} and passed to
 * {@link com.pointmyauth.handler.AuthorizationHandler#authorize(AuthorizationContext)}.
 * <p>
 * Contains the resolved parameter values, the intercepted method, an optional
 * authorization case label, and the current user (when available).
 *
 * @param <U> the application's user type
 */
public final class AuthorizationContext<U> {

    private final Map<String, Object> resolvedIds;
    @Nullable
    private final U currentUser;
    @Nullable
    private final String authorizationCase;
    private final Method interceptedMethod;

    private AuthorizationContext(Builder<U> builder) {
        this.resolvedIds = Collections.unmodifiableMap(new HashMap<>(builder.resolvedIds));
        this.currentUser = builder.currentUser;
        this.authorizationCase = builder.authorizationCase;
        this.interceptedMethod = builder.interceptedMethod;
    }

    /**
     * Returns an immutable view of the resolved parameter map.
     *
     * @return the resolved parameter values keyed by name
     */
    public Map<String, Object> getResolvedIds() {
        return resolvedIds;
    }

    /**
     * Returns the current user, or {@code null} if not available.
     *
     * @return the current user, or {@code null}
     */
    @Nullable
    public U getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns the authorization case label, or {@code null} if not set.
     *
     * @return the authorization case, or {@code null}
     */
    @Nullable
    public String getAuthorizationCase() {
        return authorizationCase;
    }

    /**
     * Returns the intercepted Spring-managed method.
     *
     * @return the intercepted method
     */
    public Method getInterceptedMethod() {
        return interceptedMethod;
    }

    /**
     * Retrieves a resolved parameter as the given type.
     *
     * @param <T>   the expected type
     * @param name  the parameter name
     * @param type  the expected class
     * @return the resolved value
     * @throws ClassCastException if the value cannot be cast to the expected type
     */
    @SuppressWarnings("unchecked")
    public <T> T getId(String name, Class<T> type) {
        Object value = resolvedIds.get(name);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException(
                    "Parameter '" + name + "' is of type " + value.getClass().getName()
                            + " but " + type.getName() + " was requested");
        }
        return (T) value;
    }

    /**
     * Convenience method to retrieve a resolved parameter as {@link Long}.
     *
     * @param name the parameter name
     * @return the long value, or {@code null} if not present
     * @throws ClassCastException if the value is not a {@link Long}
     */
    @Nullable
    public Long getLongId(String name) {
        Object value = resolvedIds.get(name);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value == null) {
            return null;
        }
        throw new ClassCastException(
                "Parameter '" + name + "' is of type " + value.getClass().getName()
                        + " and cannot be converted to Long");
    }

    /**
     * Convenience method to retrieve a resolved parameter as {@link String}.
     *
     * @param name the parameter name
     * @return the string value, or {@code null} if not present
     */
    @Nullable
    public String getStringId(String name) {
        Object value = resolvedIds.get(name);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Creates a new {@link Builder} instance.
     *
     * @param <U> the application's user type
     * @return a new builder
     */
    public static <U> Builder<U> builder() {
        return new Builder<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthorizationContext<?> that)) return false;
        return Objects.equals(resolvedIds, that.resolvedIds)
                && Objects.equals(currentUser, that.currentUser)
                && Objects.equals(authorizationCase, that.authorizationCase)
                && Objects.equals(interceptedMethod, that.interceptedMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resolvedIds, currentUser, authorizationCase, interceptedMethod);
    }

    @Override
    public String toString() {
        return "AuthorizationContext{"
                + "resolvedIds=" + resolvedIds
                + ", currentUser=" + currentUser
                + ", authorizationCase='" + authorizationCase + '\''
                + ", interceptedMethod=" + (interceptedMethod != null ? interceptedMethod.getName() : "null")
                + '}';
    }

    /**
     * Builder for {@link AuthorizationContext}.
     *
     * @param <U> the application's user type
     */
    public static final class Builder<U> {

        private final Map<String, Object> resolvedIds = new HashMap<>();
        @Nullable
        private U currentUser;
        @Nullable
        private String authorizationCase;
        private Method interceptedMethod;

        private Builder() {
        }

        /**
         * Adds a resolved parameter value.
         *
         * @param name  the parameter name
         * @param value the resolved value
         * @return this builder
         */
        public Builder<U> resolvedId(String name, Object value) {
            this.resolvedIds.put(name, value);
            return this;
        }

        /**
         * Sets multiple resolved parameter values at once.
         *
         * @param entries the parameter entries
         * @return this builder
         */
        public Builder<U> resolvedIds(Map<String, Object> entries) {
            this.resolvedIds.putAll(entries);
            return this;
        }

        /**
         * Sets the current user.
         *
         * @param currentUser the current user
         * @return this builder
         */
        public Builder<U> currentUser(@Nullable U currentUser) {
            this.currentUser = currentUser;
            return this;
        }

        /**
         * Sets the authorization case label.
         *
         * @param authorizationCase the authorization case
         * @return this builder
         */
        public Builder<U> authorizationCase(@Nullable String authorizationCase) {
            this.authorizationCase = authorizationCase;
            return this;
        }

        /**
         * Sets the intercepted method.
         *
         * @param interceptedMethod the intercepted method
         * @return this builder
         */
        public Builder<U> interceptedMethod(Method interceptedMethod) {
            this.interceptedMethod = interceptedMethod;
            return this;
        }

        /**
         * Builds an immutable {@link AuthorizationContext}.
         *
         * @return a new context instance
         */
        public AuthorizationContext<U> build() {
            return new AuthorizationContext<>(this);
        }
    }
}
