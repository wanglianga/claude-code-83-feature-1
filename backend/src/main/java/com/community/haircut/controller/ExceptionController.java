package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.ExceptionRecord;
import com.community.haircut.enums.ExceptionStatus;
import com.community.haircut.enums.ExceptionType;
import com.community.haircut.enums.Role;
import com.community.haircut.enums.SubsidyType;
import com.community.haircut.security.LoginUser;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.ExceptionService;
import com.community.haircut.service.OrderService;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exceptions")
public class ExceptionController {

    private final ExceptionService exceptionService;

    public ExceptionController(ExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    private OrderService.LoginUserInfo op() {
        LoginUser u = SecurityUtils.get();
        return new OrderService.LoginUserInfo(u.getUserId(), u.getRealName(), u.getRole());
    }

    public record ReportRequest(@NotNull Long orderId, @NotNull String type, String description) {
    }

    @PostMapping
    public Result<ExceptionRecord> report(@RequestBody ReportRequest request) {
        return Result.ok(exceptionService.report(request.orderId(),
                ExceptionType.valueOf(request.type()), request.description(), op()));
    }

    @GetMapping
    public Result<List<ExceptionRecord>> list(@RequestParam(required = false) String status,
                                              @RequestParam(required = false) Boolean financeOnly) {
        return Result.ok(exceptionService.list(status == null ? null : ExceptionStatus.valueOf(status), financeOnly));
    }

    @GetMapping("/order/{orderId}")
    public Result<List<ExceptionRecord>> listByOrder(@PathVariable Long orderId) {
        return Result.ok(exceptionService.listByOrder(orderId));
    }

    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.FINANCE);
        exceptionService.handle(id, op());
        return Result.ok();
    }

    public record ResolveRequest(String resolution, String newSubsidyType, Boolean paymentPaid) {
    }

    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id, @RequestBody ResolveRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.FINANCE);
        exceptionService.resolve(id, request.resolution(),
                request.newSubsidyType() == null ? null : SubsidyType.valueOf(request.newSubsidyType()),
                request.paymentPaid(), op());
        return Result.ok();
    }
}
