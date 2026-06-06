package com.pointmyauth.example;

import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.exception.AuthorizationException;
import com.pointmyauth.handler.AuthorizationHandler;
import org.springframework.stereotype.Component;

/**
 * Tenant-based authorization handler.
 * <p>
 * Checks that the tenant ID from the HTTP header is a valid non-empty string.
 */
@Component
public class TenantAuthorizationHandler implements AuthorizationHandler<PointitUser> {

    @Override
    public void authorize(AuthorizationContext<PointitUser> context) {
        String tenantId = context.getStringId("#header:X-Tenant-Id");
        if (tenantId == null || tenantId.isEmpty()) {
            throw new AuthorizationException("X-Tenant-Id header is required");
        }
    }
}
