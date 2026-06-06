package com.pointmyauth.cache;

import com.pointmyauth.context.AuthorizationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthorizationCacheSupport")
class AuthorizationCacheSupportTest {

    private AuthorizationCacheSupport cache;

    @BeforeEach
    void setUp() {
        cache = new AuthorizationCacheSupport();
    }

    @Test
    @DisplayName("should cache and retrieve result")
    void shouldCacheAndRetrieve() throws Exception {
        Method method = Object.class.getMethod("toString");
        AuthorizationContext<Object> context =
                AuthorizationContext.builder().interceptedMethod(method).build();

        cache.put(context, true);
        Boolean result = cache.get(context);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return null for uncached context")
    void shouldReturnNullForUncached() throws Exception {
        Method method = Object.class.getMethod("toString");
        AuthorizationContext<Object> context =
                AuthorizationContext.builder().interceptedMethod(method).build();

        Boolean result = cache.get(context);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should cache denial")
    void shouldCacheDenial() throws Exception {
        Method method = Object.class.getMethod("toString");
        AuthorizationContext<Object> context =
                AuthorizationContext.builder().interceptedMethod(method).build();

        cache.put(context, false);
        Boolean result = cache.get(context);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should clear cache")
    void shouldClear() throws Exception {
        Method method = Object.class.getMethod("toString");
        AuthorizationContext<Object> context =
                AuthorizationContext.builder().interceptedMethod(method).build();

        cache.put(context, true);
        assertThat(cache.size()).isEqualTo(1);

        cache.clear();
        assertThat(cache.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("should expire entries after TTL")
    void shouldExpireAfterTtl() throws Exception {
        AuthorizationCacheSupport shortTtl = new AuthorizationCacheSupport(1); // 1ms TTL

        Method method = Object.class.getMethod("toString");
        AuthorizationContext<Object> context =
                AuthorizationContext.builder().interceptedMethod(method).build();

        shortTtl.put(context, true);
        assertThat(shortTtl.get(context)).isTrue();

        // Wait for expiration
        try {
            sleep(10, 10);
        } catch (InterruptedException ignored) {
            // No Operation
        }

        assertThat(shortTtl.get(context)).isNull();
    }

    @Test
    @DisplayName("should update TTL")
    void shouldUpdateTtl() throws Exception {
        cache.setTtlMillis(5000);
        Method method = Object.class.getMethod("toString");
        AuthorizationContext<Object> context =
                AuthorizationContext.builder().interceptedMethod(method).build();

        cache.put(context, true);
        // Should not expire within 5 seconds
        assertThat(cache.get(context)).isTrue();
    }

    private void sleep(int iCount, int iDelay) throws ExecutionException, InterruptedException {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        List<Future<Integer>> futureList = new ArrayList<>(iCount);
        for (int i=0; i < iCount; i++) {
            int j = i;
            futureList.add(scheduledExecutorService.schedule(() -> j, iDelay, TimeUnit.SECONDS));
        }
        for (Future<Integer> e : futureList) {
            e.get();
        }
    }
}
