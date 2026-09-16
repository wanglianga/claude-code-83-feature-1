package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.User;
import com.community.haircut.enums.Role;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<List<User>> list() {
        SecurityUtils.requireRole(Role.ADMIN);
        List<User> users = userService.list();
        users.forEach(u -> u.setPassword(null));
        return Result.ok(users);
    }

    public record CreateRequest(@NotBlank String username, @NotBlank String password,
                                @NotBlank String realName, String phone, @NotNull String role) {
    }

    @PostMapping
    public Result<User> create(@RequestBody CreateRequest request) {
        SecurityUtils.requireRole(Role.ADMIN);
        User user = userService.create(request.username(), request.password(), request.realName(),
                request.phone(), Role.valueOf(request.role()));
        user.setPassword(null);
        return Result.ok(user);
    }

    @PostMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        SecurityUtils.requireRole(Role.ADMIN);
        userService.changeStatus(id, status);
        return Result.ok();
    }

    public record ResetRequest(@NotBlank String password) {
    }

    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody ResetRequest request) {
        SecurityUtils.requireRole(Role.ADMIN);
        userService.resetPassword(id, request.password());
        return Result.ok();
    }
}
