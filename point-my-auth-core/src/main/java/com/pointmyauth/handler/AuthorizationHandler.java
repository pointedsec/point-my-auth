package com.pointmyauth.handler;

import com.pointmyauth.context.AuthorizationContext;

/**
 * Contract for domain-specific authorization logic.
 * <p>
 * Implementations receive a fully-populated {@link AuthorizationContext} and must either
 * return silently (access granted) or throw an
 * {@link com.pointmyauth.exception.AuthorizationException} (access denied).
 * <p>
 * <strong>Example implementation:</strong>
 * <pre>{@code
 * public class OrderAuthorizationHandler implements AuthorizationHandler<User> {
 *
 *     @Override
 *     public void authorize(AuthorizationContext<User> context) {
 *         Long orderId = context.getLongId("orderId");
 *         User user = context.getCurrentUser();
 *         if (user == null || !orderBelongsToUser(orderId, user.getId())) {
 *             throw new AuthorizationException("User is not the owner of order " + orderId);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @param <U> the application's user type
 */
public interface AuthorizationHandler<U> {

    /**
     * Performs the authorization check.
     *
     * @param context the authorization context containing resolved parameters and user
     * @throws com.pointmyauth.exception.AuthorizationException if authorization fails
     */
    void authorize(AuthorizationContext<U> context);
}
