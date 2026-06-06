package com.pointmyauth.config;

import com.pointmyauth.aspect.AuthorizeEntityAspect;
import com.pointmyauth.handler.AuthorizationHandlerRegistry;
import com.pointmyauth.user.CurrentUserProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PointMyAuthAutoConfiguration")
class PointMyAuthAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PointMyAuthAutoConfiguration.class));

    @Test
    @DisplayName("should register all beans when no custom configurer")
    void shouldRegisterAllBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AuthorizationHandlerRegistry.class);
            assertThat(context).hasSingleBean(AuthorizeEntityAspect.class);
            assertThat(context).hasSingleBean(CurrentUserProvider.class);
        });
    }

    @Test
    @DisplayName("should use custom configurer when provided")
    void shouldUseCustomConfigurer() {
        contextRunner
                .withBean(PointMyAuthConfigurer.class, () -> (PointMyAuthConfigurer) () -> () -> "custom-user")
                .run(context -> {
                    CurrentUserProvider<Object> provider = context.getBean(CurrentUserProvider.class);
                    assertThat(provider.getCurrentUser()).isEqualTo("custom-user");
                });
    }

    @Test
    @DisplayName("should allow overriding registry bean")
    void shouldAllowOverridingRegistry() {
        AuthorizationHandlerRegistry custom = new AuthorizationHandlerRegistry();
        contextRunner.withBean(AuthorizationHandlerRegistry.class, () -> custom).run(context -> {
            assertThat(context.getBean(AuthorizationHandlerRegistry.class)).isSameAs(custom);
        });
    }
}
