package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.ServiceOrder;
import com.community.haircut.entity.ToolConfirmation;
import com.community.haircut.entity.VisitRecord;
import com.community.haircut.entity.ServiceRecord;
import com.community.haircut.enums.OrderStatus;
import com.community.haircut.enums.OrderType;
import com.community.haircut.enums.Role;
import com.community.haircut.repository.ElderRepository;
import com.community.haircut.security.LoginUser;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.OrderService;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final ElderRepository elderRepository;

    public OrderController(OrderService orderService, ElderRepository elderRepository) {
        this.orderService = orderService;
        this.elderRepository = elderRepository;
    }

    private OrderService.LoginUserInfo op() {
        LoginUser u = SecurityUtils.get();
        return new OrderService.LoginUserInfo(u.getUserId(), u.getRealName(), u.getRole());
    }

    public record CreateRequest(@NotNull Long elderId, @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate,
                                @NotNull String timeSlot, String type, Long eventId, Long barberId) {
    }

    /** 创建预约：智能匹配理发师排班、志愿者、风险等级、楼栋距离、工具消毒状态 */
    @PostMapping
    public Result<ServiceOrder> create(@RequestBody CreateRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN);
        OrderType type = request.type() == null ? OrderType.NORMAL : OrderType.valueOf(request.type());
        return Result.ok(orderService.createOrder(request.elderId(), request.scheduledDate(), request.timeSlot(),
                type, request.eventId(), request.barberId(), op()));
    }

    @GetMapping
    public Result<List<ServiceOrder>> list(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                           @RequestParam(required = false) Long barberId,
                                           @RequestParam(required = false) Long volunteerId,
                                           @RequestParam(required = false) Long elderId) {
        LoginUser u = SecurityUtils.get();
        // 理发师/志愿者默认只看自己的单；家属只看关联老人的单
        if (u.getRole() == Role.BARBER) {
            barberId = u.getUserId();
        } else if (u.getRole() == Role.VOLUNTEER) {
            volunteerId = u.getUserId();
        }
        List<ServiceOrder> orders = orderService.list(status == null ? null : OrderStatus.valueOf(status),
                date, barberId, volunteerId, elderId);
        if (u.getRole() == Role.FAMILY) {
            List<Long> elderIds = elderRepository.findByFamilyUserId(u.getUserId())
                    .stream().map(e -> e.getId()).toList();
            orders = orders.stream().filter(o -> elderIds.contains(o.getElderId())).toList();
        }
        return Result.ok(orders);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    public record ToolConfirmRequest(boolean toolsOk, boolean capeOk, boolean disinfectantOk, boolean packOk,
                                     String missingItems) {
    }

    @PostMapping("/{id}/confirm-tools")
    public Result<ToolConfirmation> confirmTools(@PathVariable Long id, @RequestBody ToolConfirmRequest request) {
        SecurityUtils.requireRole(Role.BARBER, Role.STAFF, Role.ADMIN);
        return Result.ok(orderService.confirmTools(id, request.toolsOk(), request.capeOk(),
                request.disinfectantOk(), request.packOk(), request.missingItems(), op()));
    }

    public record CheckInRequest(boolean entrySafe, String elderState, String mentalState,
                                 boolean familyAuthorized, String photos, String notes) {
    }

    @PostMapping("/{id}/check-in")
    public Result<VisitRecord> checkIn(@PathVariable Long id, @RequestBody CheckInRequest request) {
        SecurityUtils.requireRole(Role.VOLUNTEER, Role.STAFF, Role.ADMIN);
        return Result.ok(orderService.checkIn(id, request.entrySafe(), request.elderState(), request.mentalState(),
                request.familyAuthorized(), request.photos(), request.notes(), op()));
    }

    @PostMapping("/{id}/start")
    public Result<Void> start(@PathVariable Long id) {
        SecurityUtils.requireRole(Role.BARBER, Role.STAFF, Role.ADMIN);
        orderService.startService(id, op());
        return Result.ok();
    }

    public record CompleteRequest(String haircutPhotos, String paymentNote) {
    }

    @PostMapping("/{id}/complete")
    public Result<ServiceRecord> complete(@PathVariable Long id, @RequestBody CompleteRequest request) {
        SecurityUtils.requireRole(Role.BARBER, Role.STAFF, Role.ADMIN);
        return Result.ok(orderService.complete(id, request.haircutPhotos(), request.paymentNote(), op()));
    }

    public record CancelRequest(String reason) {
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody CancelRequest request) {
        orderService.cancel(id, request.reason(), op());
        return Result.ok();
    }

    public record RescheduleRequest(@NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
                                    @NotNull String newTimeSlot, String reason) {
    }

    @PostMapping("/{id}/reschedule")
    public Result<Void> reschedule(@PathVariable Long id, @RequestBody RescheduleRequest request) {
        orderService.reschedule(id, request.newDate(), request.newTimeSlot(), request.reason(), op());
        return Result.ok();
    }

    public record RateRequest(int rating, String comment) {
    }

    @PostMapping("/{id}/rate")
    public Result<Void> rate(@PathVariable Long id, @RequestBody RateRequest request) {
        SecurityUtils.requireRole(Role.FAMILY, Role.STAFF, Role.ADMIN);
        orderService.rate(id, request.rating(), request.comment(), op());
        return Result.ok();
    }

    @PostMapping("/{id}/confirm-payment")
    public Result<Void> confirmPayment(@PathVariable Long id) {
        SecurityUtils.requireRole(Role.BARBER, Role.STAFF, Role.ADMIN, Role.FINANCE);
        orderService.confirmPayment(id, op());
        return Result.ok();
    }
}
