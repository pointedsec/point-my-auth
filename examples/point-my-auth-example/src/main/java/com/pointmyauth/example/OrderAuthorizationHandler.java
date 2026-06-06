package com.pointmyauth.example;

import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.exception.AuthorizationException;
import com.pointmyauth.handler.AuthorizationHandler;
import org.springframework.stereotype.Component;

/**
 * Authorization handler for Order operations.
 * <p>
 * Notice the generic parameter is {@link PointitUser} — the handler
 * receives exactly the same user type that was configured via
 * {@link com.pointmyauth.config.PointMyAuthConfigurer}. The framework
 * is fully generic; your handlers work with whatever user type you
 * provide.
 */
@Component
public class OrderAuthorizationHandler implements AuthorizationHandler<PointitUser> {

    @Override
    public void authorize(AuthorizationContext<PointitUser> context) {
        PointitUser user = context.getCurrentUser();
        if (user == null) {
            throw new AuthorizationException("User not authenticated");
        }

        String authCase = context.getAuthorizationCase();
        if ("DELETE".equals(authCase) && !user.isAdmin()) {
            throw new AuthorizationException("Only admins can delete orders");
        }

        Long orderId = context.getLongId("orderId");
        if (orderId != null && orderId < 0) {
            throw new AuthorizationException("Invalid order ID: " + orderId);
        }
    }
}
