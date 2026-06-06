package com.pointmyauth.handler;

import com.pointmyauth.context.AuthorizationContext;
import jakarta.annotation.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for {@link AuthorizationHandler} instances.
 * <p>
 * Resolves handlers by first looking up Spring beans, then falling back to
 * reflective instantiation via a no-arg constructor. Resolved instances are
 * cached in a thread-safe {@link ConcurrentHashMap} for subsequent calls.
 * <p>
 * <strong>Typical usage:</strong>
 * <pre>{@code
 * AuthorizationHandlerRegistry registry = ...; // injected as a Spring bean
 * AuthorizationHandler<?> handler = registry.resolve(OrderAuthorizationHandler.class);
 * handler.authorize(context);
 * }</pre>
 *
 * @see AuthorizationHandler
 * @see AuthorizationContext
 */
public class AuthorizationHandlerRegistry implements ApplicationContextAware {

    private final Map<Class<?>, AuthorizationHandler<?>> cache = new ConcurrentHashMap<>();

    @Nullable private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@Nullable ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Resolves an {@link AuthorizationHandler} for the given handler class.
     * <p>
     * Resolution order:
     * <ol>
     *     <li>Return cached instance if previously resolved.</li>
     *     <li>Look up a Spring bean of the handler type.</li>
     *     <li>Instantiate via no-arg constructor (reflection fallback).</li>
     * </ol>
     *
     * @param <U>         the application's user type
     * @param <H>         the handler class
     * @param handlerClass the handler class to resolve
     * @return a resolved handler instance (never {@code null})
     * @throws IllegalStateException if the handler cannot be resolved
     */
    @SuppressWarnings("unchecked")
    public <U, H extends AuthorizationHandler<U>> H resolve(Class<H> handlerClass) {
        AuthorizationHandler<?> cached = cache.get(handlerClass);
        if (cached != null) {
            return (H) cached;
        }
        H handler = resolveInstance(handlerClass);
        AuthorizationHandler<?> existing = cache.putIfAbsent(handlerClass, handler);
        return (H) (existing != null ? existing : handler);
    }

    @SuppressWarnings("unchecked")
    private <U, H extends AuthorizationHandler<U>> H resolveInstance(Class<H> handlerClass) {
        if (applicationContext != null) {
            try {
                H bean = applicationContext.getBean(handlerClass);
                if (bean != null) {
                    return bean;
                }
            } catch (BeansException ignored) {
                // Not a Spring bean — fall back to reflection
            }
        }
        return instantiateViaReflection(handlerClass);
    }

    private <H extends AuthorizationHandler<?>> H instantiateViaReflection(Class<H> handlerClass) {
        if (!AuthorizationHandler.class.isAssignableFrom(handlerClass)) {
            throw new IllegalStateException(
                    "Handler class " + handlerClass.getName() + " does not implement AuthorizationHandler");
        }
        try {
            Constructor<H> constructor = handlerClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Handler class " + handlerClass.getName()
                            + " must have a no-arg constructor. "
                            + "Register it as a Spring bean or provide a no-arg constructor.",
                    e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate handler class " + handlerClass.getName(), e);
        }
    }

    /**
     * Registers a pre-built handler instance for the given handler class.
     *
     * @param <U>           the application's user type
     * @param <H>           the handler class
     * @param handlerClass  the handler class to register for
     * @param handler       the handler instance to cache
     */
    public <U, H extends AuthorizationHandler<U>> void register(Class<H> handlerClass, H handler) {
        cache.put(handlerClass, handler);
    }

    /**
     * Returns {@code true} if a handler has already been resolved for the given class.
     *
     * @param handlerClass the handler class to check
     * @return whether a handler is cached
     */
    public boolean isResolved(Class<? extends AuthorizationHandler<?>> handlerClass) {
        return cache.containsKey(handlerClass);
    }

    /**
     * Clears the handler cache. Useful in tests or when handlers need to be re-resolved.
     */
    public void clearCache() {
        cache.clear();
    }
}
