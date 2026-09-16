package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.BarberProfile;
import com.community.haircut.entity.BarberSchedule;
import com.community.haircut.entity.ToolKit;
import com.community.haircut.entity.User;
import com.community.haircut.enums.Role;
import com.community.haircut.security.LoginUser;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.BarberService;
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
