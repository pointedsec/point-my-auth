package com.pointmyauth.annotation;

import com.pointmyauth.handler.AuthorizationHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
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
 * <strong>Usage example:</strong>
 * <pre>{@code
 * @AuthorizeEntity(
 *     ids = {"orderId"},
 *     includeUser = true,
 *     authorizationHandler = OrderAuthorizationHandler.class
 * )
 * public OrderDto getOrder(Long orderId) {
 *     return mapper.toDto(orderRepo.findById(orderId).orElseThrow());
 * }
 * }</pre>
 *
 * @see AuthorizationHandler
 * @see com.pointmyauth.context.AuthorizationContext
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorizeEntity {

    /**
     * Names of the parameters to resolve from the intercepted method.
     * <p>
     * Supported resolution strategies:
     * <ul>
     *     <li>{@code "orderId"} — resolves a {@code @PathVariable} parameter named {@code orderId}.</li>
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
     * Whether to include an explicit authorization case label in the context.
     * <p>
     * When enabled, the value is taken from {@link #authorizationCase()}. This allows a
     * single handler to differentiate between create, read, update, and delete operations.
     *
     * @return {@code true} to include an authorization case string
     */
    boolean includeAuthorizationCase() default false;

    /**
     * The authorization case label (e.g., {@code "CREATE"}, {@code "DELETE"}).
     * Only effective when {@link #includeAuthorizationCase()} is {@code true}.
     *
     * @return the authorization case string
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
}
