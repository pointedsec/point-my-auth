package com.pointmyauth.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthorizationContext")
class AuthorizationContextTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("should build context with resolved IDs")
        void shouldBuildWithResolvedIds() {
            AuthorizationContext<String> ctx = AuthorizationContext.<String>builder()
                    .resolvedId("orderId", 42L)
                    .resolvedId("tenantId", "abc")
                    .build();

            Map<String, Object> ids = ctx.getResolvedIds();
            assertThat(ids).hasSize(2);
            assertThat(ids).containsEntry("orderId", 42L);
            assertThat(ids).containsEntry("tenantId", "abc");
        }

        @Test
        @DisplayName("should accept batch resolved IDs")
        void shouldAcceptBatchResolvedIds() {
            Map<String, Object> entries = Map.of("a", 1L, "b", "two");
            AuthorizationContext<Void> ctx =
                    AuthorizationContext.<Void>builder().resolvedIds(entries).build();

            assertThat(ctx.getResolvedIds()).isEqualTo(entries);
        }

        @Test
        @DisplayName("resolvedIds map should be immutable")
        void resolvedIdsShouldBeImmutable() {
            AuthorizationContext<Void> ctx = AuthorizationContext.<Void>builder()
                    .resolvedId("key", "value")
                    .build();

            assertThatThrownBy(() -> ctx.getResolvedIds().put("newKey", "newValue"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should build context with current user")
        void shouldBuildWithCurrentUser() {
            AuthorizationContext<String> ctx =
                    AuthorizationContext.<String>builder().currentUser("alice").build();

            assertThat(ctx.getCurrentUser()).isEqualTo("alice");
        }

        @Test
        @DisplayName("should build context with null current user")
        void shouldBuildWithNullUser() {
            AuthorizationContext<String> ctx =
                    AuthorizationContext.<String>builder().currentUser(null).build();

            assertThat(ctx.getCurrentUser()).isNull();
        }

        @Test
        @DisplayName("should build context with authorizationCase")
        void shouldBuildWithAuthorizationCase() {
            AuthorizationContext<Void> ctx = AuthorizationContext.<Void>builder()
                    .authorizationCase("DELETE")
                    .build();

            assertThat(ctx.getAuthorizationCase()).isEqualTo("DELETE");
        }

        @Test
        @DisplayName("should build context with interceptedMethod")
        void shouldBuildWithInterceptedMethod() throws Exception {
            Method method = String.class.getMethod("length");
            AuthorizationContext<Void> ctx = AuthorizationContext.<Void>builder()
                    .interceptedMethod(method)
                    .build();

            assertThat(ctx.getInterceptedMethod()).isEqualTo(method);
        }
    }

    @Nested
    @DisplayName("Helper methods")
    class HelperMethods {

        @Test
        @DisplayName("getId should cast to the requested type")
        void getIdShouldCast() {
            AuthorizationContext<Void> ctx =
                    AuthorizationContext.<Void>builder().resolvedId("id", 100L).build();

            assertThat(ctx.getId("id", Long.class)).isEqualTo(100L);
        }

        @Test
        @DisplayName("getId should return null for missing keys")
        void getIdShouldReturnNull() {
            AuthorizationContext<Void> ctx =
                    AuthorizationContext.<Void>builder().build();

            assertThat(ctx.getId("missing", String.class)).isNull();
        }

        @Test
        @DisplayName("getId should throw on type mismatch")
        void getIdShouldThrowOnTypeMismatch() {
            AuthorizationContext<Void> ctx = AuthorizationContext.<Void>builder()
                    .resolvedId("id", "string-value")
                    .build();

            assertThatThrownBy(() -> ctx.getId("id", Long.class)).isInstanceOf(ClassCastException.class);
        }

        @Test
        @DisplayName("getLongId should return Long value")
        void getLongIdShouldReturnLong() {
            AuthorizationContext<Void> ctx = AuthorizationContext.<Void>builder()
                    .resolvedId("orderId", 42L)
                    .build();

            assertThat(ctx.getLongId("orderId")).isEqualTo(42L);
        }

        @Test
        @DisplayName("getLongId should auto-convert Integer to Long")
        void getLongIdShouldConvertInteger() {
            AuthorizationContext<Void> ctx = AuthorizationContext.<Void>builder()
                    .resolvedId("orderId", 42)
                    .build();

            assertThat(ctx.getLongId("orderId")).isEqualTo(42L);
        }

        @Test
        @DisplayName("getLongId should return null for missing key")
        void getLongIdShouldReturnNull() {
            AuthorizationContext<Void> ctx =
                    AuthorizationContext.<Void>builder().build();

            assertThat(ctx.getLongId("missing")).isNull();
        }

        @Test
        @DisplayName("getLongId should throw on incompatible type")
        void getLongIdShouldThrowOnIncompatibleType() {
            AuthorizationContext<Void> ctx = AuthorizationContext.<Void>builder()
                    .resolvedId("id", "not-a-number")
                    .build();

            assertThatThrownBy(() -> ctx.getLongId("id")).isInstanceOf(ClassCastException.class);
        }

        @Test
        @DisplayName("getStringId should return string representation")
        void getStringIdShouldReturnString() {
            AuthorizationContext<Void> ctx = AuthorizationContext.<Void>builder()
                    .resolvedId("tenant", "t-001")
                    .build();

            assertThat(ctx.getStringId("tenant")).isEqualTo("t-001");
        }

        @Test
        @DisplayName("getStringId should call toString on non-String values")
        void getStringIdShouldCallToString() {
            AuthorizationContext<Void> ctx = AuthorizationContext.<Void>builder()
                    .resolvedId("count", 99L)
                    .build();

            assertThat(ctx.getStringId("count")).isEqualTo("99");
        }

        @Test
        @DisplayName("getStringId should return null for missing key")
        void getStringIdShouldReturnNull() {
            AuthorizationContext<Void> ctx =
                    AuthorizationContext.<Void>builder().build();

            assertThat(ctx.getStringId("missing")).isNull();
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class Equality {

        @Test
        @DisplayName("should be equal for same values")
        void shouldBeEqual() {
            AuthorizationContext<String> ctx1 = AuthorizationContext.<String>builder()
                    .resolvedId("id", 1L)
                    .currentUser("alice")
                    .authorizationCase("READ")
                    .build();

            AuthorizationContext<String> ctx2 = AuthorizationContext.<String>builder()
                    .resolvedId("id", 1L)
                    .currentUser("alice")
                    .authorizationCase("READ")
                    .build();

            assertThat(ctx1).isEqualTo(ctx2);
            assertThat(ctx1.hashCode()).isEqualTo(ctx2.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different values")
        void shouldNotBeEqual() {
            AuthorizationContext<String> ctx1 =
                    AuthorizationContext.<String>builder().resolvedId("id", 1L).build();

            AuthorizationContext<String> ctx2 =
                    AuthorizationContext.<String>builder().resolvedId("id", 2L).build();

            assertThat(ctx1).isNotEqualTo(ctx2);
        }
    }
}
