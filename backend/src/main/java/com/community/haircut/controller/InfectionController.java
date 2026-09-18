package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.InfectionCase;
import com.community.haircut.entity.InfectionContact;
import com.community.haircut.enums.InfectionCaseStatus;
import com.community.haircut.enums.Role;
import com.community.haircut.security.LoginUser;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.InfectionTraceService;
import com.community.haircut.service.OrderService;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 交叉感染追溯：发起反查、通知接触老人家属与志愿者、记录就医/随访结果、确认责任并处置理发师。
 */
@RestController
@RequestMapping("/api/infection-cases")
public class InfectionController {

    private final InfectionTraceService infectionTraceService;

    public InfectionController(InfectionTraceService infectionTraceService) {
        this.infectionTraceService = infectionTraceService;
    }

    private OrderService.LoginUserInfo op() {
        LoginUser u = SecurityUtils.get();
        return new OrderService.LoginUserInfo(u.getUserId(), u.getRealName(), u.getRole());
    }

    public record CreateCaseRequest(@NotNull Long orderId, String symptom, String description) {
    }

    /** 发起追溯（老人/家属/理发师/志愿者/社区均可接报；系统也会自动发起） */
    @PostMapping
    public Result<InfectionCase> create(@RequestBody CreateCaseRequest request) {
        return Result.ok(infectionTraceService.createCase(request.orderId(), request.symptom(),
                request.description(), op()));
    }

    @GetMapping
    public Result<List<InfectionCase>> list(@RequestParam(required = false) String status) {
        return Result.ok(infectionTraceService.list(
                status == null ? null : InfectionCaseStatus.valueOf(status)));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(infectionTraceService.detail(id));
    }

    /** 通知同工具服务老人的家属与志愿者，建议就医 */
    @PostMapping("/{id}/notify")
    public Result<InfectionCase> notifyContacts(@PathVariable Long id) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.VOLUNTEER);
        return Result.ok(infectionTraceService.notifyContacts(id, op()));
    }

    public record ContactResultRequest(String followUpResult, boolean confirmedInfected) {
    }

    /** 记录接触老人就医/随访结果 */
    @PostMapping("/contacts/{contactId}/result")
    public Result<InfectionContact> contactResult(@PathVariable Long contactId,
                                                  @RequestBody ContactResultRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.VOLUNTEER, Role.GRID);
        return Result.ok(infectionTraceService.recordContactResult(contactId, request.followUpResult(),
                request.confirmedInfected(), op()));
    }

    public record ConcludeRequest(boolean crossInfectionConfirmed, boolean disinfectionResponsible,
                                  Integer weightDeduction, String conclusion) {
    }

    /** 社区确认结论并处置（暂停资格/培训/复检/下调派单权重，或排除） */
    @PostMapping("/{id}/conclude")
    public Result<InfectionCase> conclude(@PathVariable Long id, @RequestBody ConcludeRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(infectionTraceService.conclude(id, request.crossInfectionConfirmed(),
                request.disinfectionResponsible(), request.weightDeduction(), request.conclusion(), op()));
    }
}
