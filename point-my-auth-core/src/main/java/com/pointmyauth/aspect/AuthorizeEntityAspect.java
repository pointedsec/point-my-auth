package com.pointmyauth.aspect;

import com.pointmyauth.annotation.AuthorizeEntity;
import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.handler.AuthorizationHandler;
import com.pointmyauth.handler.AuthorizationHandlerRegistry;
import com.pointmyauth.resolver.CompositeParameterResolver;
import com.pointmyauth.resolver.PathVariableResolver;
import com.pointmyauth.resolver.RequestBodyResolver;
import com.pointmyauth.user.CurrentUserProvider;
import jakarta.annotation.Nullable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AOP aspect that intercepts methods annotated with {@link AuthorizeEntity}
 * and delegates authorization decisions to the specified handler.
 * <p>
 * The aspect runs at {@link #HIGHEST_PRECEDENCE} + 10 priority, ensuring
 * it executes early in the aspect chain while still allowing security
 * aspects to run first.
 * <p>
 * <strong>Execution flow:</strong>
 * <ol>
 *     <li>Read {@code @AuthorizeEntity} metadata from the intercepted method.</li>
 *     <li>Resolve parameter values declared in {@code ids[]}.</li>
 *     <li>Optionally fetch the current user via {@link CurrentUserProvider}.</li>
 *     <li>Build an {@link AuthorizationContext}.</li>
 *     <li>Resolve and invoke the {@link AuthorizationHandler}.</li>
 *     <li>Proceed with method execution if authorization passes.</li>
 * </ol>
 *
 * @see AuthorizeEntity
 * @see AuthorizationHandler
 */
@Aspect
@Component
@Order(10)
public class AuthorizeEntityAspect {

    private final AuthorizationHandlerRegistry handlerRegistry;

    @Nullable private final CurrentUserProvider<?> currentUserProvider;

    /**
     * Creates the aspect with the given registry and optional user provider.
     *
     * @param handlerRegistry    the handler registry
     * @param currentUserProvider the current user provider (may be {@code null})
     */
    public AuthorizeEntityAspect(
            AuthorizationHandlerRegistry handlerRegistry, @Nullable CurrentUserProvider<?> currentUserProvider) {
        this.handlerRegistry = handlerRegistry;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Around advice that intercepts methods annotated with {@link AuthorizeEntity}.
     *
     * @param joinPoint the intercepted join point
     * @return the method return value
     * @throws Throwable if the method or handler throws
     */
    @Around("@annotation(authorizeEntity)")
    public Object around(ProceedingJoinPoint joinPoint, AuthorizeEntity authorizeEntity) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Map<String, Object> resolvedIds = resolveParameters(authorizeEntity.ids(), method, joinPoint.getArgs());

        Object user = null;
        if (authorizeEntity.includeUser() && currentUserProvider != null) {
            user = currentUserProvider.getCurrentUser();
        }

        String authCase = authorizeEntity.authorizationCase();
        if (authCase.isEmpty()) {
            authCase = null;
        }

        @SuppressWarnings("unchecked")
        AuthorizationContext<Object> context = AuthorizationContext.builder()
                .resolvedIds(resolvedIds)
                .currentUser(user)
                .authorizationCase(authCase)
                .interceptedMethod(method)
                .build();

        @SuppressWarnings("unchecked")
        AuthorizationHandler<Object> handler = handlerRegistry.resolve(authorizeEntity.authorizationHandler());
        handler.authorize(context);

        return joinPoint.proceed();
    }

    private Map<String, Object> resolveParameters(String[] ids, Method method, Object[] args) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        if (ids.length == 0) {
            return resolved;
        }

        CompositeParameterResolver resolver =
                new CompositeParameterResolver(new PathVariableResolver(), new RequestBodyResolver());

        for (String id : ids) {
            Object value = resolver.resolve(id, method, args);
            resolved.put(id, value);
        }

        return resolved;
    }
}
