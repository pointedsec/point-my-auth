package com.pointmyauth.audit;

import com.pointmyauth.context.AuthorizationContext;

import java.time.Instant;

/**
 * Immutable event object representing an authorization attempt.
 * <p>
 * Published by {@link AuthorizationAuditListener} after each authorization
 * check. Applications can consume these events for audit logging, metrics,
 * or alerting.
 *
 * @param succeeded whether authorization was granted
 * @param handlerClass the handler class that was invoked
 * @param authorizationCase the authorization case (may be {@code null})
 * @param durationNanos the handler execution time in nanoseconds
 * @param timestamp when the event occurred
 * @param errorMessage the error message if failed (may be {@code null})
 */
public record AuthorizationEvent(
        boolean succeeded,
        Class<?> handlerClass,
        String authorizationCase,
        long durationNanos,
        Instant timestamp,
        String errorMessage) {

    /**
     * Creates a success event.
     *
     * @param context the authorization context
     * @param handlerClass the handler class
     * @param durationNanos execution time in nanoseconds
     * @return a success event
     */
    public static AuthorizationEvent success(
            AuthorizationContext<?> context, Class<?> handlerClass, long durationNanos) {
        return new AuthorizationEvent(
                true, handlerClass, context.getAuthorizationCase(), durationNanos, Instant.now(), null);
    }

    /**
     * Creates a failure event.
     *
     * @param context the authorization context
     * @param handlerClass the handler class
     * @param durationNanos execution time in nanoseconds
     * @param errorMessage the error message
     * @return a failure event
     */
    public static AuthorizationEvent failure(
            AuthorizationContext<?> context, Class<?> handlerClass, long durationNanos, String errorMessage) {
        return new AuthorizationEvent(
                false, handlerClass, context.getAuthorizationCase(), durationNanos, Instant.now(), errorMessage);
    }
}
