package com.pointmyauth.example;

import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.handler.AuthorizationHandler;
import org.springframework.stereotype.Component;

/**
 * Always-allowed authorization handler.
 * <p>
 * Used for endpoints that need authorization interception but no actual
 * access control (e.g., health checks, public listings).
 */
@Component
public class AllowAllAuthorizationHandler implements AuthorizationHandler<Object> {

    @Override
    public void authorize(AuthorizationContext<Object> context) {
        // Always grants access — no-op
    }
}
