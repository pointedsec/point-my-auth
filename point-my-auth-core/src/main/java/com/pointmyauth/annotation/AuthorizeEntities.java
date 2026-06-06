package com.pointmyauth.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeatable {@link AuthorizeEntity} annotations.
 * <p>
 * Allows applying multiple authorization rules to a single method:
 * <pre>{@code
 * @AuthorizeEntities({
 *     @AuthorizeEntity(ids = {"orderId"}, authorizationHandler = OrderHandler.class),
 *     @AuthorizeEntity(ids = {"#header:X-Tenant-Id"}, authorizationHandler = TenantHandler.class)
 * })
 * public OrderDto getOrder(Long orderId) { ... }
 * }</pre>
 *
 * @see AuthorizeEntity
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorizeEntities {

    /**
     * The authorization entity annotations.
     *
     * @return the array of authorization annotations
     */
    AuthorizeEntity[] value();
}
