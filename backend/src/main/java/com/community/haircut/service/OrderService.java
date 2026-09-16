package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.*;
import com.community.haircut.enums.*;
import com.community.haircut.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderService {

    /** 单次理发标准价（元） */
    public static final BigDecimal BASE_PRICE = new BigDecimal("30");
    /** 部分补贴时补贴金额（元） */
    public static final BigDecimal PARTIAL_SUBSIDY = new BigDecimal("20");
    /** 消毒有效期（小时） */
    public static final long DISINFECTION_VALID_HOURS = 48;

    private final ServiceOrderRepository orderRepository;
    private final ElderRepository elderRepository;
    private final BarberScheduleRepository scheduleRepository;
    private final BarberProfileRepository barberProfileRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final ToolKitRepository toolKitRepository;
    private final OrderEventRepository eventRepository;
    private final ToolConfirmationRepository toolConfirmationRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final SubsidyRecordRepository subsidyRecordRepository;
    private final FollowUpRepository followUpRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private final AtomicInteger orderSeq = new AtomicInteger(0);

    public OrderService(ServiceOrderRepository orderRepository, ElderRepository elderRepository,
                        BarberScheduleRepository scheduleRepository,
                        BarberProfileRepository barberProfileRepository,
                        VolunteerProfileRepository volunteerProfileRepository,
                        ToolKitRepository toolKitRepository, OrderEventRepository eventRepository,
                        ToolConfirmationRepository toolConfirmationRepository,
                        VisitRecordRepository visitRecordRepository,
                        ServiceRecordRepository serviceRecordRepository,
                        SubsidyRecordRepository subsidyRecordRepository,
                        FollowUpRepository followUpRepository, UserRepository userRepository,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.elderRepository = elderRepository;
        this.scheduleRepository = scheduleRepository;
        this.barberProfileRepository = barberProfileRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.toolKitRepository = toolKitRepository;
        this.eventRepository = eventRepository;
        this.toolConfirmationRepository = toolConfirmationRepository;
        this.visitRecordRepository = visitRecordRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.subsidyRecordRepository = subsidyRecordRepository;
        this.followUpRepository = followUpRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // ---------------- 智能派单 ----------------

    /**
     * 创建预约：根据理发师排班、志愿者陪同、老人风险等级、楼栋距离、工具消毒状态自动生成服务单。
     */
    @Transactional
    public ServiceOrder createOrder(Long elderId, LocalDate date, String timeSlot, OrderType type,
                                    Long eventId, Long manualBarberId, LoginUserInfo operator) {
        Elder elder = elderRepository.findById(elderId).orElseThrow(() -> new BizException("老人档案不存在"));
        if (!"ACTIVE".equals(elder.getStatus())) {
            throw new BizException("该老人档案已停用");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new BizException("预约日期不能早于今天");
        }
        boolean duplicated = orderRepository.existsByElderIdAndScheduledDateAndStatusNotIn(
                elderId, date, List.of(OrderStatus.CANCELLED));
        if (duplicated) {
            throw new BizException("该老人在 " + date + " 已有进行中的预约，请勿重复下单");
        }

        // 1. 匹配理发师：排班覆盖时段 + 当日容量未满 + 工具消毒有效
        BarberSchedule chosen = chooseBarber(elder, date, timeSlot, manualBarberId);
        Long barberId = chosen.getBarberId();
        User barber = userRepository.findById(barberId).orElseThrow(() -> new BizException("理发师不存在"));

        // 2. 匹配志愿者：高风险 / 独居 / 需家属在场的老人必须安排志愿者陪同
        boolean volunteerNeeded = elder.getRiskLevel() != RiskLevel.LOW
                || Boolean.TRUE.equals(elder.getLivingAlone())
                || Boolean.TRUE.equals(elder.getNeedFamilyPresent());
        VolunteerProfile volunteer = chooseVolunteer(date);
        if (volunteerNeeded && volunteer == null) {
            throw new BizException("该老人为高风险/独居/需家属在场，必须安排志愿者陪同，但当日无可用志愿者");
        }
        User volunteerUser = volunteer == null ? null : userRepository.findById(volunteer.getUserId()).orElse(null);

        // 3. 计算费用与补贴
        BigDecimal[] amounts = computeAmounts(elder, type);

        ServiceOrder order = new ServiceOrder();
        order.setOrderNo(genOrderNo());
        order.setElderId(elder.getId());
        order.setElderName(elder.getName());
        order.setBarberId(barberId);
        order.setBarberName(barber.getRealName());
        order.setVolunteerId(volunteerUser == null ? null : volunteerUser.getId());
        order.setVolunteerName(volunteerUser == null ? null : volunteerUser.getRealName());
        order.setCreatedById(operator.userId());
        order.setType(type);
        order.setEventId(eventId);
        order.setScheduledDate(date);
        order.setTimeSlot(timeSlot);
        order.setAddress(elder.getAddress());
        order.setNeedFamilyPresent(elder.getNeedFamilyPresent());
        order.setRiskLevel(elder.getRiskLevel());
        order.setLivingAlone(elder.getLivingAlone());
        order.setTotalAmount(amounts[0]);
        order.setSubsidyAmount(amounts[1]);
        order.setSelfPayAmount(amounts[2]);
        order.setPaymentStatus(amounts[2].compareTo(BigDecimal.ZERO) == 0 ? PaymentStatus.WAIVED : PaymentStatus.UNPAID);
        orderRepository.save(order);

        addEvent(order.getId(), "CREATE", operator.userId(), operator.realName(), operator.role(),
                "创建" + typeName(type) + "服务单，派单给理发师「" + barber.getRealName() + "」"
                        + (volunteerUser != null ? "，志愿者「" + volunteerUser.getRealName() + "」陪同" : "，无需志愿者陪同")
                        + "；补贴 ¥" + amounts[1] + "，自费 ¥" + amounts[2]);

        notificationService.notify(barberId, "新服务单 " + order.getOrderNo(),
                date + " " + timeSlot + " 上门为「" + elder.getName() + "」理发，地址：" + elder.getAddress(), "ORDER");
        if (volunteerUser != null) {
            notificationService.notify(volunteerUser.getId(), "新陪同任务 " + order.getOrderNo(),
                    date + " " + timeSlot + " 陪同为「" + elder.getName() + "」上门理发", "ORDER");
        }
        if (elder.getFamilyUserId() != null) {
            notificationService.notify(elder.getFamilyUserId(), "预约成功 " + order.getOrderNo(),
                    date + " " + timeSlot + " 将为「" + elder.getName() + "」上门理发", "ORDER");
        }
        return order;
    }

    private BarberSchedule chooseBarber(Elder elder, LocalDate date, String timeSlot, Long manualBarberId) {
        String slotStart = timeSlot.split("-")[0];
        String slotEnd = timeSlot.split("-")[1];
        List<BarberSchedule> schedules = scheduleRepository.findByWorkDateAndStatus(date, "ACTIVE");
        BarberSchedule best = null;
        double bestScore = Double.MAX_VALUE;
        for (BarberSchedule s : schedules) {
            if (manualBarberId != null && !s.getBarberId().equals(manualBarberId)) {
                continue;
            }
            // 排班需覆盖预约时段
            if (s.getStartTime().compareTo(slotStart) > 0 || s.getEndTime().compareTo(slotEnd) < 0) {
                continue;
            }
            // 当日容量
            long load = orderRepository.countByBarberIdAndScheduledDateAndStatusNotIn(
                    s.getBarberId(), date, List.of(OrderStatus.CANCELLED));
            if (load >= s.getMaxOrders()) {
                continue;
            }
            // 工具消毒状态必须有效
            ToolKit kit = toolKitRepository.findByBarberId(s.getBarberId()).orElse(null);
            if (kit == null || effectiveDisinfectionStatus(kit) != DisinfectionStatus.DISINFECTED) {
                continue;
            }
            // 评分：楼栋距离 + 当前负载；常驻楼栋优先
            BarberProfile profile = barberProfileRepository.findByUserId(s.getBarberId()).orElse(null);
            if (profile != null && !profile.getActive()) {
                continue;
            }
            double score = load * 2.0;
            int distance = elder.getBuildingDistance() == null ? 0 : elder.getBuildingDistance();
            score += distance / 500.0;
            if (profile != null && profile.getServiceBuildings() != null
                    && elder.getBuilding() != null
                    && Arrays.asList(profile.getServiceBuildings().split(",")).contains(elder.getBuilding())) {
                score -= 5;
            }
            if (score < bestScore) {
                bestScore = score;
                best = s;
            }
        }
        if (best == null) {
            if (manualBarberId != null) {
                throw new BizException("指定理发师在该时段不可用（排班/容量/工具消毒限制）");
            }
            throw new BizException("该时段无可用理发师：请检查理发师排班、当日容量与工具消毒状态");
        }
        return best;
    }

    private VolunteerProfile chooseVolunteer(LocalDate date) {
        List<VolunteerProfile> volunteers = volunteerProfileRepository.findByActiveTrue();
        VolunteerProfile best = null;
        long minLoad = Long.MAX_VALUE;
        for (VolunteerProfile v : volunteers) {
            long load = orderRepository.countByVolunteerIdAndScheduledDateAndStatusNotIn(
                    v.getUserId(), date, List.of(OrderStatus.CANCELLED));
            if (load < minLoad) {
                minLoad = load;
                best = v;
            }
        }
        return best;
    }

    /** 费用计算：返回 [总价, 补贴, 自费] */
    private BigDecimal[] computeAmounts(Elder elder, OrderType type) {
        BigDecimal total = BASE_PRICE;
        BigDecimal subsidy;
        if (type == OrderType.BIRTHDAY || type == OrderType.EVENT) {
            subsidy = total; // 生日理发与社区活动为纯公益服务
        } else {
            switch (elder.getSubsidyType()) {
                case FULL -> subsidy = total;
                case PARTIAL -> subsidy = PARTIAL_SUBSIDY;
                default -> subsidy = BigDecimal.ZERO;
            }
        }
        return new BigDecimal[]{total, subsidy, total.subtract(subsidy)};
    }

    public DisinfectionStatus effectiveDisinfectionStatus(ToolKit kit) {
        if (kit.getStatus() == DisinfectionStatus.DISINFECTED && kit.getDisinfectedAt() != null) {
            if (kit.getDisinfectedAt().plusHours(DISINFECTION_VALID_HOURS).isAfter(LocalDateTime.now())) {
                return DisinfectionStatus.DISINFECTED;
            }
            return DisinfectionStatus.EXPIRED;
        }
        return kit.getStatus();
    }

    private String genOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Set<String> existing = new HashSet<>();
        for (ServiceOrder o : orderRepository.findAll()) {
            existing.add(o.getOrderNo());
        }
        int seq = orderSeq.updateAndGet(v -> v >= 999 ? 1 : v + 1);
        String no = "HF" + date + "-" + String.format("%03d", seq);
        while (existing.contains(no)) {
            seq = orderSeq.updateAndGet(v -> v >= 999 ? 1 : v + 1);
            no = "HF" + date + "-" + String.format("%03d", seq);
        }
        return no;
    }

    // ---------------- 查询 ----------------

    public ServiceOrder get(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new BizException("服务单不存在"));
    }

    public List<ServiceOrder> list(OrderStatus status, LocalDate date, Long barberId, Long volunteerId, Long elderId) {
        List<ServiceOrder> all = orderRepository.findAllByOrderByCreatedAtDesc();
        return all.stream()
                .filter(o -> status == null || o.getStatus() == status)
                .filter(o -> date == null || o.getScheduledDate().equals(date))
                .filter(o -> barberId == null || barberId.equals(o.getBarberId()))
                .filter(o -> volunteerId == null || volunteerId.equals(o.getVolunteerId()))
                .filter(o -> elderId == null || elderId.equals(o.getElderId()))
                .toList();
    }

    /** 服务单完整详情：基础信息 + 时间线 + 工具确认 + 陪同记录 + 服务档案 + 补贴 */
    public Map<String, Object> detail(Long id) {
        ServiceOrder order = get(id);
        Map<String, Object> map = new HashMap<>();
        map.put("order", order);
        elderRepository.findById(order.getElderId()).ifPresent(e -> map.put("elder", e));
        map.put("events", eventRepository.findByOrderIdOrderByCreatedAtAsc(id));
        map.put("toolConfirmation", toolConfirmationRepository.findByOrderId(id).orElse(null));
        map.put("visitRecord", visitRecordRepository.findByOrderId(id).orElse(null));
        map.put("serviceRecord", serviceRecordRepository.findByOrderId(id).orElse(null));
        map.put("subsidies", subsidyRecordRepository.findByOrderId(id));
        map.put("followUps", followUpRepository.findByOrderId(id));
        return map;
    }

    // ---------------- 流程动作 ----------------

    /** 理发师上门前确认：工具、围布、消毒用品、服务包 */
    @Transactional
    public ToolConfirmation confirmTools(Long orderId, boolean toolsOk, boolean capeOk, boolean disinfectantOk,
                                         boolean packOk, String missingItems, LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        checkBarber(order, operator);
        if (order.getStatus() != OrderStatus.ASSIGNED) {
            throw new BizException("当前状态不可确认工具（需为待上门状态）");
        }
        ToolConfirmation tc = new ToolConfirmation();
        tc.setOrderId(orderId);
        tc.setBarberId(operator.userId());
        tc.setToolsOk(toolsOk);
        tc.setCapeOk(capeOk);
        tc.setDisinfectantOk(disinfectantOk);
        tc.setPackOk(packOk);
        tc.setMissingItems(missingItems);
        toolConfirmationRepository.save(tc);

        boolean allOk = toolsOk && capeOk && disinfectantOk && packOk;
        if (allOk) {
            order.setStatus(OrderStatus.TOOL_CONFIRMED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            addEvent(orderId, "TOOL_CONFIRM", operator.userId(), operator.realName(), operator.role(),
                    "上门前确认完成：理发工具、围布、消毒用品、服务包均已备齐");
        } else {
            addEvent(orderId, "TOOL_CONFIRM", operator.userId(), operator.realName(), operator.role(),
                    "工具确认存在遗漏：" + (missingItems == null ? "未说明" : missingItems));
        }
        return tc;
    }

    /** 志愿者陪同进门：核对老人状态、现场照片、家属授权 */
    @Transactional
    public VisitRecord checkIn(Long orderId, boolean entrySafe, String elderState, String mentalState,
                               boolean familyAuthorized, String photos, String notes, LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        if (order.getVolunteerId() == null || !order.getVolunteerId().equals(operator.userId())) {
            throw new BizException(403, "仅该服务单的陪同志愿者可执行进门核对");
        }
        if (order.getStatus() != OrderStatus.TOOL_CONFIRMED && order.getStatus() != OrderStatus.ASSIGNED) {
            throw new BizException("当前状态不可进行进门核对");
        }
        if (Boolean.TRUE.equals(order.getNeedFamilyPresent()) && !familyAuthorized) {
            throw new BizException("该老人要求家属在场，必须确认家属授权后才能开始服务");
        }
        VisitRecord record = new VisitRecord();
        record.setOrderId(orderId);
        record.setVolunteerId(operator.userId());
        record.setEntrySafe(entrySafe);
        record.setElderState(elderState);
        record.setMentalState(mentalState);
        record.setFamilyAuthorized(familyAuthorized);
        record.setPhotos(photos);
        record.setNotes(notes);
        visitRecordRepository.save(record);

        order.setStatus(OrderStatus.ON_SITE);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        addEvent(orderId, "CHECK_IN", operator.userId(), operator.realName(), operator.role(),
                "志愿者陪同进门：进门安全=" + (entrySafe ? "正常" : "异常") + "，老人精神状态=" + mentalState
                        + "，家属授权=" + (familyAuthorized ? "已确认" : "未确认"));
        return record;
    }

    /** 理发师开始服务 */
    @Transactional
    public void startService(Long orderId, LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        checkBarber(order, operator);
        if (order.getStatus() != OrderStatus.ON_SITE
                && !(order.getStatus() == OrderStatus.TOOL_CONFIRMED && order.getVolunteerId() == null)) {
            throw new BizException("当前状态不可开始服务（需志愿者完成进门核对）");
        }
        order.setStatus(OrderStatus.IN_SERVICE);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        addEvent(orderId, "START", operator.userId(), operator.realName(), operator.role(), "理发师开始理发服务");
    }

    /** 理发师完成服务：剪发照片入档，生成补贴记录与回访任务 */
    @Transactional
    public ServiceRecord complete(Long orderId, String haircutPhotos, String paymentNote, LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        checkBarber(order, operator);
        if (order.getStatus() != OrderStatus.IN_SERVICE) {
            throw new BizException("当前状态不可完成服务（需先开始服务）");
        }
        ServiceRecord record = new ServiceRecord();
        record.setOrderId(orderId);
        record.setHaircutPhotos(haircutPhotos);
        record.setTotalAmount(order.getTotalAmount());
        record.setSubsidyAmount(order.getSubsidyAmount());
        record.setSelfPayAmount(order.getSelfPayAmount());
        record.setPaymentNote(paymentNote);
        serviceRecordRepository.save(record);

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        if (order.getPaymentStatus() == PaymentStatus.UNPAID
                && order.getSelfPayAmount().compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus(PaymentStatus.WAIVED);
        }
        orderRepository.save(order);

        // 理发师信用与统计
        barberProfileRepository.findByUserId(order.getBarberId()).ifPresent(p -> {
            p.setCompletedCount(p.getCompletedCount() + 1);
            p.setCreditScore(Math.min(100, p.getCreditScore() + 1));
            barberProfileRepository.save(p);
        });
        if (order.getVolunteerId() != null) {
            volunteerProfileRepository.findByUserId(order.getVolunteerId()).ifPresent(v -> {
                v.setServiceCount(v.getServiceCount() + 1);
                volunteerProfileRepository.save(v);
            });
        }

        // 补贴记录 → 财务审核
        if (order.getSubsidyAmount().compareTo(BigDecimal.ZERO) > 0) {
            SubsidyRecord subsidy = new SubsidyRecord();
            subsidy.setOrderId(orderId);
            subsidy.setElderId(order.getElderId());
            subsidy.setElderName(order.getElderName());
            subsidy.setAmount(order.getSubsidyAmount());
            subsidyRecordRepository.save(subsidy);
            notificationService.notifyRole(Role.FINANCE, "待审核补贴 " + order.getOrderNo(),
                    "老人「" + order.getElderName() + "」补贴 ¥" + order.getSubsidyAmount() + " 待审核", "FINANCE");
        }

        // 独居老人服务后回访；有异常的服务单生成异常回访
        Elder elder = elderRepository.findById(order.getElderId()).orElse(null);
        if (Boolean.TRUE.equals(order.getLivingAlone())) {
            createFollowUp(order, FollowUpType.SERVICE,
                    "独居老人服务后回访：确认进门安全、老人精神状态与后续关怀需求");
        }
        if (Boolean.TRUE.equals(order.getHasException())) {
            createFollowUp(order, FollowUpType.EXCEPTION, "服务单存在异常，需回访确认老人状况与处理结果");
        }

        addEvent(orderId, "COMPLETE", operator.userId(), operator.realName(), operator.role(),
                "服务完成，剪发照片已入档；补贴 ¥" + order.getSubsidyAmount() + "，自费 ¥" + order.getSelfPayAmount());
        notificationService.notifyRole(Role.STAFF, "服务完成 " + order.getOrderNo(),
                "老人「" + order.getElderName() + "」理发服务已完成", "ORDER");
        if (elder != null && elder.getFamilyUserId() != null) {
            notificationService.notify(elder.getFamilyUserId(), "服务完成 " + order.getOrderNo(),
                    "「" + order.getElderName() + "」理发服务已完成，可进行满意度评价", "ORDER");
        }
        return record;
    }

    private void createFollowUp(ServiceOrder order, FollowUpType type, String content) {
        FollowUp followUp = new FollowUp();
        followUp.setElderId(order.getElderId());
        followUp.setElderName(order.getElderName());
        followUp.setOrderId(order.getId());
        followUp.setType(type);
        followUp.setContent(content);
        followUpRepository.save(followUp);
        notificationService.notifyRole(Role.STAFF, "新回访任务",
                "老人「" + order.getElderName() + "」：" + content, "CARE");
    }

    /** 取消预约 */
    @Transactional
    public void cancel(Long orderId, String reason, LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BizException("服务单已完结，不可取消");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        if (operator.role() == Role.BARBER) {
            barberProfileRepository.findByUserId(operator.userId()).ifPresent(p -> {
                p.setCancelCount(p.getCancelCount() + 1);
                p.setCreditScore(Math.max(0, p.getCreditScore() - 3));
                barberProfileRepository.save(p);
            });
        }
        addEvent(orderId, "CANCEL", operator.userId(), operator.realName(), operator.role(), "取消服务单，原因：" + reason);
        notificationService.notifyRole(Role.STAFF, "服务单取消 " + order.getOrderNo(),
                "老人「" + order.getElderName() + "」的预约被取消：" + reason, "ORDER");
    }

    /** 改约（家属改约等）：调整日期与时段，重新校验理发师可用性 */
    @Transactional
    public void reschedule(Long orderId, LocalDate newDate, String newTimeSlot, String reason, LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BizException("服务单已完结，不可改约");
        }
        if (newDate.isBefore(LocalDate.now())) {
            throw new BizException("改约日期不能早于今天");
        }
        Elder elder = elderRepository.findById(order.getElderId()).orElseThrow(() -> new BizException("老人档案不存在"));
        // 校验原理发师新时段可用性，不可用则重新智能匹配
        BarberSchedule chosen = chooseBarber(elder, newDate, newTimeSlot, null);
        User barber = userRepository.findById(chosen.getBarberId()).orElseThrow();
        order.setScheduledDate(newDate);
        order.setTimeSlot(newTimeSlot);
        order.setBarberId(barber.getId());
        order.setBarberName(barber.getRealName());
        order.setRescheduleCount(order.getRescheduleCount() + 1);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        addEvent(orderId, "RESCHEDULE", operator.userId(), operator.realName(), operator.role(),
                "改约至 " + newDate + " " + newTimeSlot + "，原因：" + reason + "；理发师调整为「" + barber.getRealName() + "」");
        notificationService.notify(barber.getId(), "服务单改约 " + order.getOrderNo(),
                newDate + " " + newTimeSlot + " 上门为「" + order.getElderName() + "」理发", "ORDER");
    }

    /** 满意度评价（家属/社区） */
    @Transactional
    public void rate(Long orderId, int rating, String comment, LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BizException("服务完成后才能评价");
        }
        if (rating < 1 || rating > 5) {
            throw new BizException("评分需在 1-5 之间");
        }
        order.setSatisfactionRating(rating);
        order.setSatisfactionComment(comment);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        addEvent(orderId, "RATE", operator.userId(), operator.realName(), operator.role(),
                "满意度评价：" + rating + " 星" + (comment != null && !comment.isBlank() ? "，" + comment : ""));
        if (rating <= 2) {
            createFollowUp(order, FollowUpType.SERVICE, "满意度评价较低（" + rating + " 星），需回访了解原因");
        }
    }

    /** 确认自费部分收款（理发师/社区/财务） */
    @Transactional
    public void confirmPayment(Long orderId, LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BizException("服务完成后才能确认收款");
        }
        if (order.getPaymentStatus() != PaymentStatus.UNPAID && order.getPaymentStatus() != PaymentStatus.DISPUTED) {
            throw new BizException("当前费用状态无需确认");
        }
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        addEvent(orderId, "PAYMENT", operator.userId(), operator.realName(), operator.role(),
                "确认收到自费金额 ¥" + order.getSelfPayAmount());
    }

    // ---------------- 工具方法 ----------------

    public void addEvent(Long orderId, String type, Long actorId, String actorName, Role actorRole, String content) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(orderId);
        event.setEventType(type);
        event.setActorId(actorId);
        event.setActorName(actorName);
        event.setActorRole(actorRole == null ? "SYSTEM" : actorRole.name());
        event.setContent(content);
        eventRepository.save(event);
    }

    private void checkBarber(ServiceOrder order, LoginUserInfo operator) {
        if (operator.role() == Role.ADMIN || operator.role() == Role.STAFF) {
            return;
        }
        if (order.getBarberId() == null || !order.getBarberId().equals(operator.userId())) {
            throw new BizException(403, "仅该服务单的理发师可执行该操作");
        }
    }

    private String typeName(OrderType type) {
        return switch (type) {
            case BIRTHDAY -> "生日理发";
            case EVENT -> "社区活动";
            case PATROL -> "巡访理发";
            default -> "普通预约";
        };
    }

    /** 操作人信息（避免 service 依赖 security 上下文，便于种子数据复用） */
    public record LoginUserInfo(Long userId, String realName, Role role) {
    }
}
