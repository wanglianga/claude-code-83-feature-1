package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.SubsidyRecord;
import com.community.haircut.enums.Role;
import com.community.haircut.enums.SubsidyStatus;
import com.community.haircut.security.LoginUser;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.FinanceService;
import com.community.haircut.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    private OrderService.LoginUserInfo op() {
        LoginUser u = SecurityUtils.get();
        return new OrderService.LoginUserInfo(u.getUserId(), u.getRealName(), u.getRole());
    }

    @GetMapping("/subsidies")
    public Result<List<SubsidyRecord>> list(@RequestParam(required = false) String status) {
        SecurityUtils.requireRole(Role.FINANCE, Role.STAFF, Role.ADMIN);
        return Result.ok(financeService.list(status == null ? null : SubsidyStatus.valueOf(status)));
    }

    public record ReviewRequest(boolean approve, String note) {
    }

    @PostMapping("/subsidies/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody ReviewRequest request) {
        SecurityUtils.requireRole(Role.FINANCE, Role.ADMIN);
        financeService.review(id, request.approve(), request.note(), op());
        return Result.ok();
    }
}
