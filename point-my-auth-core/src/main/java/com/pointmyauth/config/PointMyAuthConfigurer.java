package com.pointmyauth.config;

import com.pointmyauth.user.CurrentUserProvider;

/**
 * Interface for customizing the {@code point-myauth} auto-configuration.
 * <p>
 * Implementations should be registered as Spring beans. The auto-configuration
 * will detect them via {@link org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean}
 * and apply the custom settings.
 * <p>
 * <strong>Example:</strong>
 * <pre>{@code
 * @Bean
 * public PointMyAuthConfigurer myAuthConfigurer() {
 *     return () -> {
 *         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 *         return (auth != null && auth.isAuthenticated())
 *                 ? (User) auth.getPrincipal()
 *                 : null;
 *     };
 * }
 * }</pre>
 */
public interface PointMyAuthConfigurer {

    /**
     * Returns the current user provider for the application.
     *
     * @return the current user provider
     */
    CurrentUserProvider<?> currentUserProvider();
}
