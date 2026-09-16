package com.community.haircut.config;

import com.community.haircut.entity.*;
import com.community.haircut.enums.*;
import com.community.haircut.repository.*;
import com.community.haircut.service.CareTaskService;
import com.community.haircut.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 首次启动时初始化演示账号与演示业务数据。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final BarberProfileRepository barberProfileRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final ToolKitRepository toolKitRepository;
    private final BarberScheduleRepository scheduleRepository;
    private final ElderRepository elderRepository;
    private final ServiceOrderRepository orderRepository;
    private final OrderEventRepository eventRepository;
    private final ToolConfirmationRepository toolConfirmationRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final SubsidyRecordRepository subsidyRepository;
    private final ExceptionRecordRepository exceptionRepository;
    private final FollowUpRepository followUpRepository;
    private final CommunityEventRepository event2Repository;
    private final PasswordEncoder passwordEncoder;
    private final CareTaskService careTaskService;

    public DataInitializer(UserRepository userRepository, BarberProfileRepository barberProfileRepository,
                           VolunteerProfileRepository volunteerProfileRepository, ToolKitRepository toolKitRepository,
                           BarberScheduleRepository scheduleRepository, ElderRepository elderRepository,
                           ServiceOrderRepository orderRepository, OrderEventRepository eventRepository,
                           ToolConfirmationRepository toolConfirmationRepository,
                           VisitRecordRepository visitRecordRepository, ServiceRecordRepository serviceRecordRepository,
                           SubsidyRecordRepository subsidyRepository, ExceptionRecordRepository exceptionRepository,
                           FollowUpRepository followUpRepository, CommunityEventRepository event2Repository,
                           PasswordEncoder passwordEncoder, CareTaskService careTaskService) {
        this.userRepository = userRepository;
        this.barberProfileRepository = barberProfileRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.toolKitRepository = toolKitRepository;
        this.scheduleRepository = scheduleRepository;
        this.elderRepository = elderRepository;
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.toolConfirmationRepository = toolConfirmationRepository;
        this.visitRecordRepository = visitRecordRepository;
        this.serviceRecordRepository = serviceRecordRepository;
        this.subsidyRepository = subsidyRepository;
        this.exceptionRepository = exceptionRepository;
        this.followUpRepository = followUpRepository;
        this.event2Repository = event2Repository;
        this.passwordEncoder = passwordEncoder;
        this.careTaskService = careTaskService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("初始化演示数据...");

        // ---------- 用户 ----------
        User admin = user("admin", "admin123", "系统管理员", Role.ADMIN, "13800000001");
        User staff = user("staff", "staff123", "王社区", Role.STAFF, "13800000002");
        User barber1 = user("barber1", "barber123", "李理发", Role.BARBER, "13800000003");
        User barber2 = user("barber2", "barber123", "赵理发", Role.BARBER, "13800000004");
        User barber3 = user("barber3", "barber123", "孙理发", Role.BARBER, "13800000005");
        User volunteer1 = user("volunteer1", "volunteer123", "陈志愿", Role.VOLUNTEER, "13800000006");
        User volunteer2 = user("volunteer2", "volunteer123", "刘志愿", Role.VOLUNTEER, "13800000007");
        User finance = user("finance", "finance123", "钱财务", Role.FINANCE, "13800000008");
        User grid = user("grid", "grid123", "周网格", Role.GRID, "13800000009");
        User family = user("family", "family123", "张家属", Role.FAMILY, "13800000010");

        // ---------- 理发师档案与工具包 ----------
        barberProfile(barber1, "男士短发,女士短发,剃头", "1栋,2栋,3栋", 96, 5, 0, 0, 0);
        barberProfile(barber2, "女士短发,染发,修面", "4栋,5栋,6栋", 88, 4, 0, 1, 1);
        barberProfile(barber3, "男士短发", "1栋,4栋", 100, 0, 0, 0, 0);
        volunteerProfile(volunteer1, 6);
        volunteerProfile(volunteer2, 4);

        toolKit(barber1, "标准理发工具包A", "剪刀,推子,梳子,围布,消毒喷雾,服务包",
                DisinfectionStatus.DISINFECTED, LocalDateTime.now().minusHours(2));
        toolKit(barber2, "标准理发工具包B", "剪刀,推子,梳子,围布,消毒喷雾,服务包",
                DisinfectionStatus.DISINFECTED, LocalDateTime.now().minusHours(20));
        toolKit(barber3, "标准理发工具包C", "剪刀,推子,梳子,围布",
                DisinfectionStatus.PENDING, LocalDateTime.now().minusDays(4));

        // ---------- 排班：今天起 14 天 ----------
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 14; i++) {
            LocalDate d = today.plusDays(i);
            schedule(barber1, d, "08:00", "12:00", 4);
            schedule(barber1, d, "14:00", "18:00", 4);
            schedule(barber2, d, "08:00", "12:00", 4);
            schedule(barber2, d, "14:00", "18:00", 4);
            schedule(barber3, d, "09:00", "12:00", 3);
        }

        // ---------- 老人档案 ----------
        Elder e1 = elder("张桂英", "女", today.minusYears(82).minusMonths(1), "1栋", "2单元", "301", 200, true,
                "高血压、腿脚不便", RiskLevel.HIGH, "拄拐", true, "齐耳短发，不要剃太短", "上午",
                SubsidyType.FULL, "张家属", "13800000010", family.getId(), true, 30);
        Elder e2 = elder("王建国", "男", today.minusYears(78).minusMonths(3), "1栋", "1单元", "502", 200, false,
                "身体健康", RiskLevel.LOW, "自如", false, "平头", "下午",
                SubsidyType.NONE, "王儿子", "13900000001", null, false, 30);
        Elder e3 = elder("李秀兰", "女", today.minusYears(85).minusMonths(5), "2栋", "1单元", "201", 450, true,
                "糖尿病、半失能", RiskLevel.HIGH, "轮椅", true, "短发，注意保暖", "上午",
                SubsidyType.FULL, "李女儿", "13900000002", null, true, 30);
        Elder e4 = elder("赵德柱", "男", today.minusYears(75).minusMonths(7), "2栋", "3单元", "102", 450, false,
                "轻微关节炎", RiskLevel.MID, "自如", false, "寸头", "下午",
                SubsidyType.PARTIAL, "赵儿子", "13900000003", null, false, 30);
        Elder e5 = elder("孙玉梅", "女", today.minusYears(80).minusMonths(9), "3栋", "1单元", "401", 700, false,
                "心脏支架术后", RiskLevel.MID, "拄拐", true, "女士短发", "上午",
                SubsidyType.PARTIAL, "孙女儿", "13900000004", null, false, 30);
        Elder e6 = elder("周福生", "男", today.minusYears(88).minusMonths(11), "3栋", "2单元", "602", 700, true,
                "行动不便、耳背", RiskLevel.HIGH, "轮椅", false, "剃光头", "上午",
                SubsidyType.FULL, "周侄子", "13900000005", null, true, 30);
        Elder e7 = elder("吴彩霞", "女", today.minusYears(76).minusMonths(2), "4栋", "1单元", "303", 900, false,
                "身体健康", RiskLevel.LOW, "自如", false, "齐肩短发", "下午",
                SubsidyType.NONE, "吴儿子", "13900000006", null, false, 30);
        Elder e8 = elder("郑永康", "男", today.minusYears(79).minusMonths(4), "4栋", "2单元", "202", 900, false,
                "慢性支气管炎", RiskLevel.MID, "自如", false, "平头", "上午",
                SubsidyType.PARTIAL, "郑女儿", "13900000007", null, false, 30);
        Elder e9 = elder("钱淑华", "女", today.minusYears(83).minusMonths(6), "5栋", "1单元", "501", 1200, false,
                "皮肤敏感、高血压", RiskLevel.HIGH, "拄拐", true, "短发，动作轻", "上午",
                SubsidyType.FULL, "钱儿子", "13900000008", null, false, 30);
        Elder e10 = elder("冯长顺", "男", today.minusYears(77).minusMonths(8), "5栋", "2单元", "301", 1200, false,
                "身体健康", RiskLevel.LOW, "自如", false, "寸头", "下午",
                SubsidyType.NONE, "冯女儿", "13900000009", null, false, 30);
        Elder e11 = elder("陈月娥", "女", today.minusYears(81).plusDays(3), "6栋", "1单元", "101", 1500, false,
                "轻微骨质疏松", RiskLevel.MID, "自如", false, "女士短发", "上午",
                SubsidyType.PARTIAL, "陈儿子", "13900000010", null, false, 30);
        Elder e12 = elder("褚寿山", "男", today.minusYears(90).plusDays(5), "6栋", "2单元", "401", 1500, true,
                "高龄、听力下降", RiskLevel.HIGH, "拄拐", false, "剃头", "上午",
                SubsidyType.FULL, "褚孙子", "13900000011", null, true, 30);

        // ---------- 历史服务单 ----------
        // 已完成：王建国（低风险无补贴，自费已付）
        ServiceOrder o1 = seedOrder("HF" + today.minusDays(10).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001",
                e2, barber1, null, OrderType.NORMAL, today.minusDays(10), "14:00-16:00",
                bd(30), bd(0), bd(30), PaymentStatus.PAID, 5, "手艺很好，下次还约");
        completeSeed(o1, barber1, null, false);
        // 已完成：张桂英（全额补贴，补贴已发放）
        ServiceOrder o2 = seedOrder("HF" + today.minusDays(7).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001",
                e1, barber1, volunteer1, OrderType.NORMAL, today.minusDays(7), "09:00-11:00",
                bd(30), bd(30), bd(0), PaymentStatus.WAIVED, 5, "服务很贴心");
        completeSeed(o2, barber1, volunteer1, true);
        approveSeedSubsidy(o2, finance);
        // 已完成：赵德柱（部分补贴，补贴待审核）
        ServiceOrder o3 = seedOrder("HF" + today.minusDays(5).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001",
                e4, barber2, volunteer2, OrderType.NORMAL, today.minusDays(5), "14:00-16:00",
                bd(30), bd(20), bd(10), PaymentStatus.PAID, 4, "不错");
        completeSeed(o3, barber2, volunteer2, true);
        // 已完成但有异常：钱淑华（皮肤划伤已解决，异常回访待办）
        ServiceOrder o4 = seedOrder("HF" + today.minusDays(3).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001",
                e9, barber2, volunteer1, OrderType.NORMAL, today.minusDays(3), "09:00-11:00",
                bd(30), bd(30), bd(0), PaymentStatus.WAIVED, 3, "过程有点磕碰");
        o4.setHasException(true);
        orderRepository.save(o4);
        completeSeed(o4, barber2, volunteer1, true);
        seedException(o4, ExceptionType.SKIN_CUT, staff, "理发过程中老人耳后轻微划伤，已现场消毒处理",
                "已送医检查无大碍，理发师加强培训并扣除信用分", true);
        seedFollowUp(e9, o4, FollowUpType.EXCEPTION, "皮肤划伤异常回访：确认伤口愈合情况与老人情绪");
        // 已完成：李秀兰（独居，服务回访待办）
        ServiceOrder o5 = seedOrder("HF" + today.minusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001",
                e3, barber1, volunteer1, OrderType.NORMAL, today.minusDays(2), "09:00-11:00",
                bd(30), bd(30), bd(0), PaymentStatus.WAIVED, 5, "非常感谢");
        completeSeed(o5, barber1, volunteer1, true);
        seedFollowUp(e3, o5, FollowUpType.SERVICE, "独居老人服务后回访：确认进门安全、老人精神状态与后续关怀需求");
        // 75 天前完成：冯长顺（触发长期未预约关怀）
        ServiceOrder o6 = seedOrder("HF" + today.minusDays(75).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001",
                e10, barber1, null, OrderType.NORMAL, today.minusDays(75), "14:00-16:00",
                bd(30), bd(0), bd(30), PaymentStatus.PAID, 5, null);
        completeSeed(o6, barber1, null, false);
        // 连续取消：吴彩霞（触发连续取消关怀）
        seedCancelled(e7, barber2, today.minusDays(3), "老人临时去医院复查");
        seedCancelled(e7, barber2, today.minusDays(1), "家属要求改期");
        // 今天待上门：张桂英（供理发师/志愿者演示流程）
        ServiceOrder todayOrder = seedOrder("HF" + today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001",
                e1, barber1, volunteer1, OrderType.NORMAL, today, "09:00-11:00",
                bd(30), bd(30), bd(0), PaymentStatus.WAIVED, null, null);
        seedEvent(todayOrder, "CREATE", staff, "创建普通预约服务单，派单给理发师「李理发」，志愿者「陈志愿」陪同；补贴 ¥30，自费 ¥0");
        // 明天待上门：孙玉梅
        ServiceOrder tomorrowOrder = seedOrder("HF" + today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-002",
                e5, barber2, volunteer2, OrderType.NORMAL, today.plusDays(1), "09:00-11:00",
                bd(30), bd(20), bd(10), PaymentStatus.UNPAID, null, null);
        seedEvent(tomorrowOrder, "CREATE", staff, "创建普通预约服务单，派单给理发师「赵理发」，志愿者「刘志愿」陪同；补贴 ¥20，自费 ¥10");

        // ---------- 社区活动 ----------
        CommunityEvent event = new CommunityEvent();
        event.setTitle("重阳节爱心理发集中服务");
        event.setEventDate(today.plusDays(10));
        event.setTimeSlot("09:00-11:00");
        event.setLocation("社区活动中心一楼大厅");
        event.setMaxElders(20);
        event.setNotes("集中服务，纯公益免费，欢迎社区老人报名");
        event2Repository.save(event);

        // ---------- 自动扫描关怀任务 ----------
        int tasks = careTaskService.scan();
        log.info("演示数据初始化完成，自动生成关怀任务 {} 条", tasks);
    }

    // ---------------- 辅助方法 ----------------

    private User user(String username, String password, String realName, Role role, String phone) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setRealName(realName);
        u.setRole(role);
        u.setPhone(phone);
        return userRepository.save(u);
    }

    private void barberProfile(User barber, String skills, String buildings, int credit,
                               int completed, int cancel, int late, int incident) {
        BarberProfile p = new BarberProfile();
        p.setUserId(barber.getId());
        p.setSkillTags(skills);
        p.setServiceBuildings(buildings);
        p.setCreditScore(credit);
        p.setCompletedCount(completed);
        p.setCancelCount(cancel);
        p.setLateCount(late);
        p.setIncidentCount(incident);
        barberProfileRepository.save(p);
    }

    private void volunteerProfile(User volunteer, int serviceCount) {
        VolunteerProfile p = new VolunteerProfile();
        p.setUserId(volunteer.getId());
        p.setServiceCount(serviceCount);
        volunteerProfileRepository.save(p);
    }

    private void toolKit(User barber, String name, String items, DisinfectionStatus status, LocalDateTime disinfectedAt) {
        ToolKit kit = new ToolKit();
        kit.setBarberId(barber.getId());
        kit.setName(name);
        kit.setItems(items);
        kit.setStatus(status);
        kit.setDisinfectedAt(disinfectedAt);
        toolKitRepository.save(kit);
    }

    private void schedule(User barber, LocalDate date, String start, String end, int maxOrders) {
        BarberSchedule s = new BarberSchedule();
        s.setBarberId(barber.getId());
        s.setWorkDate(date);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setMaxOrders(maxOrders);
        scheduleRepository.save(s);
    }

    private Elder elder(String name, String gender, LocalDate birthDate, String building, String unit, String room,
                        int distance, boolean livingAlone, String health, RiskLevel risk, String mobility,
                        boolean needFamily, String hairPref, String preferredTime, SubsidyType subsidyType,
                        String familyName, String familyPhone, Long familyUserId, boolean patrol, int patrolDays) {
        Elder e = new Elder();
        e.setName(name);
        e.setGender(gender);
        e.setBirthDate(birthDate);
        e.setPhone("137" + String.format("%08d", Math.abs(name.hashCode()) % 100000000));
        e.setCommunity("阳光社区");
        e.setBuilding(building);
        e.setUnit(unit);
        e.setRoom(room);
        e.setAddress("阳光社区" + building + unit + room + "室");
        e.setBuildingDistance(distance);
        e.setLivingAlone(livingAlone);
        e.setHealthCondition(health);
        e.setRiskLevel(risk);
        e.setMobility(mobility);
        e.setNeedFamilyPresent(needFamily);
        e.setHairPreference(hairPref);
        e.setPreferredTime(preferredTime);
        e.setSubsidyType(subsidyType);
        e.setFamilyContactName(familyName);
        e.setFamilyContactPhone(familyPhone);
        e.setFamilyUserId(familyUserId);
        e.setPatrolEnabled(patrol);
        e.setPatrolIntervalDays(patrolDays);
        if (patrol) {
            e.setLastPatrolAt(LocalDate.now().minusDays(20));
        }
        return elderRepository.save(e);
    }

    private ServiceOrder seedOrder(String orderNo, Elder elder, User barber, User volunteer, OrderType type,
                                   LocalDate date, String slot, BigDecimal total, BigDecimal subsidy,
                                   BigDecimal selfPay, PaymentStatus paymentStatus,
                                   Integer rating, String ratingComment) {
        ServiceOrder o = new ServiceOrder();
        o.setOrderNo(orderNo);
        o.setElderId(elder.getId());
        o.setElderName(elder.getName());
        o.setBarberId(barber.getId());
        o.setBarberName(barber.getRealName());
        if (volunteer != null) {
            o.setVolunteerId(volunteer.getId());
            o.setVolunteerName(volunteer.getRealName());
        }
        o.setType(type);
        o.setScheduledDate(date);
        o.setTimeSlot(slot);
        o.setAddress(elder.getAddress());
        o.setNeedFamilyPresent(elder.getNeedFamilyPresent());
        o.setRiskLevel(elder.getRiskLevel());
        o.setLivingAlone(elder.getLivingAlone());
        o.setTotalAmount(total);
        o.setSubsidyAmount(subsidy);
        o.setSelfPayAmount(selfPay);
        o.setPaymentStatus(paymentStatus);
        o.setSatisfactionRating(rating);
        o.setSatisfactionComment(ratingComment);
        o.setCreatedAt(date.atTime(8, 0));
        o.setUpdatedAt(date.atTime(8, 0));
        return orderRepository.save(o);
    }

    /** 把种子单推进到已完成，并补齐工具确认/陪同/服务档案/补贴记录 */
    private void completeSeed(ServiceOrder order, User barber, User volunteer, boolean withSubsidy) {
        LocalDate date = order.getScheduledDate();
        ToolConfirmation tc = new ToolConfirmation();
        tc.setOrderId(order.getId());
        tc.setBarberId(barber.getId());
        tc.setToolsOk(true);
        tc.setCapeOk(true);
        tc.setDisinfectantOk(true);
        tc.setPackOk(true);
        tc.setConfirmedAt(date.atTime(8, 30));
        toolConfirmationRepository.save(tc);

        if (volunteer != null) {
            VisitRecord vr = new VisitRecord();
            vr.setOrderId(order.getId());
            vr.setVolunteerId(volunteer.getId());
            vr.setEntrySafe(true);
            vr.setElderState("老人身体状态平稳，可以理发");
            vr.setMentalState("良好");
            vr.setFamilyAuthorized(true);
            vr.setNotes("陪同进门顺利");
            vr.setCreatedAt(date.atTime(9, 0));
            visitRecordRepository.save(vr);
        }

        ServiceRecord sr = new ServiceRecord();
        sr.setOrderId(order.getId());
        sr.setTotalAmount(order.getTotalAmount());
        sr.setSubsidyAmount(order.getSubsidyAmount());
        sr.setSelfPayAmount(order.getSelfPayAmount());
        sr.setPaymentNote(order.getSelfPayAmount().compareTo(BigDecimal.ZERO) > 0 ? "自费部分已现金收讫" : "全额补贴，无需自费");
        sr.setCompletedAt(date.atTime(10, 0));
        serviceRecordRepository.save(sr);

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(date.atTime(10, 0));
        orderRepository.save(order);

        seedEvent(order, "CREATE", null, "创建服务单并派单");
        seedEvent(order, "TOOL_CONFIRM", barber, "上门前确认完成：理发工具、围布、消毒用品、服务包均已备齐");
        if (volunteer != null) {
            seedEvent(order, "CHECK_IN", volunteer, "志愿者陪同进门：进门安全=正常，老人精神状态=良好，家属授权=已确认");
        }
        seedEvent(order, "COMPLETE", barber, "服务完成，剪发照片已入档");

        if (withSubsidy && order.getSubsidyAmount().compareTo(BigDecimal.ZERO) > 0) {
            SubsidyRecord subsidy = new SubsidyRecord();
            subsidy.setOrderId(order.getId());
            subsidy.setElderId(order.getElderId());
            subsidy.setElderName(order.getElderName());
            subsidy.setAmount(order.getSubsidyAmount());
            subsidy.setCreatedAt(date.atTime(10, 5));
            subsidyRepository.save(subsidy);
        }
    }

    private void approveSeedSubsidy(ServiceOrder order, User finance) {
        subsidyRepository.findByOrderId(order.getId()).forEach(s -> {
            s.setStatus(SubsidyStatus.APPROVED);
            s.setReviewedBy(finance.getRealName());
            s.setReviewedAt(LocalDateTime.now().minusDays(5));
            s.setNote("资格核验通过");
            subsidyRepository.save(s);
        });
    }

    private void seedCancelled(Elder elder, User barber, LocalDate date, String reason) {
        ServiceOrder o = seedOrder("HF" + date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-C" + elder.getId() + date.getDayOfMonth(),
                elder, barber, null, OrderType.NORMAL, date, "14:00-16:00",
                bd(30), bd(0), bd(30), PaymentStatus.UNPAID, null, null);
        o.setStatus(OrderStatus.CANCELLED);
        o.setCancelReason(reason);
        orderRepository.save(o);
        seedEvent(o, "CANCEL", null, "取消服务单，原因：" + reason);
    }

    private void seedException(ServiceOrder order, ExceptionType type, User reporter, String description,
                               String resolution, boolean financeInvolved) {
        ExceptionRecord record = new ExceptionRecord();
        record.setOrderId(order.getId());
        record.setType(type);
        record.setReportedById(reporter.getId());
        record.setReportedByName(reporter.getRealName());
        record.setReportedByRole(reporter.getRole().name());
        record.setDescription(description);
        record.setStatus(ExceptionStatus.RESOLVED);
        record.setResolution(resolution);
        record.setResolvedAt(order.getScheduledDate().atTime(18, 0));
        record.setFinanceInvolved(financeInvolved);
        record.setCreatedAt(order.getScheduledDate().atTime(10, 30));
        exceptionRepository.save(record);
        seedEvent(order, "EXCEPTION", reporter, "上报异常【皮肤划伤】：" + description);
        seedEvent(order, "EXCEPTION", reporter, "异常【皮肤划伤】已解决：" + resolution);
    }

    private void seedFollowUp(Elder elder, ServiceOrder order, FollowUpType type, String content) {
        FollowUp f = new FollowUp();
        f.setElderId(elder.getId());
        f.setElderName(elder.getName());
        f.setOrderId(order.getId());
        f.setType(type);
        f.setContent(content);
        followUpRepository.save(f);
    }

    private void seedEvent(ServiceOrder order, String type, User actor, String content) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(order.getId());
        event.setEventType(type);
        if (actor != null) {
            event.setActorId(actor.getId());
            event.setActorName(actor.getRealName());
            event.setActorRole(actor.getRole().name());
        } else {
            event.setActorRole("SYSTEM");
        }
        event.setContent(content);
        eventRepository.save(event);
    }

    private BigDecimal bd(int v) {
        return new BigDecimal(v);
    }
}
