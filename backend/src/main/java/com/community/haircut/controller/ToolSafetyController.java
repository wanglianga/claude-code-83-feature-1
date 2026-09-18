package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.InfectionContact;
import com.community.haircut.entity.InfectionTrace;
import com.community.haircut.entity.ToolIssueRecord;
import com.community.haircut.entity.ToolKit;
import com.community.haircut.enums.Role;
import com.community.haircut.security.LoginUser;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.OrderService;
import com.community.haircut.service.ToolSafetyService;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具消毒失效与交叉感染追溯：备用服务包、核验不通过处置与补偿、感染反查与责任处置。
 */
@RestController
@RequestMapping("/api/tool-safety")
public class ToolSafetyController {

    private final ToolSafetyService toolSafetyService;

    public ToolSafetyController(ToolSafetyService toolSafetyService) {
        this.toolSafetyService = toolSafetyService;
    }

    private OrderService.LoginUserInfo op() {
        LoginUser u = SecurityUtils.get();
        return new OrderService.LoginUserInfo(u.getUserId(), u.getRealName(), u.getRole());
    }

    // ---------------- 备用服务包与消毒补录 ----------------

    @GetMapping("/backup-kits")
    public Result<List<ToolKit>> backupKits() {
        return Result.ok(toolSafetyService.listBackupKits());
    }

    @GetMapping("/pending-disinfection")
    public Result<List<ToolKit>> pending() {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.FINANCE);
        return Result.ok(toolSafetyService.listPendingDisinfection());
    }

    public record BackupKitRequest(String name, String items, String sealNo, String method,
                                   String cabinetNo, String responsiblePerson) {
    }

    @PostMapping("/backup-kits")
    public Result<ToolKit> createBackup(@RequestBody BackupKitRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(toolSafetyService.createBackupKit(request.name(), request.items(), request.sealNo(),
                request.method(), request.cabinetNo(), request.responsiblePerson()));
    }

    public record DisinfectionRequest(String sealNo, String method, String cabinetNo, String responsiblePerson) {
    }

    /** 补录消毒记录（备用服务包使用后必须补录，未补录不得再派单） */
    @PostMapping("/kits/{id}/disinfection")
    public Result<ToolKit> recordDisinfection(@PathVariable Long id, @RequestBody DisinfectionRequest request) {
        LoginUser u = SecurityUtils.get();
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.STAFF) {
            throw new com.community.haircut.common.BizException(403, "消毒记录由社区登记补录");
        }
        return Result.ok(toolSafetyService.recordDisinfection(id, request.sealNo(), request.method(),
                request.cabinetNo(), request.responsiblePerson(), op()));
    }

    // ---------------- 核验不通过处置 ----------------

    public record IssueRequest(@NotNull Long orderId, @NotNull String issueType, String issueDetail,
                               @NotNull String action, Long backupKitId,
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rescheduleDate,
                               String timeSlot) {
    }

    @PostMapping("/issues")
    public Result<ToolIssueRecord> handleIssue(@RequestBody IssueRequest request) {
        SecurityUtils.requireRole(Role.BARBER, Role.STAFF, Role.ADMIN);
        return Result.ok(toolSafetyService.handleIssue(request.orderId(), request.issueType(),
                request.issueDetail(), request.action(), request.backupKitId(),
                request.rescheduleDate(), request.timeSlot(), op()));
    }

    @GetMapping("/issues")
    public Result<List<ToolIssueRecord>> listIssues(@RequestParam(defaultValue = "false") boolean pendingOnly) {
        return Result.ok(toolSafetyService.listIssues(pendingOnly));
    }

    public record CompensationRequest(@NotNull String result, BigDecimal amount, String note) {
    }

    @PostMapping("/issues/{id}/compensation")
    public Result<ToolIssueRecord> compensation(@PathVariable Long id, @RequestBody CompensationRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.FINANCE);
        return Result.ok(toolSafetyService.reviewCompensation(id, request.result(), request.amount(),
                request.note(), op()));
    }

    // ---------------- 交叉感染追溯 ----------------

    public record InfectionRequest(@NotNull Long orderId, @NotNull String symptom, String description,
                                   String toolItemNo) {
    }

    @PostMapping("/infections")
    public Result<InfectionTrace> report(@RequestBody InfectionRequest request) {
        return Result.ok(toolSafetyService.reportInfection(request.orderId(), request.symptom(),
                request.description(), request.toolItemNo(), op()));
    }

    @GetMapping("/infections")
    public Result<List<InfectionTrace>> list(@RequestParam(defaultValue = "false") boolean openOnly) {
        return Result.ok(toolSafetyService.listTraces(openOnly));
    }

    @GetMapping("/infections/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        InfectionTrace trace = toolSafetyService.getTrace(id);
        Map<String, Object> map = new HashMap<>();
        map.put("trace", trace);
        map.put("contacts", toolSafetyService.listContacts(id));
        return Result.ok(map);
    }

    @PostMapping("/contacts/{id}/notify")
    public Result<InfectionContact> notifyContact(@PathVariable Long id) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.VOLUNTEER);
        return Result.ok(toolSafetyService.notifyContact(id, op()));
    }

    public record ContactResultRequest(String medicalResult, String followUpNote) {
    }

    @PostMapping("/contacts/{id}/result")
    public Result<InfectionContact> contactResult(@PathVariable Long id, @RequestBody ContactResultRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.GRID, Role.VOLUNTEER);
        return Result.ok(toolSafetyService.recordContactResult(id, request.medicalResult(),
                request.followUpNote(), op()));
    }

    public record ConfirmRequest(boolean crossInfection, boolean disinfectionFault,
                                 Double weight, String note) {
    }

    @PostMapping("/infections/{id}/confirm")
    public Result<InfectionTrace> confirm(@PathVariable Long id, @RequestBody ConfirmRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(toolSafetyService.confirmResponsibility(id, request.crossInfection(),
                request.disinfectionFault(), request.weight() == null ? 0 : request.weight(),
                request.note(), op()));
    }

    public record ResolveRequest(boolean trainingPassed, boolean toolReinspected, boolean restoreWeight,
                                 String resolution) {
    }

    @PostMapping("/infections/{id}/resolve")
    public Result<InfectionTrace> resolve(@PathVariable Long id, @RequestBody ResolveRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(toolSafetyService.resolveTrace(id, request.trainingPassed(),
                request.toolReinspected(), request.restoreWeight(), request.resolution(), op()));
    }
}
