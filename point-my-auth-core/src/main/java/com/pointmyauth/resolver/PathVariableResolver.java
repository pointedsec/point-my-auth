package com.pointmyauth.resolver;

import org.springframework.web.bind.annotation.PathVariable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Resolves parameters annotated with {@link PathVariable @PathVariable}.
 * <p>
 * Matches a parameter name declared in {@code @AuthorizeEntity#ids()} against
 * the {@code value} or {@code name} attribute of the {@code @PathVariable} annotation,
 * or against the Java parameter name if neither is set.
 * <p>
 * <strong>Example:</strong>
 * <pre>{@code
 * @AuthorizeEntity(ids = {"orderId"}, ...)
 * public OrderDto getOrder(@PathVariable Long orderId) { ... }
 * }</pre>
 */
public class PathVariableResolver implements ParameterResolver {

    @Override
    public Object resolve(String paramName, Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PathVariable pathVariable = parameters[i].getDeclaredAnnotation(PathVariable.class);
            if (pathVariable == null) {
                continue;
            }
            String name = resolveName(pathVariable, parameters[i]);
            if (paramName.equals(name)) {
                return args[i];
            }
        }
        return null;
    }

    @Override
    public boolean supports(String paramName) {
        return paramName != null && !paramName.startsWith("#") && !paramName.contains(".");
    }

    private String resolveName(PathVariable pathVariable, Parameter parameter) {
        String value = pathVariable.value();
        String name = pathVariable.name();
        if (value != null && !value.isEmpty()) {
            return value;
        }
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return parameter.getName();
    }
}
