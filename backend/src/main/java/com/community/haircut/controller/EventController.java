package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.CommunityEvent;
import com.community.haircut.entity.ServiceOrder;
import com.community.haircut.enums.Role;
import com.community.haircut.security.LoginUser;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.EventService;
import com.community.haircut.service.OrderService;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(eventService.list());
    }

    @PostMapping
    public Result<CommunityEvent> create(@RequestBody CommunityEvent event) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(eventService.create(event));
    }

    @PostMapping("/{id}/status")
    public Result<CommunityEvent> updateStatus(@PathVariable Long id, @RequestParam String status) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(eventService.updateStatus(id, status));
    }

    public record BookRequest(@NotNull Long elderId) {
    }

    @PostMapping("/{id}/book")
    public Result<ServiceOrder> book(@PathVariable Long id, @RequestBody BookRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        LoginUser u = SecurityUtils.get();
        return Result.ok(eventService.book(id, request.elderId(),
                new OrderService.LoginUserInfo(u.getUserId(), u.getRealName(), u.getRole())));
    }
}
