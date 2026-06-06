package com.pointmyauth.test;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composed annotation that registers {@link PointMyAuthTestExtension}.
 * <p>
 * Provides automatic cache clearing, mock handler injection, and
 * assertion helpers for unit tests of authorization logic.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * @PointMyAuthTest
 * class OrderAuthorizationTest {
 *     @Test
 *     void shouldDeny(MockAuthorizationHandler<User> handler) {
 *         handler.denyWhen(ctx -> ctx.getCurrentUser() == null);
 *         handler.authorize(context);
 *         assertThat(handler.wasInvoked()).isTrue();
 *     }
 * }
 * }</pre>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(PointMyAuthTestExtension.class)
public @interface PointMyAuthTest {}
