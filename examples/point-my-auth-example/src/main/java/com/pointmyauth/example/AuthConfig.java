package com.pointmyauth.example;

import com.pointmyauth.config.PointMyAuthConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

    private User currentUser;

    @Bean
    public PointMyAuthConfigurer authConfigurer() {
        return () -> () -> currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void clearCurrentUser() {
        this.currentUser = null;
    }
}
