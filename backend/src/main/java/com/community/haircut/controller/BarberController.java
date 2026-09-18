package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.BarberProfile;
import com.community.haircut.entity.BarberSchedule;
import com.community.haircut.entity.DisinfectionRecord;
import com.community.haircut.entity.ToolKit;
import com.community.haircut.entity.User;
import com.community.haircut.enums.DisinfectionMethod;
import com.community.haircut.enums.Role;
import com.community.haircut.security.LoginUser;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.BarberService;
import com.community.haircut.service.OrderService;
import com.community.haircut.service.UserService;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/barbers")
public class BarberController {

    private final BarberService barberService;
    private final UserService userService;

    public BarberController(BarberService barberService, UserService userService) {
        this.barberService = barberService;
        this.userService = userService;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(barberService.listBarbers());
    }

    @GetMapping("/volunteers")
    public Result<List<User>> volunteers() {
        return Result.ok(userService.listByRole(Role.VOLUNTEER));
    }

    public record ProfileRequest(String skillTags, String serviceBuildings, Boolean active) {
    }

    @PutMapping("/{userId}/profile")
    public Result<BarberProfile> updateProfile(@PathVariable Long userId, @RequestBody ProfileRequest request) {
        LoginUser u = SecurityUtils.get();
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.STAFF && !u.getUserId().equals(userId)) {
            throw new com.community.haircut.common.BizException(403, "无权限");
        }
        return Result.ok(barberService.updateProfile(userId, request.skillTags(), request.serviceBuildings(), request.active()));
    }

    public record CreditRequest(int delta, String reason) {
    }

    @PostMapping("/{userId}/credit")
    public Result<BarberProfile> adjustCredit(@PathVariable Long userId, @RequestBody CreditRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(barberService.adjustCredit(userId, request.delta(), request.reason()));
    }

    @GetMapping("/schedules")
    public Result<List<BarberSchedule>> schedules(@RequestParam(required = false) Long barberId,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.ok(barberService.listSchedules(barberId, from, to));
    }

    public record ScheduleRequest(@NotNull Long barberId, @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
                                  @NotNull String startTime, @NotNull String endTime, Integer maxOrders) {
    }

    @PostMapping("/schedules")
    public Result<BarberSchedule> addSchedule(@RequestBody ScheduleRequest request) {
        LoginUser u = SecurityUtils.get();
        if (u.getRole() == Role.BARBER && !u.getUserId().equals(request.barberId())) {
            throw new com.community.haircut.common.BizException(403, "只能为自己排班");
        }
        SecurityUtils.requireRole(Role.BARBER, Role.STAFF, Role.ADMIN);
        return Result.ok(barberService.addSchedule(request.barberId(), request.workDate(),
                request.startTime(), request.endTime(), request.maxOrders()));
    }

    @DeleteMapping("/schedules/{id}")
    public Result<Void> deleteSchedule(@PathVariable Long id) {
        LoginUser u = SecurityUtils.get();
        boolean staff = u.getRole() == Role.STAFF || u.getRole() == Role.ADMIN;
        if (!staff && u.getRole() != Role.BARBER) {
            throw new com.community.haircut.common.BizException(403, "无权限");
        }
        barberService.deleteSchedule(id, u.getUserId(), staff);
        return Result.ok();
    }

    @GetMapping("/{barberId}/toolkit")
    public Result<ToolKit> getToolKit(@PathVariable Long barberId) {
        return Result.ok(barberService.getToolKit(barberId));
    }

    /** 理发师全部工具包（主包 + 上门备用服务包），含实时核验结论 */
    @GetMapping("/{barberId}/kits")
    public Result<List<Map<String, Object>>> listKits(@PathVariable Long barberId) {
        return Result.ok(barberService.listKits(barberId));
    }

    /** 上门前扫码核验服务包封签 */
    @GetMapping("/{barberId}/scan")
    public Result<Map<String, Object>> scan(@PathVariable Long barberId, @RequestParam String sealCode) {
        LoginUser u = SecurityUtils.get();
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.STAFF && !u.getUserId().equals(barberId)) {
            throw new com.community.haircut.common.BizException(403, "无权限");
        }
        return Result.ok(barberService.scanBySeal(barberId, sealCode));
    }

    /** 新建上门备用服务包 */
    public record SpareKitRequest(String name, String items) {
    }

    @PostMapping("/{barberId}/spare-kits")
    public Result<ToolKit> createSpareKit(@PathVariable Long barberId, @RequestBody SpareKitRequest request) {
        LoginUser u = SecurityUtils.get();
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.STAFF && !u.getUserId().equals(barberId)) {
            throw new com.community.haircut.common.BizException(403, "无权限");
        }
        return Result.ok(barberService.createSpareKit(barberId, request.name(), request.items()));
    }

    /** 消毒记录查询（社区可查全部；理发师查本人或指定包） */
    @GetMapping("/disinfections")
    public Result<List<DisinfectionRecord>> disinfections(@RequestParam(required = false) Long barberId,
                                                          @RequestParam(required = false) Long kitId) {
        return Result.ok(barberService.listDisinfections(barberId, kitId));
    }

    public record DisinfectionRequest(@NotNull Long kitId, String method, String cabinetNo,
                                      String responsiblePerson, String sealCode, Integer validHours,
                                      boolean supplementary, Long orderId, String note) {
    }

    /** 消毒登记 / 备用服务包使用后补录 */
    @PostMapping("/disinfections")
    public Result<DisinfectionRecord> recordDisinfection(@RequestBody DisinfectionRequest request) {
        return Result.ok(barberService.recordDisinfection(request.kitId(),
                DisinfectionMethod.valueOf(request.method()), request.cabinetNo(), request.responsiblePerson(),
                request.sealCode(), request.validHours(), request.supplementary(), request.orderId(),
                request.note(), op()));
    }

    /** 登记封签破损 */
    @PostMapping("/kits/{kitId}/seal-broken")
    public Result<ToolKit> markSealBroken(@PathVariable Long kitId) {
        LoginUser u = SecurityUtils.get();
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.STAFF) {
            throw new com.community.haircut.common.BizException(403, "仅社区可登记封签破损");
        }
        return Result.ok(barberService.markSealBroken(kitId, op()));
    }

    /** 社区确认消毒培训完成 */
    @PostMapping("/{userId}/training")
    public Result<BarberProfile> completeTraining(@PathVariable Long userId, @RequestBody(required = false) Map<String, String> body) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        String note = body == null ? null : body.get("note");
        return Result.ok(barberService.completeTraining(userId, note, op()));
    }

    /** 社区安排工具复检并登记结果 */
    public record RecheckRequest(boolean passed, String note) {
    }

    @PostMapping("/{userId}/recheck")
    public Result<BarberProfile> recheckTools(@PathVariable Long userId, @RequestBody RecheckRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(barberService.recheckTools(userId, request.passed(), request.note(), op()));
    }

    /** 培训+复检通过后恢复上门资格 */
    @PostMapping("/{userId}/reinstate")
    public Result<BarberProfile> reinstate(@PathVariable Long userId) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        return Result.ok(barberService.reinstate(userId, op()));
    }

    private OrderService.LoginUserInfo op() {
        LoginUser u = SecurityUtils.get();
        return new OrderService.LoginUserInfo(u.getUserId(), u.getRealName(), u.getRole());
    }

    public record ToolKitRequest(String name, String items, String notes) {
    }

    @PutMapping("/{barberId}/toolkit")
    public Result<ToolKit> saveToolKit(@PathVariable Long barberId, @RequestBody ToolKitRequest request) {
        LoginUser u = SecurityUtils.get();
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.STAFF && !u.getUserId().equals(barberId)) {
            throw new com.community.haircut.common.BizException(403, "无权限");
        }
        return Result.ok(barberService.saveToolKit(barberId, request.name(), request.items(), request.notes()));
    }

    @PostMapping("/{barberId}/toolkit/disinfect")
    public Result<ToolKit> disinfect(@PathVariable Long barberId) {
        LoginUser u = SecurityUtils.get();
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.STAFF && !u.getUserId().equals(barberId)) {
            throw new com.community.haircut.common.BizException(403, "无权限");
        }
        return Result.ok(barberService.disinfect(barberId));
    }
}
