package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.enums.Role;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(statsService.dashboard());
    }

    @GetMapping("/coverage")
    public Result<Map<String, Object>> coverage() {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.GRID);
        return Result.ok(statsService.coverage());
    }

    @GetMapping("/barbers")
    public Result<List<Map<String, Object>>> barbers() {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(statsService.barberQuality());
    }

    @GetMapping("/funds")
    public Result<Map<String, Object>> funds() {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.FINANCE);
        return Result.ok(statsService.funds());
    }

    @GetMapping("/alerts")
    public Result<Map<String, Object>> alerts() {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.GRID);
        return Result.ok(statsService.alerts());
    }

    /** 工具消毒、交叉感染投诉、复检与空跑补偿复盘 */
    @GetMapping("/infection-review")
    public Result<Map<String, Object>> infectionReview() {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.FINANCE);
        return Result.ok(statsService.infectionReview());
    }
}
