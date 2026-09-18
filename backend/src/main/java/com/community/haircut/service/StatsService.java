package com.community.haircut.service;

import com.community.haircut.entity.*;
import com.community.haircut.enums.*;
import com.community.haircut.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计复盘：按老人风险、理发师、楼栋、志愿者、公益资金查看服务质量与覆盖。
 */
@Service
public class StatsService {

    private final ElderRepository elderRepository;
    private final ServiceOrderRepository orderRepository;
    private final BarberProfileRepository barberProfileRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final UserRepository userRepository;
    private final SubsidyRecordRepository subsidyRepository;
    private final ExceptionRecordRepository exceptionRepository;
    private final CareTaskRepository careTaskRepository;
    private final FollowUpRepository followUpRepository;
    private final ToolIssueRecordRepository toolIssueRepository;
    private final InfectionTraceRepository infectionTraceRepository;

    public StatsService(ElderRepository elderRepository, ServiceOrderRepository orderRepository,
                        BarberProfileRepository barberProfileRepository,
                        VolunteerProfileRepository volunteerProfileRepository, UserRepository userRepository,
                        SubsidyRecordRepository subsidyRepository, ExceptionRecordRepository exceptionRepository,
                        CareTaskRepository careTaskRepository, FollowUpRepository followUpRepository,
                        ToolIssueRecordRepository toolIssueRepository,
                        InfectionTraceRepository infectionTraceRepository) {
        this.elderRepository = elderRepository;
        this.orderRepository = orderRepository;
        this.barberProfileRepository = barberProfileRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.userRepository = userRepository;
        this.subsidyRepository = subsidyRepository;
        this.exceptionRepository = exceptionRepository;
        this.careTaskRepository = careTaskRepository;
        this.followUpRepository = followUpRepository;
        this.toolIssueRepository = toolIssueRepository;
        this.infectionTraceRepository = infectionTraceRepository;
    }

    public Map<String, Object> dashboard() {
        List<ServiceOrder> orders = orderRepository.findAll();
        List<Elder> elders = elderRepository.findAll();
        Map<String, Object> map = new HashMap<>();
        map.put("elderCount", elders.stream().filter(e -> "ACTIVE".equals(e.getStatus())).count());
        map.put("orderTotal", orders.size());
        map.put("orderCompleted", orders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count());
        map.put("orderToday", orders.stream().filter(o -> o.getScheduledDate().equals(LocalDate.now())
                && o.getStatus() != OrderStatus.CANCELLED).count());
        map.put("orderCancelled", orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count());
        map.put("openExceptions", exceptionRepository.findByStatusOrderByCreatedAtDesc(ExceptionStatus.OPEN).size()
                + exceptionRepository.findByStatusOrderByCreatedAtDesc(ExceptionStatus.PROCESSING).size());
        map.put("pendingCareTasks", careTaskRepository.findByStatusOrderByCreatedAtDesc(CareTaskStatus.PENDING).size());
        map.put("pendingFollowUps", followUpRepository.findByStatusOrderByCreatedAtDesc(FollowUpStatus.PENDING).size());
        BigDecimal subsidyTotal = subsidyRepository.findAll().stream()
                .filter(s -> s.getStatus() != SubsidyStatus.REJECTED)
                .map(SubsidyRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        map.put("subsidyTotal", subsidyTotal);
        map.put("pendingSubsidy", subsidyRepository.findByStatusOrderByCreatedAtDesc(SubsidyStatus.PENDING).size());
        double avgRating = orders.stream().filter(o -> o.getSatisfactionRating() != null)
                .mapToInt(ServiceOrder::getSatisfactionRating).average().orElse(0);
        map.put("avgSatisfaction", Math.round(avgRating * 100.0) / 100.0);
        return map;
    }

    /** 服务覆盖：按楼栋 / 按风险等级 / 按志愿者 */
    public Map<String, Object> coverage() {
        List<Elder> elders = elderRepository.findByStatus("ACTIVE");
        List<ServiceOrder> orders = orderRepository.findAll();
        Set<Long> servedElderIds = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(ServiceOrder::getElderId).collect(Collectors.toSet());

        // 按楼栋
        Map<String, long[]> byBuilding = new TreeMap<>();
        for (Elder e : elders) {
            String building = e.getBuilding() == null ? "未填写" : e.getBuilding();
            long[] arr = byBuilding.computeIfAbsent(building, k -> new long[2]);
            arr[0]++;
            if (servedElderIds.contains(e.getId())) {
                arr[1]++;
            }
        }
        List<Map<String, Object>> buildingStats = byBuilding.entrySet().stream().map(en -> {
            Map<String, Object> m = new HashMap<>();
            m.put("building", en.getKey());
            m.put("elderCount", en.getValue()[0]);
            m.put("servedCount", en.getValue()[1]);
            return m;
        }).toList();

        // 按风险等级
        List<Map<String, Object>> riskStats = new ArrayList<>();
        for (RiskLevel level : RiskLevel.values()) {
            List<Elder> group = elders.stream().filter(e -> e.getRiskLevel() == level).toList();
            Map<String, Object> m = new HashMap<>();
            m.put("riskLevel", level.name());
            m.put("elderCount", group.size());
            m.put("servedCount", group.stream().filter(e -> servedElderIds.contains(e.getId())).count());
            m.put("orderCount", orders.stream().filter(o -> o.getRiskLevel() == level
                    && o.getStatus() == OrderStatus.COMPLETED).count());
            riskStats.add(m);
        }

        // 按志愿者
        List<Map<String, Object>> volunteerStats = new ArrayList<>();
        for (VolunteerProfile v : volunteerProfileRepository.findAll()) {
            Map<String, Object> m = new HashMap<>();
            userRepository.findById(v.getUserId()).ifPresent(u -> m.put("name", u.getRealName()));
            m.put("serviceCount", v.getServiceCount());
            m.put("orderCount", orders.stream().filter(o -> v.getUserId().equals(o.getVolunteerId())
                    && o.getStatus() == OrderStatus.COMPLETED).count());
            volunteerStats.add(m);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("byBuilding", buildingStats);
        map.put("byRisk", riskStats);
        map.put("byVolunteer", volunteerStats);
        return map;
    }

    /** 理发师服务质量与信用 */
    public List<Map<String, Object>> barberQuality() {
        List<ServiceOrder> orders = orderRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (BarberProfile p : barberProfileRepository.findAll()) {
            Map<String, Object> m = new HashMap<>();
            userRepository.findById(p.getUserId()).ifPresent(u -> m.put("name", u.getRealName()));
            m.put("profile", p);
            List<ServiceOrder> mine = orders.stream().filter(o -> p.getUserId().equals(o.getBarberId())).toList();
            m.put("completed", mine.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count());
            m.put("cancelled", mine.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count());
            double avg = mine.stream().filter(o -> o.getSatisfactionRating() != null)
                    .mapToInt(ServiceOrder::getSatisfactionRating).average().orElse(0);
            m.put("avgSatisfaction", Math.round(avg * 100.0) / 100.0);
            result.add(m);
        }
        return result;
    }

    /** 公益资金复盘：按月统计补贴 */
    public Map<String, Object> funds() {
        List<SubsidyRecord> records = subsidyRepository.findAll();
        Map<String, BigDecimal> byMonth = new TreeMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (SubsidyRecord r : records) {
            if (r.getStatus() == SubsidyStatus.REJECTED) {
                continue;
            }
            String month = r.getCreatedAt().format(fmt);
            byMonth.merge(month, r.getAmount(), BigDecimal::add);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("byMonth", byMonth);
        map.put("approvedTotal", records.stream().filter(r -> r.getStatus() == SubsidyStatus.APPROVED)
                .map(SubsidyRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        map.put("pendingTotal", records.stream().filter(r -> r.getStatus() == SubsidyStatus.PENDING)
                .map(SubsidyRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        map.put("pendingCount", records.stream().filter(r -> r.getStatus() == SubsidyStatus.PENDING).count());
        map.put("approvedCount", records.stream().filter(r -> r.getStatus() == SubsidyStatus.APPROVED).count());

        // 工具问题空跑补偿（公益资金复盘）
        List<ToolIssueRecord> issues = toolIssueRepository.findAllByOrderByHandledAtDesc();
        BigDecimal compTotal = issues.stream()
                .filter(i -> i.getCompensationStatus() == com.community.haircut.enums.CompensationStatus.COMMUNITY
                        || i.getCompensationStatus() == com.community.haircut.enums.CompensationStatus.PAID)
                .map(ToolIssueRecord::getCompensationAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        map.put("emptyRunCompTotal", compTotal);
        map.put("compPendingCount", issues.stream()
                .filter(i -> i.getCompensationStatus() == com.community.haircut.enums.CompensationStatus.PENDING).count());
        map.put("barberBearCount", issues.stream()
                .filter(i -> i.getCompensationStatus() == com.community.haircut.enums.CompensationStatus.BARBER_BEAR).count());

        // 交叉感染/消毒责任复盘
        List<InfectionTrace> traces = infectionTraceRepository.findAllByOrderByCreatedAtDesc();
        map.put("infectionTotal", traces.size());
        map.put("infectionOpenCount", traces.stream()
                .filter(t -> t.getStatus() != com.community.haircut.enums.InfectionStatus.RESOLVED).count());
        map.put("infectionConfirmedCount", traces.stream()
                .filter(InfectionTrace::getCrossInfectionConfirmed).count());
        map.put("suspendedBarberCount", barberProfileRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getVisitSuspended())).count());
        return map;
    }

    /** 预警名单：长期未预约 / 连续取消 */
    public Map<String, Object> alerts() {
        List<Elder> elders = elderRepository.findByStatus("ACTIVE");
        List<ServiceOrder> orders = orderRepository.findAll();
        LocalDate threshold = LocalDate.now().minusDays(CareTaskService.NO_APPOINTMENT_DAYS);

        List<Map<String, Object>> noAppointment = new ArrayList<>();
        List<Map<String, Object>> repeatedCancel = new ArrayList<>();
        for (Elder e : elders) {
            List<ServiceOrder> mine = orders.stream()
                    .filter(o -> o.getElderId().equals(e.getId()))
                    .sorted(Comparator.comparing(ServiceOrder::getCreatedAt).reversed())
                    .toList();
            boolean hasRecent = mine.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .anyMatch(o -> o.getCreatedAt().toLocalDate().isAfter(threshold));
            if (!hasRecent) {
                Map<String, Object> m = new HashMap<>();
                m.put("elder", e);
                m.put("lastOrderAt", mine.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                        .map(o -> o.getCreatedAt().toLocalDate()).findFirst().orElse(null));
                noAppointment.add(m);
            }
            int cancels = 0;
            for (ServiceOrder o : mine) {
                if (o.getStatus() == OrderStatus.CANCELLED && !Boolean.TRUE.equals(o.getToolIssueCaused())) {
                    cancels++;
                } else if (o.getStatus() == OrderStatus.CANCELLED) {
                    continue;
                } else {
                    break;
                }
            }
            if (cancels >= CareTaskService.CANCEL_THRESHOLD) {
                Map<String, Object> m = new HashMap<>();
                m.put("elder", e);
                m.put("cancelCount", cancels);
                repeatedCancel.add(m);
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("noAppointment", noAppointment);
        map.put("repeatedCancel", repeatedCancel);
        return map;
    }
}
