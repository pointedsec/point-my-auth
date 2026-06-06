package com.pointmyauth.test;

import com.pointmyauth.cache.AuthorizationCacheSupport;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension that manages the point-my-auth test lifecycle.
 * <p>
 * Automatically provides and manages:
 * <ul>
 *     <li>{@link AuthorizationCacheSupport} — cleared before each test</li>
 *     <li>{@link MockAuthorizationHandler} — fresh instance per test</li>
 * </ul>
 * <p>
 * Parameters of supported types in test methods are automatically resolved:
 * <pre>{@code
 * @ExtendWith(PointMyAuthTestExtension.class)
 * class OrderServiceTest {
 *     @Test
 *     void shouldDeny(MockAuthorizationHandler<User> handler, AuthorizationCacheSupport cache) {
 *         handler.denyAll("no access");
 *         // cache is automatically cleared
 *     }
 * }
 * }</pre>
 */
public class PointMyAuthTestExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(PointMyAuthTestExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        getCache(context).clear();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Object handler = getStore(context).get("mockHandler");
        if (handler instanceof MockAuthorizationHandler) {
            ((MockAuthorizationHandler<?>) handler).reset();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return AuthorizationCacheSupport.class.isAssignableFrom(type)
                || MockAuthorizationHandler.class.isAssignableFrom(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();

        if (AuthorizationCacheSupport.class.isAssignableFrom(type)) {
            return getCache(extensionContext);
        }

        if (MockAuthorizationHandler.class.isAssignableFrom(type)) {
            return getHandler(extensionContext);
        }

        throw new IllegalArgumentException("Unsupported parameter type: " + type);
    }

    private AuthorizationCacheSupport getCache(ExtensionContext context) {
        return getStore(context)
                .getOrComputeIfAbsent(
                        "cacheSupport", k -> new AuthorizationCacheSupport(), AuthorizationCacheSupport.class);
    }

    private MockAuthorizationHandler<?> getHandler(ExtensionContext context) {
        return getStore(context)
                .getOrComputeIfAbsent(
                        "mockHandler", k -> new MockAuthorizationHandler<>(), MockAuthorizationHandler.class);
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }
}
