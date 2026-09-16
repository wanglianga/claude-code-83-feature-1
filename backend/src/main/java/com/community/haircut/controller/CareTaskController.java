package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.CareTask;
import com.community.haircut.enums.CareTaskStatus;
import com.community.haircut.enums.Role;
import com.community.haircut.security.LoginUser;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.CareTaskService;
import com.community.haircut.service.OrderService;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/care-tasks")
public class CareTaskController {

    private final CareTaskService careTaskService;

    public CareTaskController(CareTaskService careTaskService) {
        this.careTaskService = careTaskService;
    }

    private OrderService.LoginUserInfo op() {
        LoginUser u = SecurityUtils.get();
        return new OrderService.LoginUserInfo(u.getUserId(), u.getRealName(), u.getRole());
    }

    @GetMapping
    public Result<List<CareTask>> list(@RequestParam(required = false) String status) {
        LoginUser u = SecurityUtils.get();
        Long gridWorkerId = u.getRole() == Role.GRID ? u.getUserId() : null;
        return Result.ok(careTaskService.list(status == null ? null : CareTaskStatus.valueOf(status), gridWorkerId));
    }

    /** 手动触发扫描：长期未预约 / 连续取消 → 自动生成关怀任务 */
    @PostMapping("/scan")
    public Result<Integer> scan() {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(careTaskService.scan());
    }

    public record CreateRequest(@NotNull Long elderId, String notes, Long gridWorkerId) {
    }

    @PostMapping
    public Result<CareTask> create(@RequestBody CreateRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(careTaskService.createManual(request.elderId(), request.notes(), request.gridWorkerId()));
    }

    public record ConfirmRequest(String result, boolean needExtraCare) {
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id, @RequestBody ConfirmRequest request) {
        SecurityUtils.requireRole(Role.GRID, Role.STAFF, Role.ADMIN);
        careTaskService.confirm(id, request.result(), request.needExtraCare(), op());
        return Result.ok();
    }
}
