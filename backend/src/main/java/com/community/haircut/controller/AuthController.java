package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.User;
import com.community.haircut.repository.UserRepository;
import com.community.haircut.security.LoginUser;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request.username(), request.password()));
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        LoginUser login = SecurityUtils.get();
        User user = userRepository.findById(login.getUserId()).orElseThrow();
        return Result.ok(authService.toProfile(user));
    }
}
