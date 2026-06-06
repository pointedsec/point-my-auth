package com.pointmyauth.example;

/**
 * Example application user type.
 * In a real application this would typically extend from a base
 * User entity or implement a security principal interface.
 *
 * @param id       the user ID
 * @param name     the display name
 * @param email    the email address
 * @param role     the user role (e.g. "ADMIN", "USER")
 */
public record PointitUser(Long id, String name, String email, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
