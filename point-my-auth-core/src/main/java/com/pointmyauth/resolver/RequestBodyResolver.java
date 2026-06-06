package com.pointmyauth.resolver;

import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Resolves parameters from the {@code @RequestBody} object or its fields.
 * <p>
 * Supports two patterns:
 * <ul>
 *     <li>{@code "dto"} — resolves the entire {@code @RequestBody} object.</li>
 *     <li>{@code "dto.fieldName"} — resolves a specific field inside the body (max depth 2).</li>
 * </ul>
 * <p>
 * <strong>Example:</strong>
 * <pre>{@code
 * @AuthorizeEntity(ids = {"request.companyId"}, ...)
 * public void update(@RequestBody UpdateRequest request) { ... }
 * }</pre>
 */
public class RequestBodyResolver implements ParameterResolver {

    private static final int MAX_FIELD_DEPTH = 2;

    @Override
    public Object resolve(String paramName, Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getDeclaredAnnotation(RequestBody.class) == null) {
                continue;
            }
            String paramJavaName = parameters[i].getName();

            if (paramName.equals(paramJavaName)) {
                return args[i];
            }

            if (paramName.startsWith(paramJavaName + ".")) {
                String fieldPath = paramName.substring(paramJavaName.length() + 1);
                return resolveField(args[i], fieldPath);
            }
        }
        return null;
    }

    @Override
    public boolean supports(String paramName) {
        return paramName != null && !paramName.startsWith("#") && paramName.contains(".");
    }

    private Object resolveField(Object target, String fieldPath) {
        if (target == null) {
            return null;
        }
        String[] parts = fieldPath.split("\\.");
        if (parts.length > MAX_FIELD_DEPTH) {
            return null;
        }
        Object current = target;
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            current = getFieldValue(current, part);
        }
        return current;
    }

    private Object getFieldValue(Object target, String fieldName) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }
}
