package com.pointmyauth.adminbypass;

import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.exception.AuthorizationException;
import com.pointmyauth.handler.AuthorizationHandler;

class DenyHandler implements AuthorizationHandler<Object> {
    @Override
    public void authorize(AuthorizationContext<Object> context) {
        throw new AuthorizationException("Access denied");
    }
}
