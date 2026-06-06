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
import org.springframework.expression.spel.support.StandardEvaluationContext;

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
     * With @Repeatable, the compiler wraps even single annotations in the container,
     * so we match on @AuthorizeEntities (the container) which is always present.
     */
    @Around(
            "@annotation(com.pointmyauth.annotation.AuthorizeEntity) || @annotation(com.pointmyauth.annotation.AuthorizeEntities)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        AuthorizeEntities container = method.getAnnotation(AuthorizeEntities.class);
        if (container != null) {
            for (AuthorizeEntity annotation : container.value()) {
                doAuthorize(joinPoint, annotation);
            }
        } else {
            AuthorizeEntity single = method.getAnnotation(AuthorizeEntity.class);
            if (single != null) {
                doAuthorize(joinPoint, single);
            }
        }

        return joinPoint.proceed();
    }

    /**
     * Around advice for {@link ConditionalAuthorize} — evaluates SpEL before handler.
     */
    @Around("@annotation(com.pointmyauth.annotation.ConditionalAuthorize)")
    public Object aroundConditional(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ConditionalAuthorize annotation = method.getAnnotation(ConditionalAuthorize.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        StandardEvaluationContext ctx = new StandardEvaluationContext();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }

        Boolean result = parser.parseExpression(annotation.condition()).getValue(ctx, Boolean.class);
        if (result == null || !result) {
            throw new AuthorizationException("Conditional authorization denied: " + annotation.condition());
        }

        @SuppressWarnings("unchecked")
        AuthorizationHandler<Object> handler = handlerRegistry.resolve(annotation.authorizationHandler());
        handler.authorize(
                AuthorizationContext.builder().interceptedMethod(method).build());

        return joinPoint.proceed();
    }

    private void doAuthorize(ProceedingJoinPoint joinPoint, AuthorizeEntity authorizeEntity) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        @SuppressWarnings("unchecked")
        AuthorizationContext<Object> context = buildContext(authorizeEntity, method, joinPoint.getArgs());
        throwIfCachedDenied(context);

        @SuppressWarnings("unchecked")
        AuthorizationHandler<Object> handler = handlerRegistry.resolve(authorizeEntity.authorizationHandler());
        long start = System.nanoTime();
        boolean success = false;
        String errorMessage = null;

        try {
            handler.authorize(context);
            success = true;
            cacheResult(context, true);
        } catch (AuthorizationException e) {
            errorMessage = e.getMessage();
            cacheResult(context, false);
            throw e;
        } finally {
            firePostActions(context, handler.getClass(), success, System.nanoTime() - start, errorMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private AuthorizationContext<Object> buildContext(
            AuthorizeEntity authorizeEntity, Method method, Object[] args) {
        Map<String, Object> resolvedIds = resolveParameters(authorizeEntity.ids(), method, args);

        Object user = null;
        if (authorizeEntity.includeUser() && currentUserProvider != null) {
            user = currentUserProvider.getCurrentUser();
        }

        String authCase = authorizeEntity.authorizationCase();
        if (authCase.isEmpty()) {
            authCase = null;
        }

        return AuthorizationContext.builder()
                .resolvedIds(resolvedIds)
                .currentUser(user)
                .authorizationCase(authCase)
                .interceptedMethod(method)
                .build();
    }

    private void throwIfCachedDenied(AuthorizationContext<?> context) {
        if (cacheSupport == null) {
            return;
        }
        Boolean cached = cacheSupport.get(context);
        if (cached != null && !cached) {
            throw new AuthorizationException("Access denied (cached)");
        }
    }

    private void cacheResult(AuthorizationContext<?> context, boolean granted) {
        if (cacheSupport != null) {
            cacheSupport.put(context, granted);
        }
    }

    private void firePostActions(
            AuthorizationContext<?> context,
            Class<?> handlerClass,
            boolean success,
            long duration,
            String errorMessage) {
        if (auditListener != null) {
            AuthorizationEvent event = success
                    ? AuthorizationEvent.success(context, handlerClass, duration)
                    : AuthorizationEvent.failure(context, handlerClass, duration, errorMessage);
            auditListener.onEvent(event);
        }
        if (postProcessors != null) {
            for (AuthorizationPostProcessor processor : postProcessors) {
                processor.afterAuthorization(context, success);
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
