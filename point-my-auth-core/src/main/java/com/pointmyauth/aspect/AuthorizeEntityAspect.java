package com.pointmyauth.aspect;

import com.pointmyauth.annotation.AuthorizeEntities;
import com.pointmyauth.annotation.AuthorizeEntity;
import com.pointmyauth.annotation.ConditionalAuthorize;
import com.pointmyauth.audit.AuthorizationAuditListener;
import com.pointmyauth.audit.AuthorizationEvent;
import com.pointmyauth.cache.AuthorizationCacheSupport;
import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.exception.AuthorizationException;
import com.pointmyauth.handler.AuthorizationHandler;
import com.pointmyauth.handler.AuthorizationHandlerRegistry;
import com.pointmyauth.processor.AuthorizationPostProcessor;
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
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AOP aspect that intercepts methods annotated with {@link AuthorizeEntity}
 * (or {@link AuthorizeEntities}) and delegates authorization decisions to
 * the specified handler(s).
 * <p>
 * Supports:
 * <ul>
 *     <li>Repeatable annotations — multiple handlers on a single method</li>
 *     <li>{@link ConditionalAuthorize} — SpEL-based conditional authorization</li>
 *     <li>{@link AuthorizationPostProcessor} — post-processing after authorization</li>
 *     <li>{@link AuthorizationAuditListener} — audit event publishing</li>
 *     <li>{@link AuthorizationCacheSupport} — result caching</li>
 * </ul>
 */
@Aspect
@Order(10)
public class AuthorizeEntityAspect {

    private final AuthorizationHandlerRegistry handlerRegistry;

    @Nullable private final CurrentUserProvider<Object> currentUserProvider;

    @Nullable private final List<AuthorizationPostProcessor> postProcessors;

    @Nullable private final AuthorizationAuditListener auditListener;

    @Nullable private final AuthorizationCacheSupport cacheSupport;

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * Creates the aspect with the given dependencies.
     */
    public AuthorizeEntityAspect(
            AuthorizationHandlerRegistry registry,
            @Nullable CurrentUserProvider<Object> currentUserProvider,
            @Nullable List<AuthorizationPostProcessor> postProcessors,
            @Nullable AuthorizationAuditListener auditListener,
            @Nullable AuthorizationCacheSupport cacheSupport) {
        this.handlerRegistry = registry;
        this.currentUserProvider = currentUserProvider;
        this.postProcessors = postProcessors;
        this.auditListener = auditListener;
        this.cacheSupport = cacheSupport;
    }

    /**
     * Creates the aspect with only registry and user provider (backward-compatible).
     */
    public AuthorizeEntityAspect(
            AuthorizationHandlerRegistry registry, @Nullable CurrentUserProvider<Object> currentUserProvider) {
        this(registry, currentUserProvider, null, null, null);
    }

    /**
     * Around advice that intercepts methods annotated with {@link AuthorizeEntity}.
     */
    @Around("@annotation(com.pointmyauth.annotation.AuthorizeEntity)")
    public Object aroundSingle(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AuthorizeEntity annotation = signature.getMethod().getAnnotation(AuthorizeEntity.class);
        return doAuthorize(joinPoint, annotation);
    }

    /**
     * Around advice for repeatable container.
     */
    @Around("@annotation(com.pointmyauth.annotation.AuthorizeEntities)")
    public Object aroundMultiple(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuthorizeEntities container = method.getAnnotation(AuthorizeEntities.class);
        if (container != null) {
            for (AuthorizeEntity annotation : container.value()) {
                doAuthorize(joinPoint, annotation);
            }
        }
        return joinPoint.proceed();
    }

    private Object doAuthorize(ProceedingJoinPoint joinPoint, AuthorizeEntity authorizeEntity) throws Throwable {
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
        Class<?> handlerClass = handler.getClass();

        // Check cache
        if (cacheSupport != null) {
            Boolean cached = cacheSupport.get(context);
            if (cached != null) {
                if (!cached) {
                    throw new AuthorizationException("Access denied (cached)");
                }
                return joinPoint.proceed();
            }
        }

        long start = System.nanoTime();
        boolean success = false;
        String errorMessage = null;

        try {
            handler.authorize(context);
            success = true;

            // Cache result
            if (cacheSupport != null) {
                cacheSupport.put(context, true);
            }

            return joinPoint.proceed();
        } catch (AuthorizationException e) {
            errorMessage = e.getMessage();

            if (cacheSupport != null) {
                cacheSupport.put(context, false);
            }

            throw e;
        } finally {
            long duration = System.nanoTime() - start;

            // Audit
            if (auditListener != null) {
                AuthorizationEvent event = success
                        ? AuthorizationEvent.success(context, handlerClass, duration)
                        : AuthorizationEvent.failure(context, handlerClass, duration, errorMessage);
                auditListener.onEvent(event);
            }

            // Post-processors
            if (postProcessors != null) {
                for (AuthorizationPostProcessor processor : postProcessors) {
                    processor.afterAuthorization(context, success);
                }
            }
        }
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
