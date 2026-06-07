package com.pointmyauth.example;

import com.pointmyauth.config.PointMyAuthConfigurer;
import com.pointmyauth.user.AdminChecker;
import com.pointmyauth.user.CurrentUserProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Example configuration showing how to wire a custom user type
 * (here {@link PointitUser}) into the point-my-auth framework.
 * <p>
 * The key is that {@link PointMyAuthConfigurer#currentUserProvider()}
 * returns a {@link CurrentUserProvider} parameterized with whatever
 * user type your application uses — it does not have to be a
 * specific class.
 * <p>
 * {@link PointMyAuthConfigurer#adminChecker()} is also overridden to enable
 * admin bypass: when the current user has {@code admin == true}, all
 * {@code @AuthorizeEntity} annotations with {@code skipForAdmin = true}
 * (the default) will be skipped.
 */
@Configuration
public class AuthConfig {

    private PointitUser currentUser;

    @Bean
    public PointMyAuthConfigurer authConfigurer() {
        return new PointMyAuthConfigurer() {
            @Override
            public CurrentUserProvider<Object> currentUserProvider() {
                return () -> currentUser;
            }

            @Override
            public AdminChecker<Object> adminChecker() {
                return user -> user instanceof PointitUser pu && pu.admin();
            }
        };
    }

    public void setCurrentUser(PointitUser user) {
        this.currentUser = user;
    }

    public void clearCurrentUser() {
        this.currentUser = null;
    }
}
