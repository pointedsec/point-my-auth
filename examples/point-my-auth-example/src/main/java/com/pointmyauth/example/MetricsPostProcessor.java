package com.pointmyauth.example;

import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.processor.AuthorizationPostProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Example post-processor that counts authorization attempts per handler.
 * <p>
 * Register as a bean to activate:
 * <pre>{@code
 * @Bean
 * public MetricsPostProcessor metricsPostProcessor() {
 *     return new MetricsPostProcessor();
 * }
 * }</pre>
 */
public class MetricsPostProcessor implements AuthorizationPostProcessor {

    private final Map<String, AtomicLong> successCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> failureCount = new ConcurrentHashMap<>();

    @Override
    public void afterAuthorization(AuthorizationContext<?> context, boolean success) {
        String handler = context.getInterceptedMethod().getName();

        if (success) {
            successCount.computeIfAbsent(handler, k -> new AtomicLong()).incrementAndGet();
        } else {
            failureCount.computeIfAbsent(handler, k -> new AtomicLong()).incrementAndGet();
        }
    }

    public long getSuccessCount(String handler) {
        AtomicLong count = successCount.get(handler);
        return count != null ? count.get() : 0;
    }

    public long getFailureCount(String handler) {
        AtomicLong count = failureCount.get(handler);
        return count != null ? count.get() : 0;
    }

    public void reset() {
        successCount.clear();
        failureCount.clear();
    }
}
