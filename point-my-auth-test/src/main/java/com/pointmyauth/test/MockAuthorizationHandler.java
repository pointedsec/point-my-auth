package com.pointmyauth.test;

import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.exception.AuthorizationException;
import com.pointmyauth.handler.AuthorizationHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A configurable {@link AuthorizationHandler} for unit and integration tests.
 * <p>
 * By default, all invocations are <b>granted</b> (silent return). Tests can
 * configure custom rules to deny specific contexts, verify interactions, or
 * record authorization attempts for later assertions.
 *
 * <p><strong>Usage example:</strong></p>
 * <pre>{@code
 * // Grant everything (default)
 * MockAuthorizationHandler<User> handler = new MockAuthorizationHandler<>();
 *
 * // Deny when user is null
 * handler.denyWhen(ctx -> ctx.getCurrentUser() == null);
 *
 * // Or deny unconditionally
 * handler.denyAll("Not allowed");
 *
 * // Verify calls
 * assertThat(handler.wasInvoked()).isTrue();
 * assertThat(handler.invocationCount()).isEqualTo(2);
 * handler.lastContext(ctx -> {
 *     assertThat(ctx.getCurrentUser()).isNotNull();
 *     assertThat(ctx.getAuthorizationCase()).isEqualTo("DELETE");
 * });
 * }</pre>
 *
 * @param <U> the application's user type
 */
public class MockAuthorizationHandler<U> implements AuthorizationHandler<U> {

    private final List<Predicate<AuthorizationContext<U>>> denyRules = new ArrayList<>();
    private final List<Consumer<AuthorizationContext<U>>> verifications = new ArrayList<>();
    private final List<AuthorizationContext<U>> invocations = new ArrayList<>();
    private boolean denyAll = false;
    private String denyAllMessage = "Access denied by MockAuthorizationHandler";

    /**
     * Adds a rule that denies access when the predicate matches.
     *
     * @param condition the deny condition
     * @return this instance for chaining
     */
    public MockAuthorizationHandler<U> denyWhen(Predicate<AuthorizationContext<U>> condition) {
        denyRules.add(condition);
        return this;
    }

    /**
     * Configures the handler to deny all requests with the given message.
     *
     * @param message the exception message
     * @return this instance for chaining
     */
    public MockAuthorizationHandler<U> denyAll(String message) {
        this.denyAll = true;
        this.denyAllMessage = message;
        return this;
    }

    /**
     * Configures the handler to deny when the current user is {@code null}.
     *
     * @param message the exception message
     * @return this instance for chaining
     */
    public MockAuthorizationHandler<U> denyWhenUnauthenticated(String message) {
        this.denyAllMessage = message;
        this.denyRules.add(ctx -> ctx.getCurrentUser() == null);
        return this;
    }

    /**
     * Configures the handler to deny when the authorization case matches.
     *
     * @param authCase the authorization case to deny
     * @param message  the exception message
     * @return this instance for chaining
     */
    public MockAuthorizationHandler<U> denyWhenCase(String authCase, String message) {
        this.denyRules.add(ctx -> authCase.equals(ctx.getAuthorizationCase()));
        this.denyAllMessage = message;
        return this;
    }

    /**
     * Registers a verification callback that is invoked on every authorization attempt.
     *
     * @param verification the verification consumer
     * @return this instance for chaining
     */
    public MockAuthorizationHandler<U> onInvoke(Consumer<AuthorizationContext<U>> verification) {
        verifications.add(verification);
        return this;
    }

    @Override
    public void authorize(AuthorizationContext<U> context) {
        invocations.add(context);

        for (Consumer<AuthorizationContext<U>> v : verifications) {
            v.accept(context);
        }

        if (denyAll) {
            throw new AuthorizationException(denyAllMessage);
        }

        for (Predicate<AuthorizationContext<U>> rule : denyRules) {
            if (rule.test(context)) {
                throw new AuthorizationException(denyAllMessage);
            }
        }
    }

    /**
     * Returns whether the handler was invoked at least once.
     */
    public boolean wasInvoked() {
        return !invocations.isEmpty();
    }

    /**
     * Returns the total number of invocations.
     */
    public int invocationCount() {
        return invocations.size();
    }

    /**
     * Returns all recorded invocations.
     */
    public List<AuthorizationContext<U>> getInvocations() {
        return List.copyOf(invocations);
    }

    /**
     * Returns the context of the most recent invocation, or empty if never invoked.
     */
    public AuthorizationContext<U> lastContext() {
        if (invocations.isEmpty()) {
            throw new AssertionError("Handler was never invoked");
        }
        return invocations.get(invocations.size() - 1);
    }

    /**
     * Applies an assertion on the most recent invocation context.
     *
     * @param assertion the assertion consumer
     */
    public void lastContext(Consumer<AuthorizationContext<U>> assertion) {
        assertion.accept(lastContext());
    }

    /**
     * Applies an assertion on the context at the given invocation index.
     *
     * @param index    the invocation index (0-based)
     * @param assertion the assertion consumer
     */
    public void contextAt(int index, Consumer<AuthorizationContext<U>> assertion) {
        if (index < 0 || index >= invocations.size()) {
            throw new IndexOutOfBoundsException(
                    "Invocation index " + index + " out of range [0, " + invocations.size() + ")");
        }
        assertion.accept(invocations.get(index));
    }

    /**
     * Resets all state: rules, invocations, verifications.
     */
    public void reset() {
        denyRules.clear();
        verifications.clear();
        invocations.clear();
        denyAll = false;
        denyAllMessage = "Access denied by MockAuthorizationHandler";
    }
}
