package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.Elder;
import com.community.haircut.entity.PatrolRecord;
import com.community.haircut.entity.ServiceOrder;
import com.community.haircut.enums.Role;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.ElderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/elders")
public class ElderController {

    private final ElderService elderService;

    public ElderController(ElderService elderService) {
        this.elderService = elderService;
    }

    @GetMapping
    public Result<List<Elder>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(elderService.list(keyword));
    }

    @GetMapping("/{id}")
    public Result<Elder> get(@PathVariable Long id) {
        return Result.ok(elderService.get(id));
    }

    @PostMapping
    public Result<Elder> create(@RequestBody Elder elder) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(elderService.create(elder, SecurityUtils.uid()));
    }

    @PutMapping("/{id}")
    public Result<Elder> update(@PathVariable Long id, @RequestBody Elder elder) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(elderService.update(id, elder));
    }

    @GetMapping("/{id}/orders")
    public Result<List<ServiceOrder>> orders(@PathVariable Long id) {
        return Result.ok(elderService.elderOrders(id));
    }

    @GetMapping("/birthdays")
    public Result<List<Map<String, Object>>> birthdays(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(elderService.upcomingBirthdays(days));
    }

    @GetMapping("/patrol")
    public Result<List<Map<String, Object>>> patrol() {
        return Result.ok(elderService.patrolList());
    }

    public record PatrolRequest(String content, String result) {
    }

    @PostMapping("/{id}/patrol")
    public Result<PatrolRecord> recordPatrol(@PathVariable Long id, @RequestBody PatrolRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.GRID, Role.VOLUNTEER);
        var user = SecurityUtils.get();
        return Result.ok(elderService.recordPatrol(id, user.getUserId(), user.getRealName(),
                request.content(), request.result()));
    }
}
