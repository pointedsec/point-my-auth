package com.pointmyauth.example;

import com.pointmyauth.annotation.ConditionalAuthorize;
import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.handler.AuthorizationHandler;
import org.springframework.stereotype.Component;

/**
 * Example handler for {@link ConditionalAuthorize} demos.
 */
@Component
public class ConditionalOrderHandler implements AuthorizationHandler<Object> {

    @Override
    public void authorize(AuthorizationContext<Object> context) {
        // Always granted if the SpEL condition passed
    }
}
