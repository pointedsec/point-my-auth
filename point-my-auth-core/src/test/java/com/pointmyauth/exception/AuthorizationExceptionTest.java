package com.pointmyauth.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthorizationException")
class AuthorizationExceptionTest {

    @Test
    @DisplayName("should store message")
    void shouldStoreMessage() {
        AuthorizationException ex = new AuthorizationException("Access denied");

        assertThat(ex.getMessage()).isEqualTo("Access denied");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("should store message and cause")
    void shouldStoreMessageAndCause() {
        Throwable cause = new IllegalArgumentException("bad arg");
        AuthorizationException ex = new AuthorizationException("Access denied", cause);

        assertThat(ex.getMessage()).isEqualTo("Access denied");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("message-only constructor should have null cause")
    void messageOnlyShouldHaveNullCause() {
        AuthorizationException ex = new AuthorizationException("Denied");

        assertThat(ex.getCause()).isNull();
    }
}
