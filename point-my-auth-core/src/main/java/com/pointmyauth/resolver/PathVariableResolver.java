package com.pointmyauth.resolver;

import org.springframework.web.bind.annotation.PathVariable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Resolves any method parameter by name, supporting all Java types
 * ({@code String}, {@code Integer}, {@code Long}, {@code List}, POJOs, etc.).
 * <p>
 * Resolution strategy:
 * <ol>
 *     <li>If the parameter is annotated with {@link PathVariable @PathVariable},
 *         the annotation's {@code value} or {@code name} attribute takes priority.</li>
 *     <li>Otherwise, the parameter is matched by its Java parameter name.</li>
 * </ol>
 * <p>
 * <strong>Examples:</strong>
 * <pre>{@code
 * // Matched by @PathVariable("orderId") annotation value
 * public OrderDto getOrder(@PathVariable("orderId") Long id) { ... }
 *
 * // Matched by Java parameter name "orderId"
 * public OrderDto getOrder(Long orderId) { ... }
 *
 * // Matched by Java parameter name "username"
 * public UserDto getUser(String username) { ... }
 *
 * // Matched by Java parameter name "itemIds"
 * public void process(List<Long> itemIds) { ... }
 * }</pre>
 */
public class PathVariableResolver implements ParameterResolver {

    @Override
    public Object resolve(String paramName, Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            PathVariable pathVariable = parameters[i].getDeclaredAnnotation(PathVariable.class);
            if (pathVariable != null) {
                String name = resolveName(pathVariable, parameters[i]);
                if (paramName.equals(name)) {
                    return args[i];
                }
            }
        }
        for (int i = 0; i < parameters.length; i++) {
            if (paramName.equals(parameters[i].getName())) {
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
