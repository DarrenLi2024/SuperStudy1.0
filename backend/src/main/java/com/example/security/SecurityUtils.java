package com.example.security;

import com.example.config.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return (LoginUser) authentication.getPrincipal();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static String getCurrentRole() {
        return getCurrentUser().getRole();
    }
}
