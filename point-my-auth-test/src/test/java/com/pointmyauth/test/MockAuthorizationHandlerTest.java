package com.pointmyauth.test;

import com.pointmyauth.context.AuthorizationContext;
import com.pointmyauth.exception.AuthorizationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockAuthorizationHandlerTest {

    private final MockAuthorizationHandler<Object> handler = new MockAuthorizationHandler<>();

    @Test
    @DisplayName("should grant by default")
    void shouldGrantByDefault() {
        AuthorizationContext<Object> ctx =
                AuthorizationTestSupport.context().user("alice").build();

        handler.authorize(ctx);

        assertThat(handler.wasInvoked()).isTrue();
        assertThat(handler.invocationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("should deny all when configured")
    void shouldDenyAll() {
        handler.denyAll("Not allowed");

        assertThatThrownBy(() ->
                        handler.authorize(AuthorizationTestSupport.context().build()))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Not allowed");
    }

    @Test
    @DisplayName("should deny when predicate matches")
    void shouldDenyWhenPredicate() {
        handler.denyWhen(ctx -> ctx.getCurrentUser() == null);

        AuthorizationContext<Object> ctx =
                AuthorizationTestSupport.context().user(null).build();

        assertThatThrownBy(() -> handler.authorize(ctx)).isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("should grant when predicate does not match")
    void shouldGrantWhenPredicateNotMatched() {
        handler.denyWhen(ctx -> ctx.getCurrentUser() == null);

        AuthorizationContext<Object> ctx =
                AuthorizationTestSupport.context().user("alice").build();

        handler.authorize(ctx);
        assertThat(handler.wasInvoked()).isTrue();
    }

    @Test
    @DisplayName("should deny when unauthenticated")
    void shouldDenyWhenUnauthenticated() {
        handler.denyWhenUnauthenticated("Not authenticated");

        AuthorizationContext<Object> ctx =
                AuthorizationTestSupport.context().user(null).build();

        assertThatThrownBy(() -> handler.authorize(ctx))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Not authenticated");
    }

    @Test
    @DisplayName("should record invocations")
    void shouldRecordInvocations() {
        AuthorizationContext<Object> ctx1 =
                AuthorizationTestSupport.context().user("alice").build();
        AuthorizationContext<Object> ctx2 =
                AuthorizationTestSupport.context().user("bob").build();

        handler.authorize(ctx1);
        handler.authorize(ctx2);

        assertThat(handler.invocationCount()).isEqualTo(2);
        assertThat(handler.getInvocations()).hasSize(2);
        assertThat(handler.lastContext().getCurrentUser()).isEqualTo("bob");
    }

    @Test
    @DisplayName("should run verifications on each invocation")
    void shouldRunVerifications() {
        handler.onInvoke(ctx -> {
            if (ctx.getCurrentUser() == null) {
                throw new AssertionError("User should not be null");
            }
        });

        handler.authorize(AuthorizationTestSupport.context().user("alice").build());

        assertThat(handler.wasInvoked()).isTrue();
    }

    @Test
    @DisplayName("should assert on specific invocation index")
    void shouldAssertOnSpecificIndex() {
        handler.authorize(AuthorizationTestSupport.context().user("alice").build());
        handler.authorize(AuthorizationTestSupport.context().user("bob").build());

        handler.contextAt(0, ctx -> assertThat(ctx.getCurrentUser()).isEqualTo("alice"));
        handler.contextAt(1, ctx -> assertThat(ctx.getCurrentUser()).isEqualTo("bob"));
    }

    @Test
    @DisplayName("should reset all state")
    void shouldResetState() {
        handler.denyAll("denied");
        assertThatThrownBy(() -> handler.authorize(
                        AuthorizationTestSupport.context().user("alice").build()))
                .isInstanceOf(AuthorizationException.class);
        assertThat(handler.invocationCount()).isEqualTo(1);

        handler.reset();

        assertThat(handler.wasInvoked()).isFalse();
        assertThat(handler.invocationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("should throw AssertionError on lastContext when never invoked")
    void shouldThrowOnLastContextWhenNeverInvoked() {
        assertThatThrownBy(() -> handler.lastContext()).isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("should throw on contextAt with invalid index")
    void shouldThrowOnInvalidIndex() {
        assertThatThrownBy(() -> handler.contextAt(0, ctx -> {})).isInstanceOf(IndexOutOfBoundsException.class);
    }
}
