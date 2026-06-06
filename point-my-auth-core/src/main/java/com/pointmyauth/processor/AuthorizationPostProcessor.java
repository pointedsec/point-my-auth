package com.pointmyauth.processor;

import com.pointmyauth.context.AuthorizationContext;

/**
 * Strategy interface for post-processing after authorization has completed.
 * <p>
 * Implementations are invoked after the {@link com.pointmyauth.handler.AuthorizationHandler}
 * returns (authorization granted) or throws (authorization denied). This enables
 * cross-cutting concerns like audit logging, metrics, and notifications without
 * polluting handler logic.
 * <p>
 * <strong>Example implementation:</strong>
 * <pre>{@code
 * @Component
 * public class MetricsPostProcessor implements AuthorizationPostProcessor {
 *     @Override
 *     public void afterAuthorization(AuthorizationContext<?> context, boolean success) {
 *         metrics.counter("auth." + (success ? "success" : "failure")).increment();
 *     }
 * }
 * }</pre>
 */
public interface AuthorizationPostProcessor {

    /**
     * Called after the authorization handler completes.
     *
     * @param context the authorization context
     * @param success {@code true} if authorization succeeded, {@code false} if denied
     */
    void afterAuthorization(AuthorizationContext<?> context, boolean success);
}
