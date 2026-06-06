package com.pointmyauth.annotation;

import com.pointmyauth.handler.AuthorizationHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditional authorization annotation that evaluates a SpEL expression before
 * invoking the handler.
 * <p>
 * If the expression evaluates to {@code false}, authorization is denied without
 * invoking the handler. If {@code true}, the handler is invoked normally.
 * <p>
 * The SpEL expression has access to method parameters by name:
 * <pre>{@code
 * @ConditionalAuthorize(
 *     condition = "#orderId > 0",
 *     authorizationHandler = OrderHandler.class
 * )
 * public OrderDto getOrder(Long orderId) { ... }
 * }</pre>
 *
 * @see AuthorizationHandler
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConditionalAuthorize {

    /**
     * The SpEL expression to evaluate. If {@code true}, the handler is invoked.
     * <p>
     * Method parameters are available by name with {@code #} prefix.
     *
     * @return the condition expression
     */
    String condition();

    /**
     * The handler class responsible for implementing the authorization logic.
     *
     * @return the authorization handler class
     */
    Class<? extends AuthorizationHandler> authorizationHandler();
}
