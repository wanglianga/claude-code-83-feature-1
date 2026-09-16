package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.*;
import com.community.haircut.enums.*;
import com.community.haircut.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 异常协同：老人、家属、理发师、志愿者、社区、财务在同一服务单里处理异常。
 */
@Service
public class ExceptionService {

    private final ExceptionRecordRepository exceptionRepository;
    private final ServiceOrderRepository orderRepository;
    private final BarberProfileRepository barberProfileRepository;
    private final ElderRepository elderRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;

    public ExceptionService(ExceptionRecordRepository exceptionRepository, ServiceOrderRepository orderRepository,
                            BarberProfileRepository barberProfileRepository, ElderRepository elderRepository,
                            OrderService orderService, NotificationService notificationService) {
        this.exceptionRepository = exceptionRepository;
        this.orderRepository = orderRepository;
        this.barberProfileRepository = barberProfileRepository;
        this.elderRepository = elderRepository;
        this.orderService = orderService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ExceptionRecord report(Long orderId, ExceptionType type, String description,
                                  OrderService.LoginUserInfo operator) {
        ServiceOrder order = orderRepository.findById(orderId).orElseThrow(() -> new BizException("服务单不存在"));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BizException("服务单已取消，不可上报异常");
        }
        ExceptionRecord record = new ExceptionRecord();
        record.setOrderId(orderId);
        record.setType(type);
        record.setReportedById(operator.userId());
        record.setReportedByName(operator.realName());
        record.setReportedByRole(operator.role().name());
        record.setDescription(description);
        record.setFinanceInvolved(type == ExceptionType.REFUSE_PAYMENT
                || type == ExceptionType.SUBSIDY_CHANGE
                || type == ExceptionType.SKIN_CUT);
        exceptionRepository.save(record);

        order.setHasException(true);
        order.setUpdatedAt(LocalDateTime.now());

        // 各类异常的联动副作用
        switch (type) {
            case BARBER_LATE -> adjustBarberCredit(order.getBarberId(), -5, p -> p.setLateCount(p.getLateCount() + 1));
            case SKIN_CUT -> adjustBarberCredit(order.getBarberId(), -10, p -> p.setIncidentCount(p.getIncidentCount() + 1));
            case TOOL_MISSING -> adjustBarberCredit(order.getBarberId(), -3, p -> {
            });
            case REFUSE_PAYMENT -> order.setPaymentStatus(PaymentStatus.DISPUTED);
            default -> {
            }
        }
        orderRepository.save(order);

        orderService.addEvent(orderId, "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                "上报异常【" + typeName(type) + "】：" + description);

        // 通知社区与财务（同一服务单协同）
        notificationService.notifyRole(Role.STAFF, "服务单异常 " + order.getOrderNo(),
                "【" + typeName(type) + "】" + description, "EXCEPTION");
        if (Boolean.TRUE.equals(record.getFinanceInvolved())) {
            notificationService.notifyRole(Role.FINANCE, "异常需财务介入 " + order.getOrderNo(),
                    "【" + typeName(type) + "】" + description, "EXCEPTION");
        }
        Elder elder = elderRepository.findById(order.getElderId()).orElse(null);
        if (elder != null && elder.getFamilyUserId() != null) {
            notificationService.notify(elder.getFamilyUserId(), "服务异常提醒 " + order.getOrderNo(),
                    "【" + typeName(type) + "】" + description, "EXCEPTION");
        }
        return record;
    }

    private void adjustBarberCredit(Long barberId, int delta, java.util.function.Consumer<BarberProfile> counter) {
        if (barberId == null) {
            return;
        }
        barberProfileRepository.findByUserId(barberId).ifPresent(p -> {
            p.setCreditScore(Math.max(0, Math.min(100, p.getCreditScore() + delta)));
            counter.accept(p);
            barberProfileRepository.save(p);
        });
    }

    @Transactional
    public void handle(Long exceptionId, OrderService.LoginUserInfo operator) {
        ExceptionRecord record = get(exceptionId);
        if (record.getStatus() != ExceptionStatus.OPEN) {
            throw new BizException("该异常已在处理中或已解决");
        }
        record.setStatus(ExceptionStatus.PROCESSING);
        record.setHandledById(operator.userId());
        exceptionRepository.save(record);
        orderService.addEvent(record.getOrderId(), "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                "开始处理异常【" + typeName(record.getType()) + "】");
    }

    /**
     * 解决异常；补贴资格变化时可同步更新老人补贴类型并重算服务单费用。
     */
    @Transactional
    public void resolve(Long exceptionId, String resolution, SubsidyType newSubsidyType,
                        Boolean paymentPaid, OrderService.LoginUserInfo operator) {
        ExceptionRecord record = get(exceptionId);
        if (record.getStatus() == ExceptionStatus.RESOLVED) {
            throw new BizException("该异常已解决");
        }
        record.setStatus(ExceptionStatus.RESOLVED);
        record.setResolution(resolution);
        record.setHandledById(operator.userId());
        record.setResolvedAt(LocalDateTime.now());
        exceptionRepository.save(record);

        ServiceOrder order = orderRepository.findById(record.getOrderId()).orElseThrow();
        if (record.getType() == ExceptionType.SUBSIDY_CHANGE && newSubsidyType != null) {
            Elder elder = elderRepository.findById(order.getElderId()).orElseThrow();
            elder.setSubsidyType(newSubsidyType);
            elderRepository.save(elder);
            BigDecimal subsidy = switch (newSubsidyType) {
                case FULL -> order.getTotalAmount();
                case PARTIAL -> OrderService.PARTIAL_SUBSIDY;
                default -> BigDecimal.ZERO;
            };
            order.setSubsidyAmount(subsidy);
            order.setSelfPayAmount(order.getTotalAmount().subtract(subsidy));
            orderService.addEvent(order.getId(), "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                    "补贴资格调整为 " + newSubsidyType + "，重算费用：补贴 ¥" + subsidy + "，自费 ¥" + order.getSelfPayAmount());
        }
        if (record.getType() == ExceptionType.REFUSE_PAYMENT && Boolean.TRUE.equals(paymentPaid)) {
            order.setPaymentStatus(PaymentStatus.PAID);
            orderService.addEvent(order.getId(), "PAYMENT", operator.userId(), operator.realName(), operator.role(),
                    "拒绝付款异常解决，自费金额已补缴");
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        orderService.addEvent(record.getOrderId(), "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                "异常【" + typeName(record.getType()) + "】已解决：" + resolution);
    }

    public ExceptionRecord get(Long id) {
        return exceptionRepository.findById(id).orElseThrow(() -> new BizException("异常记录不存在"));
    }

    public List<ExceptionRecord> list(ExceptionStatus status, Boolean financeOnly) {
        if (Boolean.TRUE.equals(financeOnly)) {
            return exceptionRepository.findByFinanceInvolvedTrueAndStatusNot(ExceptionStatus.RESOLVED);
        }
        if (status != null) {
            return exceptionRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return exceptionRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<ExceptionRecord> listByOrder(Long orderId) {
        return exceptionRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    private String typeName(ExceptionType type) {
        return switch (type) {
            case ELDER_UNWELL -> "老人临时不适";
            case FAMILY_RESCHEDULE -> "家属改约";
            case BARBER_LATE -> "理发师迟到";
            case SKIN_CUT -> "皮肤划伤";
            case REFUSE_PAYMENT -> "老人拒绝付款";
            case SUBSIDY_CHANGE -> "补贴资格变化";
            case TOOL_MISSING -> "工具遗漏";
        };
    }
}
