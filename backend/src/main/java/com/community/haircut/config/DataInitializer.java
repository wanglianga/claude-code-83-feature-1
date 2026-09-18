package com.community.haircut.config;

import com.community.haircut.entity.*;
import com.community.haircut.enums.*;
import com.community.haircut.repository.*;
import com.community.haircut.service.CareTaskService;
import com.community.haircut.service.OrderService;
import com.community.haircut.service.ToolSafetyService;
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
    private final ToolIssueRecordRepository toolIssueRepository;
    private final InfectionTraceRepository infectionTraceRepository;
    private final InfectionContactRepository infectionContactRepository;
    private final PasswordEncoder passwordEncoder;
    private final CareTaskService careTaskService;
    private final ToolSafetyService toolSafetyService;

    public DataInitializer(UserRepository userRepository, BarberProfileRepository barberProfileRepository,
                           VolunteerProfileRepository volunteerProfileRepository, ToolKitRepository toolKitRepository,
                           BarberScheduleRepository scheduleRepository, ElderRepository elderRepository,
                           ServiceOrderRepository orderRepository, OrderEventRepository eventRepository,
                           ToolConfirmationRepository toolConfirmationRepository,
                           VisitRecordRepository visitRecordRepository, ServiceRecordRepository serviceRecordRepository,
                           SubsidyRecordRepository subsidyRepository, ExceptionRecordRepository exceptionRepository,
                           FollowUpRepository followUpRepository, CommunityEventRepository event2Repository,
                           ToolIssueRecordRepository toolIssueRepository,
                           InfectionTraceRepository infectionTraceRepository,
                           InfectionContactRepository infectionContactRepository,
                           PasswordEncoder passwordEncoder, CareTaskService careTaskService,
                           ToolSafetyService toolSafetyService) {
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
        this.toolIssueRepository = toolIssueRepository;
        this.infectionTraceRepository = infectionTraceRepository;
        this.infectionContactRepository = infectionContactRepository;
        this.passwordEncoder = passwordEncoder;
        this.careTaskService = careTaskService;
        this.toolSafetyService = toolSafetyService;
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

        ToolKit kitA = toolKit(barber1, "标准理发工具包A", "剪刀,推子,梳子,围布,消毒喷雾,服务包",
                DisinfectionStatus.DISINFECTED, LocalDateTime.now().minusHours(2),
                KitType.NORMAL, "FB-20260918-001", "紫外线消毒柜（30分钟）", "UV-A01", "李理发", false);
        ToolKit kitB = toolKit(barber2, "标准理发工具包B", "剪刀,推子,梳子,围布,消毒喷雾,服务包",
                DisinfectionStatus.DISINFECTED, LocalDateTime.now().minusHours(20),
                KitType.NORMAL, "FB-20260917-014", "高温蒸煮（15分钟）", "XHG-B02", "赵理发", false);
        ToolKit kitC = toolKit(barber3, "标准理发工具包C", "剪刀,推子,梳子,围布",
                DisinfectionStatus.PENDING, LocalDateTime.now().minusDays(4),
                KitType.NORMAL, "FB-20260913-008", "紫外线消毒柜（30分钟）", "UV-A03", "孙理发", false);
        kitC.setSealIntact(false);
        toolKitRepository.save(kitC);
        // 社区备用服务包：一个消毒合格可应急；一个已使用待补录（未补录前不能再派单）
        ToolKit backupReady = toolKit(null, "社区备用服务包①", "一次性围布,一次性剃刀,消毒棉片,服务包",
                DisinfectionStatus.DISINFECTED, LocalDateTime.now().minusHours(6),
                KitType.BACKUP, "BB-20260918-001", "含氯消毒剂浸泡+紫外线", "UV-C01", "王社区", false);
        ToolKit backupUsed = toolKit(null, "社区备用服务包②", "一次性围布,一次性剃刀,消毒棉片,服务包",
                DisinfectionStatus.PENDING, LocalDateTime.now().minusDays(2),
                KitType.BACKUP, "BB-20260916-002", "含氯消毒剂浸泡+紫外线", "UV-C01", "王社区", true);
        backupUsed.setNotes("昨日应急使用后回收，待补录消毒记录");
        toolKitRepository.save(backupUsed);

        // ---------- 排班：今天起 14 天 ----------
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
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

        // ---------- 工具消毒失效与交叉感染追溯演示数据 ----------
        // 1) 感染风险服务：褚寿山头癣，工具单独分装并记录用后处理
        ServiceOrder riskOrder = seedOrder("HF" + today.minusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-R01",
                e12, barber1, volunteer1, OrderType.NORMAL, today.minusDays(2), "09:00-11:00",
                bd(30), bd(30), bd(0), PaymentStatus.WAIVED, 5, "一次性用品，服务很安心");
        riskOrder.setInfectionRisk(true);
        riskOrder.setInfectionNote("头癣：工具单独分装，用后含氯消毒");
        orderRepository.save(riskOrder);
        completeSeed(riskOrder, barber1, volunteer1, true);
        toolConfirmationRepository.findByOrderId(riskOrder.getId()).ifPresent(tc -> {
            tc.setInfectionRisk(InfectionRiskType.TINEA);
            tc.setInfectionNote("老人有头癣");
            tc.setSeparatelyPacked(true);
            tc.setDisposableUsed(true);
            tc.setPostHandling("工具单独分装带回，含氯消毒剂浸泡 30 分钟后高温蒸煮，一次性用品按医废封装");
            tc.setPostHandlingRecorded(true);
            toolConfirmationRepository.save(tc);
        });
        seedFollowUp(e12, riskOrder, FollowUpType.EXCEPTION, "感染风险服务回访：服务后 3-7 天确认是否出现皮肤瘙痒、红疹");

        // 2) 工具问题：孙理发消毒过期/封签破损导致取消（理发师担责）与改期（待社区认定补偿）
        ServiceOrder issueCancel = seedOrder("HF" + today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-T01",
                e7, barber3, null, OrderType.NORMAL, today, "14:00-16:00",
                bd(30), bd(0), bd(30), PaymentStatus.UNPAID, null, null);
        issueCancel.setStatus(OrderStatus.CANCELLED);
        issueCancel.setToolIssueCaused(true);
        issueCancel.setCancelReason("工具问题取消：消毒超过有效期、封签破损");
        orderRepository.save(issueCancel);
        seedEvent(issueCancel, "CANCEL", staff, "取消服务单，原因：工具问题取消：消毒超过有效期、封签破损（因工具消毒/封签等问题取消，不算老人违约，不扣补贴）");
        seedToolIssue(issueCancel, kitC.getId(), ToolIssueType.DISINFECTION_EXPIRED,
                "消毒时间已超过 48 小时且封签破损", ToolIssueAction.CANCEL,
                CompensationStatus.BARBER_BEAR, 0, "工具遗漏/消毒失责，理发师空跑自行承担", staff);

        ServiceOrder issueReschedule = seedOrder("HF" + today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-T02",
                e8, barber2, null, OrderType.NORMAL, today.plusDays(2), "09:00-11:00",
                bd(30), bd(20), bd(10), PaymentStatus.UNPAID, null, null);
        issueReschedule.setToolIssueCaused(true);
        orderRepository.save(issueReschedule);
        seedEvent(issueReschedule, "RESCHEDULE", staff,
                "原今日预约因服务包封签破损改期（因工具问题改期，不算老人违约，不扣补贴），理发师调整为「赵理发」，上门前需重新扫码核验");
        seedToolIssue(issueReschedule, kitB.getId(), ToolIssueType.SEAL_BROKEN,
                "上门扫码发现封签编号 FB-20260917-014 封签破损", ToolIssueAction.RESCHEDULE,
                CompensationStatus.PENDING, 0, "等待社区按规则认定空跑补偿", staff);

        // 3) 交叉感染追溯：钱淑华反馈红疹，反查同服务包/同日老人
        ServiceOrder infSource = seedOrder("HF" + yesterday.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-I00",
                e9, barber2, volunteer1, OrderType.NORMAL, yesterday, "09:00-11:00",
                bd(30), bd(30), bd(0), PaymentStatus.WAIVED, null, null);
        completeSeed(infSource, barber2, volunteer1, true);
        ServiceOrder infC1 = seedOrder("HF" + yesterday.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-I01",
                e8, barber2, volunteer2, OrderType.NORMAL, yesterday, "14:00-16:00",
                bd(30), bd(20), bd(10), PaymentStatus.PAID, null, null);
        completeSeed(infC1, barber2, volunteer2, false);
        ServiceOrder infC2 = seedOrder("HF" + yesterday.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-I02",
                e11, barber2, null, OrderType.NORMAL, yesterday, "16:00-18:00",
                bd(30), bd(20), bd(10), PaymentStatus.PAID, null, null);
        completeSeed(infC2, barber2, null, false);
        seedInfectionTrace(infSource, barber2, kitB, infC1, infC2, e8, e11, staff);

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

    private ToolKit toolKit(User barber, String name, String items, DisinfectionStatus status,
                            LocalDateTime disinfectedAt, KitType kitType, String sealNo,
                            String method, String cabinetNo, String responsiblePerson, boolean pending) {
        ToolKit kit = new ToolKit();
        kit.setBarberId(barber == null ? null : barber.getId());
        kit.setName(name);
        kit.setItems(items);
        kit.setStatus(status);
        kit.setDisinfectedAt(disinfectedAt);
        kit.setKitType(kitType);
        kit.setSealNo(sealNo);
        kit.setDisinfectionMethod(method);
        kit.setCabinetNo(cabinetNo);
        kit.setResponsiblePerson(responsiblePerson);
        kit.setDisinfectionPending(pending);
        return toolKitRepository.save(kit);
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

    /** 把种子单推进到已完成，并补齐扫码核验/陪同/服务档案/补贴记录 */
    private void completeSeed(ServiceOrder order, User barber, User volunteer, boolean withSubsidy) {
        LocalDate date = order.getScheduledDate();
        ToolKit kit = toolKitRepository.findByBarberId(barber.getId()).orElse(null);
        ToolConfirmation tc = new ToolConfirmation();
        tc.setOrderId(order.getId());
        tc.setBarberId(barber.getId());
        if (kit != null) {
            tc.setKitId(kit.getId());
            tc.setSealNo(kit.getSealNo());
            tc.setDisinfectedAt(kit.getDisinfectedAt());
            tc.setDisinfectionMethod(kit.getDisinfectionMethod());
            tc.setCabinetNo(kit.getCabinetNo());
            tc.setResponsiblePerson(kit.getResponsiblePerson());
            order.setUsedKitId(kit.getId());
            order.setUsedSealNo(kit.getSealNo());
            orderRepository.save(order);
        }
        tc.setSealIntact(true);
        tc.setDisinfectionValid(true);
        tc.setToolsOk(true);
        tc.setCapeOk(true);
        tc.setDisinfectantOk(true);
        tc.setPackOk(true);
        tc.setTowelDry(true);
        tc.setToolClean(true);
        tc.setInfectionRisk(InfectionRiskType.NONE);
        tc.setPassed(true);
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

    private void seedToolIssue(ServiceOrder order, Long kitId, ToolIssueType type, String detail,
                               ToolIssueAction action, CompensationStatus comp, int amount,
                               String compNote, User handler) {
        ToolIssueRecord r = new ToolIssueRecord();
        r.setOrderId(order.getId());
        r.setBarberId(order.getBarberId());
        r.setKitId(kitId);
        r.setIssueType(type);
        r.setIssueDetail(detail);
        r.setAction(action);
        r.setCompensationStatus(comp);
        r.setCompensationAmount(bd(amount));
        r.setCompensationNote(compNote);
        r.setHandledById(handler.getId());
        r.setHandledByName(handler.getRealName());
        toolIssueRepository.save(r);
    }

    private void seedInfectionTrace(ServiceOrder source, User barber, ToolKit kit,
                                    ServiceOrder c1, ServiceOrder c2, Elder e1, Elder e2, User staff) {
        InfectionTrace trace = new InfectionTrace();
        trace.setSourceOrderId(source.getId());
        trace.setSourceElderName(source.getElderName());
        trace.setBarberId(barber.getId());
        trace.setBarberName(barber.getRealName());
        trace.setKitId(kit.getId());
        trace.setSealNo(kit.getSealNo());
        trace.setSymptom("头皮红疹、瘙痒");
        trace.setDescription("家属反馈老人服务后次日头皮出现红疹并瘙痒，已就医，怀疑与工具消毒不彻底有关");
        trace.setReporterName("家属");
        trace.setStatus(InfectionStatus.TRACING);
        trace.setNotified(false);
        infectionTraceRepository.save(trace);

        seedEvent(source, "EXCEPTION", staff, "登记交叉感染追溯：反馈症状【头皮红疹、瘙痒】；社区将按理发师、服务包/工具编号与同日服务记录反查");
        seedEvent(source, "EXCEPTION", null, "反查出同工具/同日接受服务的其他老人 2 人，待通知家属与志愿者");

        infectionContact(trace, c1, e1, c1.getVolunteerId(), c1.getVolunteerName());
        infectionContact(trace, c2, e2, null, null);
    }

    private void infectionContact(InfectionTrace trace, ServiceOrder order, Elder elder,
                                  Long volunteerId, String volunteerName) {
        InfectionContact c = new InfectionContact();
        c.setTraceId(trace.getId());
        c.setOrderId(order.getId());
        c.setElderId(elder.getId());
        c.setElderName(elder.getName());
        c.setFamilyUserId(elder.getFamilyUserId());
        c.setFamilyContactName(elder.getFamilyContactName());
        c.setVolunteerId(volunteerId);
        c.setVolunteerName(volunteerName);
        c.setServedAt(order.getScheduledDate() == null ? null : order.getScheduledDate().atTime(10, 0));
        c.setKitId(order.getUsedKitId());
        infectionContactRepository.save(c);
    }

    private BigDecimal bd(int v) {
        return new BigDecimal(v);
    }
}
