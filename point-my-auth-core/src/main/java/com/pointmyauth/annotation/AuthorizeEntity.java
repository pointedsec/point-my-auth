package com.pointmyauth.annotation;

import com.pointmyauth.handler.AuthorizationHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative annotation that triggers authorization interception on a Spring-managed method.
 * <p>
 * The {@link com.pointmyauth.aspect.AuthorizeEntityAspect} reads this annotation at runtime,
 * resolves the declared {@link #ids()}, optionally fetches the current user via
 * {@link com.pointmyauth.user.CurrentUserProvider}, builds an
 * {@link com.pointmyauth.context.AuthorizationContext}, and delegates the authorization
 * decision to the specified {@link #authorizationHandler()}.
 * <p>
 * This annotation is repeatable — multiple instances can be stacked on the same method
 * using the {@link AuthorizeEntities} container.
 *
 * @see AuthorizationHandler
 * @see com.pointmyauth.context.AuthorizationContext
 * @see AuthorizeEntities
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(AuthorizeEntities.class)
public @interface AuthorizeEntity {

    /**
     * Names of the parameters to resolve from the intercepted method.
     * <p>
     * Supported resolution strategies:
     * <ul>
     *     <li>{@code "orderId"} — resolves a method parameter named {@code orderId}.</li>
     *     <li>{@code "requestDto"} — resolves the entire {@code @RequestBody} object.</li>
     *     <li>{@code "requestDto.companyId"} — navigates field {@code companyId} inside the {@code @RequestBody}.</li>
     *     <li>{@code "#header:X-Tenant-Id"} — resolves the HTTP header {@code X-Tenant-Id}.</li>
     * </ul>
     *
     * @return the parameter names to resolve; defaults to an empty array
     */
    String[] ids() default {};

    /**
     * Whether to include the current user (obtained from {@link com.pointmyauth.user.CurrentUserProvider})
     * in the {@link com.pointmyauth.context.AuthorizationContext}.
     *
     * @return {@code true} if the current user should be injected into the context
     */
    boolean includeUser() default true;

    /**
     * The authorization case label (e.g., {@code "CREATE"}, {@code "DELETE"}).
     * <p>
     * This value is always passed to the handler via the {@link com.pointmyauth.context.AuthorizationContext},
     * allowing a single handler to differentiate between create, read, update, and delete
     * operations. If left empty, the handler receives {@code null}.
     *
     * @return the authorization case string (defaults to empty)
     */
    String authorizationCase() default "";

    /**
     * The handler class responsible for implementing the authorization logic.
     * <p>
     * Must implement {@link AuthorizationHandler}. The handler is resolved via the
     * {@link com.pointmyauth.handler.AuthorizationHandlerRegistry} (Spring bean first,
     * reflection fallback).
     *
     * @return the authorization handler class
     */
    Class<? extends AuthorizationHandler> authorizationHandler();

    /**
     * Whether to skip authorization for admin users.
     * <p>
     * When {@code true} (the default) and a {@link com.pointmyauth.user.AdminChecker}
     * bean is registered, the aspect will skip the handler entirely for admin users.
     * Set to {@code false} to always run the handler regardless of admin status.
     *
     * @return {@code true} if admin users should bypass authorization
     */
    boolean skipForAdmin() default true;
}
