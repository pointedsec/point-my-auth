package com.pointmyauth.example;

import com.pointmyauth.audit.AuthorizationAuditListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Example configuration showing how to register audit event consumers.
 * <p>
 * The {@link AuthorizationAuditListener} is an event bus — register
 * consumers to receive every authorization event for logging, alerting,
 * or persistence.
 */
@Configuration
public class AuditConfig {

    private static final Logger log = LoggerFactory.getLogger(AuditConfig.class);

    @Bean
    public AuthorizationAuditListener auditListener() {
        AuthorizationAuditListener listener = new AuthorizationAuditListener();

        // Consumer 1: Log all events
        listener.addEventListener(event -> {
            if (event.succeeded()) {
                log.info(
                        "[AUDIT] GRANTED {} case={} duration={}ns",
                        event.handlerClass().getSimpleName(),
                        event.authorizationCase(),
                        event.durationNanos());
            } else {
                log.warn(
                        "[AUDIT] DENIED {} case={} error={} duration={}ns",
                        event.handlerClass().getSimpleName(),
                        event.authorizationCase(),
                        event.errorMessage(),
                        event.durationNanos());
            }
        });

        // Consumer 2: Alert on failures
        listener.addEventListener(event -> {
            if (!event.succeeded()) {
                log.error(
                        "[ALERT] Authorization FAILED for handler={}",
                        event.handlerClass().getSimpleName());
                // Could trigger PagerDuty, Slack, email, etc.
            }
        });

        return listener;
    }
}
