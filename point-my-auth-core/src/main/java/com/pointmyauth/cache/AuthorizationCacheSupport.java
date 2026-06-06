package com.pointmyauth.cache;

import com.pointmyauth.context.AuthorizationContext;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache support for authorization handler results.
 * <p>
 * Caches the outcome (granted/denied) of handler invocations to avoid
 * redundant authorization checks for the same context parameters.
 * <p>
 * <strong>Thread-safe:</strong> Uses {@link ConcurrentHashMap} for concurrent access.
 * <p>
 * <strong>Example usage:</strong>
 * <pre>{@code
 * AuthorizationCacheSupport cache = new AuthorizationCacheSupport();
 *
 * // Before handler invocation
 * Boolean cached = cache.get(context);
 * if (cached != null) {
 *     if (!cached) throw new AuthorizationException("Denied (cached)");
 *     return; // already granted
 * }
 *
 * // After handler invocation
 * cache.put(context, true); // granted
 * }</pre>
 */
public class AuthorizationCacheSupport {

    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();
    private volatile long ttlMillis = 60_000; // default 1 minute
    private final Map<String, Long> timestamps = new ConcurrentHashMap<>();

    /**
     * Creates a cache with default TTL (1 minute).
     */
    public AuthorizationCacheSupport() {}

    /**
     * Creates a cache with the specified TTL.
     *
     * @param ttlMillis time-to-live in milliseconds
     */
    public AuthorizationCacheSupport(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    /**
     * Returns the cached authorization result for the given context, or {@code null}
     * if not cached or expired.
     *
     * @param context the authorization context
     * @return the cached result ({@code true} = granted, {@code false} = denied), or {@code null}
     */
    @Nullable public Boolean get(AuthorizationContext<?> context) {
        String key = buildKey(context);
        Long timestamp = timestamps.get(key);
        if (timestamp != null && System.currentTimeMillis() - timestamp > ttlMillis) {
            cache.remove(key);
            timestamps.remove(key);
            return null;
        }
        return cache.get(key);
    }

    /**
     * Caches the authorization result for the given context.
     *
     * @param context the authorization context
     * @param granted whether authorization was granted
     */
    public void put(AuthorizationContext<?> context, boolean granted) {
        String key = buildKey(context);
        cache.put(key, granted);
        timestamps.put(key, System.currentTimeMillis());
    }

    /**
     * Evicts all cached entries.
     */
    public void clear() {
        cache.clear();
        timestamps.clear();
    }

    /**
     * Returns the number of cached entries.
     *
     * @return the cache size
     */
    public int size() {
        return cache.size();
    }

    /**
     * Updates the time-to-live for cache entries.
     *
     * @param ttlMillis the new TTL in milliseconds
     */
    public void setTtlMillis(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    private String buildKey(AuthorizationContext<?> context) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getInterceptedMethod().getName());
        sb.append(":");
        context.getResolvedIds()
                .forEach((k, v) -> sb.append(k).append("=").append(v).append(","));
        if (context.getAuthorizationCase() != null) {
            sb.append("case=").append(context.getAuthorizationCase()).append(",");
        }
        Object user = context.getCurrentUser();
        if (user != null) {
            sb.append("user=").append(user);
        }
        return sb.toString();
    }
}
