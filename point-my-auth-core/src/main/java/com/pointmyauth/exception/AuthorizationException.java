package com.pointmyauth.exception;

/**
 * Runtime exception thrown when an authorization check fails.
 * <p>
 * When used inside a Spring Web application, this exception is automatically
 * converted to an HTTP 403 Forbidden response by the framework's exception
 * handling mechanism.
 * <p>
 * <strong>Typical usage inside a handler:</strong>
 * <pre>{@code
 * public void authorize(AuthorizationContext<User> context) {
 *     if (!hasPermission(context)) {
 *         throw new AuthorizationException("Access denied for user " + context.getCurrentUser());
 *     }
 * }
 * }</pre>
 */
public class AuthorizationException extends RuntimeException {

    /**
     * Creates a new authorization exception with the given message.
     *
     * @param message the detail message
     */
    public AuthorizationException(String message) {
        super(message);
    }

    /**
     * Creates a new authorization exception with the given message and cause.
     *
     * @param message the detail message
     * @param cause   the root cause
     */
    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
