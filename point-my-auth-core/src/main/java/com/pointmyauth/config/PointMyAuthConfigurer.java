package com.pointmyauth.config;

import com.pointmyauth.user.AdminChecker;
import com.pointmyauth.user.CurrentUserProvider;
import jakarta.annotation.Nullable;

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
 *     return new PointMyAuthConfigurer() {
 *         public CurrentUserProvider<Object> currentUserProvider() {
 *             return () -> SecurityContextHolder.getContext().getAuthentication().getPrincipal();
 *         }
 *         public AdminChecker<?> adminChecker() {
 *             return user -> ((AppUser) user).isAdmin();
 *         }
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
    CurrentUserProvider<Object> currentUserProvider();

    /**
     * Returns the admin checker for the application, or {@code null} if admin
     * bypass is not enabled. When non-null, the aspect will skip authorization
     * for admin users when {@link com.pointmyauth.annotation.AuthorizeEntity#skipForAdmin()}
     * is {@code true}.
     *
     * @return the admin checker, or {@code null} to disable admin bypass
     */
    @Nullable default AdminChecker<?> adminChecker() {
        return null;
    }
}
