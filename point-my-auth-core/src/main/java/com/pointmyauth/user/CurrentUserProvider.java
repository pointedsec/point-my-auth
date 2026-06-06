package com.pointmyauth.user;

/**
 * Functional interface for providing the currently authenticated user.
 * <p>
 * Integrators must supply an implementation (e.g., via a Spring {@code @Bean}) that
 * extracts the user from the security context, JWT token, or session.
 * <p>
 * <strong>Example implementation:</strong>
 * <pre>{@code
 * @Bean
 * public CurrentUserProvider<User> currentUserProvider() {
 *     return () -> {
 *         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *         if (auth == null || !auth.isAuthenticated()) return null;
 *         return (User) auth.getPrincipal();
 *     };
 * }
 * }</pre>
 *
 * @param <U> the application's user type
 */
@FunctionalInterface
public interface CurrentUserProvider<U> {

    /**
     * Returns the current user, or {@code null} if unauthenticated.
     *
     * @return the current user, or {@code null}
     */
    U getCurrentUser();
}
