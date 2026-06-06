package com.pointmyauth.example;

import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.processor.AuthorizationPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example post-processor that logs every authorization decision.
 * <p>
 * Register as a bean to activate:
 * <pre>{@code
 * @Bean
 * public LoggingPostProcessor loggingPostProcessor() {
 *     return new LoggingPostProcessor();
 * }
 * }</pre>
 */
public class LoggingPostProcessor implements AuthorizationPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(LoggingPostProcessor.class);

    @Override
    public void afterAuthorization(AuthorizationContext<?> context, boolean success) {
        String method = context.getInterceptedMethod().getName();
        String authCase = context.getAuthorizationCase();
        Object user = context.getCurrentUser();

        if (success) {
            log.info("[AUTH GRANTED] method={}, case={}, user={}", method, authCase, user);
        } else {
            log.warn("[AUTH DENIED] method={}, case={}, user={}", method, authCase, user);
        }
    }
}
