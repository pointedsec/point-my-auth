package com.pointmyauth.example;

import com.pointmyauth.config.PointMyAuthConfigurer;
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
 */
@Configuration
public class AuthConfig {

    private PointitUser currentUser;

    @Bean
    public PointMyAuthConfigurer authConfigurer() {
        return () -> () -> currentUser;
    }

    public void setCurrentUser(PointitUser user) {
        this.currentUser = user;
    }

    public void clearCurrentUser() {
        this.currentUser = null;
    }
}
