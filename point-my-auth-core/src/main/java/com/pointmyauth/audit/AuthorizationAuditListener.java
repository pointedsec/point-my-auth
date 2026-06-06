package com.pointmyauth.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Listener that collects {@link AuthorizationEvent}s and notifies registered consumers.
 * <p>
 * Acts as an event bus for authorization audit events. Applications register
 * consumers that receive each event for logging, metrics, or alerting.
 * <p>
 * <strong>Example usage:</strong>
 * <pre>{@code
 * @Bean
 * public AuthorizationAuditListener auditListener() {
 *     AuthorizationAuditListener listener = new AuthorizationAuditListener();
 *     listener.addEventListener(event -> log.info("Auth {}: {}",
 *             event.succeeded() ? "GRANTED" : "DENIED",
 *             event.handlerClass().getSimpleName()));
 *     return listener;
 * }
 * }</pre>
 */
public class AuthorizationAuditListener {

    private final List<Consumer<AuthorizationEvent>> listeners = new ArrayList<>();

    /**
     * Registers an event consumer to receive all authorization events.
     *
     * @param listener the consumer to register
     */
    public void addEventListener(Consumer<AuthorizationEvent> listener) {
        listeners.add(listener);
    }

    /**
     * Publishes an event to all registered consumers.
     *
     * @param event the authorization event
     */
    public void onEvent(AuthorizationEvent event) {
        for (Consumer<AuthorizationEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    /**
     * Returns an unmodifiable view of registered listeners.
     *
     * @return the list of registered listeners
     */
    public List<Consumer<AuthorizationEvent>> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /**
     * Removes all registered listeners.
     */
    public void clearListeners() {
        listeners.clear();
    }
}
