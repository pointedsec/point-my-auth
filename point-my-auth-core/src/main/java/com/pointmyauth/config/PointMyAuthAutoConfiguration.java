package com.pointmyauth.config;

import com.pointmyauth.aspect.AuthorizeEntityAspect;
import com.pointmyauth.audit.AuthorizationAuditListener;
import com.pointmyauth.cache.AuthorizationCacheSupport;
import com.pointmyauth.handler.AuthorizationHandlerRegistry;
import com.pointmyauth.processor.AuthorizationPostProcessor;
import com.pointmyauth.user.CurrentUserProvider;
import jakarta.annotation.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Auto-configuration for {@code point-my-auth}.
 * <p>
 * Registers the core beans required for declarative authorization:
 * <ul>
 *     <li>{@link AuthorizationHandlerRegistry} — central handler registry</li>
 *     <li>{@link AuthorizeEntityAspect} — AOP aspect intercepting {@code @AuthorizeEntity}</li>
 *     <li>{@link CurrentUserProvider} — from {@link PointMyAuthConfigurer} if present</li>
 *     <li>{@link AuthorizationAuditListener} — audit event bus</li>
 *     <li>{@link AuthorizationCacheSupport} — result caching</li>
 * </ul>
 * <p>
 * All beans are registered with {@link ConditionalOnMissingBean} to allow
 * overriding in test or production configurations.
 */
@Configuration
public class PointMyAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationHandlerRegistry authorizationHandlerRegistry() {
        return new AuthorizationHandlerRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationAuditListener authorizationAuditListener() {
        return new AuthorizationAuditListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationCacheSupport authorizationCacheSupport() {
        return new AuthorizationCacheSupport();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizeEntityAspect authorizeEntityAspect(
            AuthorizationHandlerRegistry registry,
            @Nullable CurrentUserProvider<Object> currentUserProvider,
            @Nullable List<AuthorizationPostProcessor> postProcessors,
            @Nullable AuthorizationAuditListener auditListener,
            @Nullable AuthorizationCacheSupport cacheSupport) {
        return new AuthorizeEntityAspect(registry, currentUserProvider, postProcessors, auditListener, cacheSupport);
    }

    @Bean
    @ConditionalOnMissingBean
    public CurrentUserProvider<Object> currentUserProvider(@Nullable PointMyAuthConfigurer configurer) {
        if (configurer != null) {
            return configurer.currentUserProvider();
        }
        return () -> null;
    }
}
