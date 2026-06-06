package com.pointmyauth.example;

import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.exception.AuthorizationException;
import com.pointmyauth.handler.AuthorizationHandler;
import org.springframework.stereotype.Component;

@Component
public class OrderAuthorizationHandler implements AuthorizationHandler<User> {

    @Override
    public void authorize(AuthorizationContext<User> context) {
        User user = context.getCurrentUser();
        if (user == null) {
            throw new AuthorizationException("User not authenticated");
        }

        String authCase = context.getAuthorizationCase();
        if ("DELETE".equals(authCase) && !user.admin()) {
            throw new AuthorizationException("Only admins can delete orders");
        }

        Long orderId = context.getLongId("orderId");
        if (orderId != null && orderId < 0) {
            throw new AuthorizationException("Invalid order ID: " + orderId);
        }
    }
}
