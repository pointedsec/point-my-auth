package com.pointmyauth.resolver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chains multiple {@link ParameterResolver} implementations using the
 * Chain of Responsibility pattern.
 * <p>
 * The composite iterates through its registered resolvers and delegates
 * to the first one that {@linkplain ParameterResolver#supports(String) supports}
 * the given parameter name.
 * <p>
 * <strong>Typical setup:</strong>
 * <pre>{@code
 * CompositeParameterResolver resolver = new CompositeParameterResolver(
 *     new HeaderResolver(request),
 *     new PathVariableResolver(),
 *     new RequestBodyResolver()
 * );
 * }</pre>
 */
public class CompositeParameterResolver implements ParameterResolver {

    private final List<ParameterResolver> resolvers;

    /**
     * Creates a composite resolver from the provided resolvers.
     * Order matters: the first matching resolver wins.
     *
     * @param resolvers the resolvers to chain, in priority order
     */
    public CompositeParameterResolver(ParameterResolver... resolvers) {
        this.resolvers = new ArrayList<>(Arrays.asList(resolvers));
    }

    /**
     * Creates a composite resolver from the provided list of resolvers.
     *
     * @param resolvers the resolvers to chain, in priority order
     */
    public CompositeParameterResolver(List<ParameterResolver> resolvers) {
        this.resolvers = new ArrayList<>(resolvers);
    }

    @Override
    public Object resolve(String paramName, Method method, Object[] args) {
        for (ParameterResolver resolver : resolvers) {
            if (resolver.supports(paramName)) {
                Object result = resolver.resolve(paramName, method, args);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    public boolean supports(String paramName) {
        for (ParameterResolver resolver : resolvers) {
            if (resolver.supports(paramName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an unmodifiable view of the registered resolvers.
     *
     * @return the list of resolvers
     */
    public List<ParameterResolver> getResolvers() {
        return List.copyOf(resolvers);
    }

    /**
     * Registers an additional resolver at the end of the chain.
     *
     * @param resolver the resolver to add
     */
    public void addResolver(ParameterResolver resolver) {
        this.resolvers.add(resolver);
    }
}
