package com.pointmyauth.audit;

import com.pointmyauth.context.AuthorizationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthorizationAuditListener")
class AuthorizationAuditListenerTest {

    private AuthorizationAuditListener listener;

    @BeforeEach
    void setUp() {
        listener = new AuthorizationAuditListener();
    }

    @Test
    @DisplayName("should notify registered listeners")
    void shouldNotifyListeners() throws Exception {
        List<AuthorizationEvent> events = new ArrayList<>();
        listener.addEventListener(events::add);

        Method method = Object.class.getMethod("toString");
        AuthorizationContext<Object> context = AuthorizationContext.builder()
                .interceptedMethod(method)
                .authorizationCase("READ")
                .build();

        AuthorizationEvent event = AuthorizationEvent.success(context, String.class, 1000L);
        listener.onEvent(event);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).succeeded()).isTrue();
        assertThat(events.get(0).authorizationCase()).isEqualTo("READ");
    }

    @Test
    @DisplayName("should notify multiple listeners")
    void shouldNotifyMultiple() {
        List<AuthorizationEvent> events1 = new ArrayList<>();
        List<AuthorizationEvent> events2 = new ArrayList<>();
        listener.addEventListener(events1::add);
        listener.addEventListener(events2::add);

        Method method = null;
        try {
            method = Object.class.getMethod("toString");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        AuthorizationContext<Object> context =
                AuthorizationContext.builder().interceptedMethod(method).build();

        AuthorizationEvent event = AuthorizationEvent.success(context, String.class, 500L);
        listener.onEvent(event);

        assertThat(events1).hasSize(1);
        assertThat(events2).hasSize(1);
    }

    @Test
    @DisplayName("should clear listeners")
    void shouldClear() {
        listener.addEventListener(e -> {});
        listener.addEventListener(e -> {});
        assertThat(listener.getListeners()).hasSize(2);

        listener.clearListeners();
        assertThat(listener.getListeners()).isEmpty();
    }

    @Test
    @DisplayName("AuthorizationEvent should create failure event with message")
    void shouldCreateFailureEvent() throws Exception {
        Method method = Object.class.getMethod("toString");
        AuthorizationContext<Object> context = AuthorizationContext.builder()
                .interceptedMethod(method)
                .authorizationCase("DELETE")
                .build();

        AuthorizationEvent event = AuthorizationEvent.failure(context, String.class, 2000L, "Denied");

        assertThat(event.succeeded()).isFalse();
        assertThat(event.errorMessage()).isEqualTo("Denied");
        assertThat(event.authorizationCase()).isEqualTo("DELETE");
        assertThat(event.durationNanos()).isEqualTo(2000L);
        assertThat(event.timestamp()).isNotNull();
    }
}
