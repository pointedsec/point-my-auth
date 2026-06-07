package com.pointmyauth.user;

import jakarta.annotation.Nullable;

/**
 * Functional interface that checks whether the current user has admin privileges.
 * <p>
 * If a bean of this type is registered in the Spring context, the
 * {@link com.pointmyauth.aspect.AuthorizeEntityAspect} will skip authorization
 * for admin users when the {@link com.pointmyauth.annotation.AuthorizeEntity#skipForAdmin()}
 * attribute is {@code true} (the default).
 *
 * @param <U> the application's user type
 */
@FunctionalInterface
public interface AdminChecker<U> {

    /**
     * Returns {@code true} if the given user is an administrator.
     *
     * @param user the current user, may be {@code null} if no user is authenticated
     * @return {@code true} if the user is an admin
     */
    boolean isAdmin(@Nullable U user);
}
