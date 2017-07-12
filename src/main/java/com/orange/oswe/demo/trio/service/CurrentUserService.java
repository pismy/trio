package com.orange.oswe.demo.trio.service;

import com.orange.oswe.demo.trio.domain.User;
import com.orange.oswe.demo.trio.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Created by crhx7117 on 04/07/17.
 */
@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CurrentUserService {
    private User user;

    @Autowired
    private UserRepository userRepository;

    public User getCurrentUser() {
        if (user == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                user = userRepository.findByUsername(authentication.getName());
            }
        }
        return user;
    }
}