package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.User;
import com.community.haircut.repository.UserRepository;
import com.community.haircut.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException("用户名或密码错误"));
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException("账号已被停用，请联系管理员");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        String token = jwtUtil.generate(user);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", toProfile(user));
        return result;
    }

    public Map<String, Object> toProfile(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("phone", user.getPhone());
        profile.put("role", user.getRole().name());
        return profile;
    }
}
