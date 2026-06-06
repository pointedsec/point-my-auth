package com.pointmyauth.resolver;

import java.lang.reflect.Method;

/**
 * Strategy interface for resolving parameter values from intercepted method invocations.
 * <p>
 * Each implementation handles a specific naming convention (e.g., {@code @PathVariable},
 * {@code @RequestBody}, HTTP headers) and reports whether it can process a given
 * parameter name via {@link #supports(String)}.
 * <p>
 * Resolvers are typically chained together using {@link CompositeParameterResolver}
 * implementing the Chain of Responsibility pattern.
 */
public interface ParameterResolver {

    /**
     * Attempts to resolve the given parameter name from the intercepted method context.
     *
     * @param paramName the parameter name to resolve (as declared in {@code @AuthorizeEntity#ids()})
     * @param method    the intercepted method
     * @param args      the actual argument values passed to the method invocation
     * @return the resolved value, or {@code null} if not found
     */
    Object resolve(String paramName, Method method, Object[] args);

    /**
     * Determines whether this resolver can handle the given parameter name.
     *
     * @param paramName the parameter name
     * @return {@code true} if this resolver should attempt resolution
     */
    boolean supports(String paramName);
}
