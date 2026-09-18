package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.*;
import com.community.haircut.enums.*;
import com.community.haircut.repository.*;
import org.springframework.context.annotation.Lazy;
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
    private final DisinfectionRecordRepository disinfectionRecordRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final SubsidyRecordRepository subsidyRecordRepository;
    private final FollowUpRepository followUpRepository;
    private final ExceptionRecordRepository exceptionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final InfectionTraceService infectionTraceService;

    private final AtomicInteger orderSeq = new AtomicInteger(0);

    public OrderService(ServiceOrderRepository orderRepository, ElderRepository elderRepository,
                        BarberScheduleRepository scheduleRepository,
                        BarberProfileRepository barberProfileRepository,
                        VolunteerProfileRepository volunteerProfileRepository,
                        ToolKitRepository toolKitRepository, OrderEventRepository eventRepository,
                        ToolConfirmationRepository toolConfirmationRepository,
                        DisinfectionRecordRepository disinfectionRecordRepository,
                        VisitRecordRepository visitRecordRepository,
                        ServiceRecordRepository serviceRecordRepository,
                        SubsidyRecordRepository subsidyRecordRepository,
                        FollowUpRepository followUpRepository,
                        ExceptionRecordRepository exceptionRepository,
                        UserRepository userRepository,
                        NotificationService notificationService,
                        @Lazy InfectionTraceService infectionTraceService) {
        this.orderRepository = orderRepository;
        this.elderRepository = elderRepository;
        this.scheduleRepository = scheduleRepository;
        this.barberProfileRepository = barberProfileRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.toolKitRepository = toolKitRepository;
        this.eventRepository = eventRepository;
        this.toolConfirmationRepository = toolConfirmationRepository;
        this.disinfectionRecordRepository = disinfectionRecordRepository;
        this.visitRecordRepository = visitRecordRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.subsidyRecordRepository = subsidyRecordRepository;
        this.followUpRepository = followUpRepository;
        this.exceptionRepository = exceptionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.infectionTraceService = infectionTraceService;
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
            ToolKit kit = toolKitRepository.findFirstByBarberIdAndKitType(s.getBarberId(), KitType.MAIN).orElse(null);
            if (kit == null || effectiveDisinfectionStatus(kit) != DisinfectionStatus.DISINFECTED) {
                continue;
            }
            // 扫码核验要素必须齐全（消毒方式/消毒柜/责任人/封签），且不存在未补录的使用记录
            BarberProfile profile = barberProfileRepository.findByUserId(s.getBarberId()).orElse(null);
            if (profile == null || !profile.getActive()) {
                continue;
            }
            // 上门资格被暂停（交叉感染/消毒责任处置中）不可派单
            if (Boolean.TRUE.equals(profile.getSuspended())) {
                continue;
            }
            if (!kitCheck(kit).isEmpty()) {
                continue;
            }
            // 评分：楼栋距离 + 当前负载；常驻楼栋优先；派单权重下调者排在后面
            double score = load * 2.0;
            int distance = elder.getBuildingDistance() == null ? 0 : elder.getBuildingDistance();
            score += distance / 500.0;
            if (profile.getServiceBuildings() != null
                    && elder.getBuilding() != null
                    && Arrays.asList(profile.getServiceBuildings().split(",")).contains(elder.getBuilding())) {
                score -= 5;
            }
            score -= profile.getDispatchWeight() / 20.0;
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
            long hours = kit.getValidHours() == null ? DISINFECTION_VALID_HOURS : kit.getValidHours();
            if (kit.getDisinfectedAt().plusHours(hours).isAfter(LocalDateTime.now())) {
                return DisinfectionStatus.DISINFECTED;
            }
            return DisinfectionStatus.EXPIRED;
        }
        return kit.getStatus();
    }

    /**
     * 服务包扫码核验：返回不达标项清单（空列表表示可上门/可派单）。
     * 覆盖：消毒超有效期、封签破损/未加封、消毒方式/消毒柜/责任人缺失、使用后未补录消毒。
     */
    public List<String> kitCheck(ToolKit kit) {
        List<String> problems = new ArrayList<>();
        if (kit == null) {
            problems.add("工具包档案不存在");
            return problems;
        }
        if (effectiveDisinfectionStatus(kit) == DisinfectionStatus.EXPIRED) {
            problems.add("消毒超过有效期（" + (kit.getValidHours() == null ? DISINFECTION_VALID_HOURS : kit.getValidHours()) + " 小时）");
        } else if (effectiveDisinfectionStatus(kit) == DisinfectionStatus.PENDING) {
            problems.add("工具包尚未消毒");
        }
        if (kit.getSealStatus() == SealStatus.BROKEN) {
            problems.add("服务包封签破损");
        }
        if (kit.getSealCode() == null || kit.getSealCode().isBlank()) {
            problems.add("服务包未加封签编号");
        }
        if (kit.getDisinfectionMethod() == null) {
            problems.add("消毒方式未登记");
        }
        if (kit.getCabinetNo() == null || kit.getCabinetNo().isBlank()) {
            problems.add("消毒柜编号未登记");
        }
        if (kit.getResponsiblePerson() == null || kit.getResponsiblePerson().isBlank()) {
            problems.add("消毒责任人未登记");
        }
        // 备用服务包使用后必须补录消毒，未补录前不得再次派单/启用
        if (kit.getKitType() == KitType.SPARE && kit.getLastUsedOrderId() != null) {
            problems.add("备用服务包已使用，消毒记录补录前不可再次使用");
        }
        return problems;
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
        // 备用服务包与使用包消毒记录（供核验未通过时调度备用包、用后补录）
        if (order.getBarberId() != null) {
            map.put("spareKits", toolKitRepository.findByBarberIdAndKitType(order.getBarberId(), KitType.SPARE)
                    .stream().map(k -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("kit", k);
                        m.put("checks", kitCheck(k));
                        return m;
                    }).toList());
        }
        if (order.getUsedKitId() != null) {
            map.put("usedKitDisinfections",
                    disinfectionRecordRepository.findByKitIdOrderByDisinfectedAtDesc(order.getUsedKitId()));
        }
        return map;
    }

    // ---------------- 流程动作 ----------------

    /**
     * 理发师上门前扫码核验 + 工具检查 + 感染风险登记。
     *
     * @param sealCode            扫码封签编号
     * @param sealIntact          封签完好
     * @param disinfectionValid   消毒在有效期内（理发师对照消毒日期判断）
     * @param methodOk            消毒方式可查且合规
     * @param cabinetOk           消毒柜编号可查
     * @param responsibleOk       消毒责任人可查
     * @param toolsOk             理发工具齐备
     * @param toolsClean          剪刀/剃刀无污渍
     * @param capeOk              围布齐备
     * @param clothDry            毛巾/围布干燥未受潮
     * @param disinfectantOk      消毒用品齐备
     * @param packOk              服务包齐备
     * @param missingItems        不达标/遗漏说明
     * @param infectionRisk       是否感染风险（头癣/皮肤病/开放伤口/要求一次性用品）
     * @param infectionRiskReason 感染风险原因
     * @param toolsSeparated      工具单独分装
     * @param postUseHandling     用后处理方式
     * @param disposableUsed      是否使用一次性用品
     */
    @Transactional
    public ToolConfirmation confirmTools(Long orderId, String sealCode, boolean sealIntact, boolean disinfectionValid,
                                         boolean methodOk, boolean cabinetOk, boolean responsibleOk,
                                         boolean toolsOk, boolean toolsClean, boolean capeOk, boolean clothDry,
                                         boolean disinfectantOk, boolean packOk, String missingItems,
                                         boolean infectionRisk, String infectionRiskReason,
                                         boolean toolsSeparated, String postUseHandling, boolean disposableUsed,
                                         LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        checkBarber(order, operator);
        if (order.getStatus() != OrderStatus.ASSIGNED && order.getStatus() != OrderStatus.TOOL_CONFIRMED) {
            throw new BizException("当前状态不可确认工具（需为待上门状态）");
        }
        ToolKit kit = toolKitRepository.findAll().stream()
                .filter(k -> k.getBarberId().equals(order.getBarberId()))
                .filter(k -> sealCode != null && sealCode.trim().equals(k.getSealCode()))
                .findFirst().orElse(null);

        // 服务端按封签编号反查服务包，核验结论以系统记录为准（防止只在前端勾选）
        List<String> serverProblems;
        if (kit == null) {
            serverProblems = new ArrayList<>(List.of("扫码封签编号未匹配到本理发师的服务包"));
        } else {
            serverProblems = new ArrayList<>(kitCheck(kit));
        }
        boolean sealPass = sealIntact && kit != null && kit.getSealStatus() == SealStatus.INTACT;
        boolean itemsPass = toolsOk && toolsClean && capeOk && clothDry && disinfectantOk && packOk;
        boolean scanPass = disinfectionValid && methodOk && cabinetOk && responsibleOk;
        boolean compliant = sealPass && itemsPass && scanPass && serverProblems.isEmpty();

        // 感染风险服务：必须单独分装并记录用后处理；要求一次性用品时必须使用
        if (infectionRisk) {
            if (!toolsSeparated) {
                throw new BizException("该服务单已标记感染风险，工具必须单独分装后再上门");
            }
            if (postUseHandling == null || postUseHandling.isBlank()) {
                throw new BizException("感染风险服务必须记录工具用后处理方式");
            }
            boolean requireDisposable = infectionRiskReason != null && infectionRiskReason.contains("一次性");
            if (requireDisposable && !disposableUsed) {
                throw new BizException("老人明确要求使用一次性用品，请改用一次性用品或备用服务包");
            }
        }

        ToolConfirmation tc = toolConfirmationRepository.findByOrderId(orderId).orElseGet(ToolConfirmation::new);
        tc.setOrderId(orderId);
        tc.setBarberId(operator.userId());
        tc.setKitId(kit == null ? null : kit.getId());
        tc.setSealCode(sealCode);
        tc.setSealIntact(sealIntact);
        tc.setDisinfectionValid(disinfectionValid);
        tc.setMethodOk(methodOk);
        tc.setCabinetOk(cabinetOk);
        tc.setResponsibleOk(responsibleOk);
        tc.setToolsOk(toolsOk);
        tc.setToolsClean(toolsClean);
        tc.setCapeOk(capeOk);
        tc.setClothDry(clothDry);
        tc.setDisinfectantOk(disinfectantOk);
        tc.setPackOk(packOk);
        tc.setMissingItems(missingItems);
        tc.setInfectionRisk(infectionRisk);
        tc.setInfectionRiskReason(infectionRiskReason);
        tc.setToolsSeparated(toolsSeparated);
        tc.setPostUseHandling(postUseHandling);
        tc.setDisposableUsed(disposableUsed);
        tc.setConfirmedAt(LocalDateTime.now());
        toolConfirmationRepository.save(tc);

        // 感染风险标记同步到服务单
        order.setInfectionRisk(infectionRisk);
        order.setInfectionRiskReason(infectionRiskReason);
        order.setPostUseHandling(postUseHandling);

        if (compliant) {
            order.setStatus(OrderStatus.TOOL_CONFIRMED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            addEvent(orderId, "TOOL_CONFIRM", operator.userId(), operator.realName(), operator.role(),
                    "上门前扫码核验通过：封签 " + sealCode + "，消毒日期/方式/消毒柜/责任人均可查"
                            + (infectionRisk ? "；⚠ 感染风险服务，工具已单独分装，用后处理：" + postUseHandling : ""));
            if (infectionRisk) {
                notificationService.notifyRole(Role.STAFF, "感染风险服务单 " + order.getOrderNo(),
                        "老人「" + order.getElderName() + "」标记感染风险（" + infectionRiskReason
                                + "），工具单独分装，用后处理：" + postUseHandling, "INFECTION");
            }
        } else {
            // 不达标：不允许推进状态，保持待上门，记录问题并通知社区，引导改约或启用备用服务包
            List<String> all = new ArrayList<>(serverProblems);
            if (!sealIntact || (kit != null && kit.getSealStatus() == SealStatus.BROKEN)) {
                all.add("封签破损");
            }
            if (!toolsClean) {
                all.add("剪刀/剃刀有污渍");
            }
            if (!clothDry) {
                all.add("毛巾/围布受潮");
            }
            if (!toolsOk || !capeOk || !disinfectantOk || !packOk) {
                all.add("工具/围布/消毒用品/服务包遗漏：" + (missingItems == null ? "未说明" : missingItems));
            }
            if (!disinfectionValid) {
                all.add("消毒超过有效期");
            }
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            addEvent(orderId, "TOOL_CONFIRM", operator.userId(), operator.realName(), operator.role(),
                    "上门前扫码核验未通过，不允许开始服务：" + String.join("；", new LinkedHashSet<>(all))
                            + "。请联系社区改约或启用备用服务包");
            notificationService.notifyRole(Role.STAFF, "工具核验未通过 " + order.getOrderNo(),
                    "理发师「" + order.getBarberName() + "」上门核验不达标：" + String.join("；", new LinkedHashSet<>(all))
                            + "，需协助改约或调度备用服务包", "TOOL");
            throw new BizException("核验未通过（" + String.join("；", new LinkedHashSet<>(all))
                    + "），不允许开始服务，请先联系社区改约或启用备用服务包");
        }
        return tc;
    }

    /**
     * 启用社区备用服务包：仅可在核验未通过、服务尚未开始时使用；
     * 备用包本身须封签完好、消毒在有效期内；使用后须补录消毒，未补录前不得再次派单。
     */
    @Transactional
    public ServiceOrder activateSpareKit(Long orderId, Long spareKitId, LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        checkBarber(order, operator);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BizException("服务单已完结，不可启用备用服务包");
        }
        ToolKit kit = toolKitRepository.findById(spareKitId)
                .orElseThrow(() -> new BizException("备用服务包不存在"));
        if (kit.getKitType() != KitType.SPARE) {
            throw new BizException("该工具包不是备用服务包");
        }
        if (!kit.getBarberId().equals(order.getBarberId())) {
            throw new BizException("备用服务包不属于该理发师");
        }
        List<String> problems = kitCheck(kit);
        if (!problems.isEmpty()) {
            throw new BizException("备用服务包不可用：" + String.join("；", problems));
        }
        order.setSpareUsed(true);
        order.setStatus(OrderStatus.TOOL_CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 同步/补建工具核验单
        ToolConfirmation tc = toolConfirmationRepository.findByOrderId(orderId).orElseGet(ToolConfirmation::new);
        tc.setOrderId(orderId);
        tc.setBarberId(operator.userId());
        tc.setKitId(kit.getId());
        tc.setSealCode(kit.getSealCode());
        tc.setSealIntact(true);
        tc.setDisinfectionValid(true);
        tc.setMethodOk(true);
        tc.setCabinetOk(true);
        tc.setResponsibleOk(true);
        tc.setToolsOk(true);
        tc.setToolsClean(true);
        tc.setCapeOk(true);
        tc.setClothDry(true);
        tc.setDisinfectantOk(true);
        tc.setPackOk(true);
        toolConfirmationRepository.save(tc);

        addEvent(orderId, "TOOL_CONFIRM", operator.userId(), operator.realName(), operator.role(),
                "原工具核验不达标，启用备用服务包「" + kit.getName() + "」（封签 " + kit.getSealCode()
                        + "）；使用后须补录消毒，未补录前该备用包不得再次使用");
        notificationService.notifyRole(Role.STAFF, "已启用备用服务包 " + order.getOrderNo(),
                "理发师「" + order.getBarberName() + "」启用备用包「" + kit.getName() + "」，请跟进用后消毒补录", "TOOL");
        return order;
    }

    /** 因工具问题联系社区改约（不算老人违约、不扣补贴），重新匹配可用理发师 */
    @Transactional
    public void rescheduleForToolIssue(Long orderId, LocalDate newDate, String newTimeSlot, String reason,
                                       LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BizException("服务单已完结，不可改约");
        }
        if (newDate.isBefore(LocalDate.now())) {
            throw new BizException("改约日期不能早于今天");
        }
        Elder elder = elderRepository.findById(order.getElderId()).orElseThrow(() -> new BizException("老人档案不存在"));
        BarberSchedule chosen = chooseBarber(elder, newDate, newTimeSlot, null);
        User barber = userRepository.findById(chosen.getBarberId()).orElseThrow();
        order.setScheduledDate(newDate);
        order.setTimeSlot(newTimeSlot);
        order.setBarberId(barber.getId());
        order.setBarberName(barber.getRealName());
        order.setStatus(OrderStatus.ASSIGNED);
        order.setRescheduleCount(order.getRescheduleCount() + 1);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        addEvent(orderId, "RESCHEDULE", operator.userId(), operator.realName(), operator.role(),
                "因工具消毒/封签问题联系社区改约至 " + newDate + " " + newTimeSlot
                        + "（不记老人违约、不扣补贴），理发师调整为「" + barber.getRealName() + "」；原因：" + reason);
        notificationService.notify(barber.getId(), "改派服务单 " + order.getOrderNo(),
                newDate + " " + newTimeSlot + " 上门为「" + order.getElderName() + "」理发（原单因工具问题改约）", "ORDER");
        notificationService.notifyRole(Role.STAFF, "工具问题改约 " + order.getOrderNo(),
                "老人「" + order.getElderName() + "」因工具问题改约，已改派理发师「" + barber.getRealName() + "」", "TOOL");
    }

    /**
     * 因工具问题取消：不算老人违约、不扣补贴；标记理发师空跑，待社区认定补偿。
     */
    @Transactional
    public void cancelForToolIssue(Long orderId, String reason, boolean emptyRun, LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BizException("服务单已完结，不可取消");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelSource("TOOL_ISSUE");
        order.setCancelReason("工具问题取消：" + reason);
        order.setEmptyRun(emptyRun);
        if (emptyRun) {
            order.setCompensationStatus(CompensationStatus.PENDING);
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        addEvent(orderId, "CANCEL", operator.userId(), operator.realName(), operator.role(),
                "因工具问题取消（不记老人违约、不扣补贴）：" + reason
                        + (emptyRun ? "；理发师已空跑，待社区按规则认定补偿" : ""));
        notificationService.notifyRole(Role.STAFF, "工具问题取消 " + order.getOrderNo(),
                "老人「" + order.getElderName() + "」的预约因工具问题取消：" + reason
                        + (emptyRun ? "，理发师空跑补偿待认定" : ""), "TOOL");
        if (emptyRun) {
            notificationService.notifyRole(Role.FINANCE, "理发师空跑补偿待认定 " + order.getOrderNo(),
                    "理发师「" + order.getBarberName() + "」因工具问题空跑，请社区/财务认定补偿承担方", "FINANCE");
        }
    }

    /**
     * 社区/财务认定空跑补偿：工具遗漏或消毒失责由理发师承担，其他按社区规则由公益资金补偿。
     */
    @Transactional
    public void adjudicateCompensation(Long orderId, CompensationStatus result, java.math.BigDecimal amount,
                                       String note, LoginUserInfo operator) {
        ServiceOrder order = get(orderId);
        if (!Boolean.TRUE.equals(order.getEmptyRun())) {
            throw new BizException("该服务单未标记空跑，无需认定补偿");
        }
        if (result != CompensationStatus.COMMUNITY_APPROVED
                && result != CompensationStatus.BARBER_BORNE && result != CompensationStatus.WAIVED) {
            throw new BizException("认定结果不合法");
        }
        order.setCompensationStatus(result);
        order.setCompensationAmount(amount);
        order.setCompensationNote(note);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        String text = switch (result) {
            case COMMUNITY_APPROVED -> "社区规则认定公益资金补偿 ¥" + (amount == null ? 0 : amount);
            case BARBER_BORNE -> "认定属工具遗漏/消毒失责，空跑损失由理发师自行承担";
            default -> "认定无需补偿";
        };
        addEvent(orderId, "PAYMENT", operator.userId(), operator.realName(), operator.role(),
                "空跑补偿认定：" + text + (note == null || note.isBlank() ? "" : "；" + note));
        if (order.getBarberId() != null) {
            notificationService.notify(order.getBarberId(), "空跑补偿认定结果 " + order.getOrderNo(), text, "FINANCE");
        }
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
            throw new BizException("当前状态不可开始服务（需先通过扫码核验并由志愿者完成进门核对）");
        }
        // 暂停上门资格的理发师不得开始服务
        barberProfileRepository.findByUserId(order.getBarberId()).ifPresent(p -> {
            if (Boolean.TRUE.equals(p.getSuspended())) {
                throw new BizException("该理发师上门资格已暂停，不得开始服务");
            }
        });
        // 锁定本次实际使用的服务包与工具编号快照（供交叉感染反查）
        ToolConfirmation tc = toolConfirmationRepository.findByOrderId(orderId).orElse(null);
        if (tc != null && tc.getKitId() != null) {
            ToolKit kit = toolKitRepository.findById(tc.getKitId()).orElse(null);
            if (kit != null) {
                order.setUsedKitId(kit.getId());
                order.setUsedKitName(kit.getName());
                order.setUsedToolItems(kit.getItems());
                order.setUsedKitDisinfectedAt(kit.getDisinfectedAt());
                order.setUsedKitCompliant(kitCheck(kit).isEmpty());
            }
        }
        order.setStatus(OrderStatus.IN_SERVICE);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        addEvent(orderId, "START", operator.userId(), operator.realName(), operator.role(),
                "理发师开始理发服务；使用工具包「" + order.getUsedKitName() + "」，工具编号：" + order.getUsedToolItems()
                        + (Boolean.TRUE.equals(order.getInfectionRisk()) ? "；感染风险服务，工具单独分装" : ""));
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

        // 独居老人服务后回访；有异常的服务单生成异常回访；感染风险服务生成专项回访
        Elder elder = elderRepository.findById(order.getElderId()).orElse(null);
        if (Boolean.TRUE.equals(order.getLivingAlone())) {
            createFollowUp(order, FollowUpType.SERVICE,
                    "独居老人服务后回访：确认进门安全、老人精神状态与后续关怀需求");
        }
        if (Boolean.TRUE.equals(order.getHasException())) {
            createFollowUp(order, FollowUpType.EXCEPTION, "服务单存在异常，需回访确认老人状况与处理结果");
        }
        if (Boolean.TRUE.equals(order.getInfectionRisk())) {
            createFollowUp(order, FollowUpType.EXCEPTION,
                    "感染风险服务回访（" + order.getInfectionRiskReason() + "）：确认皮肤状况无瘙痒/红疹，工具已按" + order.getPostUseHandling() + "处理");
        }

        // 标记实际使用包：备用包使用后锁定，补录消毒前不得再次派单；未达标工具完成服务自动上报
        if (order.getUsedKitId() != null) {
            toolKitRepository.findById(order.getUsedKitId()).ifPresent(kit -> {
                kit.setLastUsedOrderId(order.getId());
                kit.setLastUsedAt(LocalDateTime.now());
                toolKitRepository.save(kit);
            });
        }
        if (Boolean.FALSE.equals(order.getUsedKitCompliant())) {
            order.setHasException(true);
            orderRepository.save(order);
            ExceptionRecord er = new ExceptionRecord();
            er.setOrderId(order.getId());
            er.setType(ExceptionType.TOOL_NONCOMPLIANT_USE);
            er.setReportedById(operator.userId());
            er.setReportedByName(operator.realName());
            er.setReportedByRole(operator.role() == null ? "SYSTEM" : operator.role().name());
            er.setDescription("已使用未达标工具完成服务（工具包「" + order.getUsedKitName() + "」），需按理发师/服务包/工具编号反查同日服务老人");
            er.setFinanceInvolved(false);
            exceptionRepository.save(er);
            addEvent(orderId, "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                    "上报异常【已用未达标工具完成服务】：工具包「" + order.getUsedKitName() + "」，请社区发起交叉感染追溯");
            // 自动按理发师/服务包/工具编号/同日服务反查
            infectionTraceService.createCase(order.getId(), "已用未达标工具完成服务",
                    "工具包「" + order.getUsedKitName() + "」消毒/封签核验不达标，仍完成服务，系统自动发起追溯", operator);
            notificationService.notifyRole(Role.STAFF, "未达标工具使用告警 " + order.getOrderNo(),
                    "理发师「" + order.getBarberName() + "」使用未达标工具完成服务，需立即追溯同日同工具服务老人", "INFECTION");
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
        order.setCancelSource(operator.role() == Role.BARBER ? "BARBER_ISSUE"
                : operator.role() == Role.FAMILY ? "FAMILY" : "ELDER");
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
