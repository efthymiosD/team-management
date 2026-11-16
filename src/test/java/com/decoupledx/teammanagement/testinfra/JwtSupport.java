package com.decoupledx.teammanagement.testinfra;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class JwtSupport {

    private JwtSupport() {
    }

    public static RequestPostProcessor account(String subject) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(subject).claim("email", subject + "@example.com"));
    }

    public static RequestPostProcessor admin(String subject) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(subject).claim("email", subject + "@example.com"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
