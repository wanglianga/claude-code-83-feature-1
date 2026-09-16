package com.community.haircut.security;

import com.community.haircut.common.BizException;
import com.community.haircut.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser user) {
            return user;
        }
        throw new BizException(401, "未登录或登录已过期");
    }

    public static Long uid() {
        return get().getUserId();
    }

    public static void requireRole(Role... roles) {
        LoginUser user = get();
        boolean ok = Arrays.asList(roles).contains(user.getRole());
        if (!ok) {
            throw new BizException(403, "无权限执行该操作");
        }
    }
}
