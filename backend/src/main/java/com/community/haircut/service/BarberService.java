package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.*;
import com.community.haircut.enums.*;
import com.community.haircut.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BarberService {

    private final BarberProfileRepository barberProfileRepository;
    private final BarberScheduleRepository scheduleRepository;
    private final ToolKitRepository toolKitRepository;
    private final DisinfectionRecordRepository disinfectionRecordRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;

    public BarberService(BarberProfileRepository barberProfileRepository,
                         BarberScheduleRepository scheduleRepository, ToolKitRepository toolKitRepository,
                         DisinfectionRecordRepository disinfectionRecordRepository,
                         UserRepository userRepository, OrderService orderService,
                         NotificationService notificationService) {
        this.barberProfileRepository = barberProfileRepository;
        this.scheduleRepository = scheduleRepository;
        this.toolKitRepository = toolKitRepository;
        this.disinfectionRecordRepository = disinfectionRecordRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.notificationService = notificationService;
    }

    /** 理发师列表（含信用、主工具包消毒状态、备用包数量） */
    public List<Map<String, Object>> listBarbers() {
        return barberProfileRepository.findAll().stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("profile", p);
            userRepository.findById(p.getUserId()).ifPresent(u -> {
                map.put("realName", u.getRealName());
                map.put("phone", u.getPhone());
                map.put("status", u.getStatus());
            });
            toolKitRepository.findFirstByBarberIdAndKitType(p.getUserId(), KitType.MAIN).ifPresent(kit -> {
                map.put("toolKit", kit);
                map.put("effectiveDisinfection", orderService.effectiveDisinfectionStatus(kit));
            });
            map.put("spareKitCount", toolKitRepository.findByBarberIdAndKitType(p.getUserId(), KitType.SPARE).size());
            return map;
        }).toList();
    }

    @Transactional
    public BarberProfile updateProfile(Long userId, String skillTags, String serviceBuildings, Boolean active) {
        BarberProfile profile = barberProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException("理发师档案不存在"));
        if (skillTags != null) {
            profile.setSkillTags(skillTags);
        }
        if (serviceBuildings != null) {
            profile.setServiceBuildings(serviceBuildings);
        }
        if (active != null) {
            profile.setActive(active);
        }
        return barberProfileRepository.save(profile);
    }

    @Transactional
    public BarberProfile adjustCredit(Long userId, int delta, String reason) {
        BarberProfile profile = requireProfile(userId);
        profile.setCreditScore(Math.max(0, Math.min(100, profile.getCreditScore() + delta)));
        return barberProfileRepository.save(profile);
    }

    // ---------------- 排班 ----------------

    public List<BarberSchedule> listSchedules(Long barberId, LocalDate from, LocalDate to) {
        if (barberId != null) {
            return scheduleRepository.findByBarberIdAndWorkDateBetween(barberId, from, to);
        }
        return scheduleRepository.findByWorkDateBetween(from, to);
    }

    @Transactional
    public BarberSchedule addSchedule(Long barberId, LocalDate workDate, String startTime, String endTime, Integer maxOrders) {
        BarberProfile profile = barberProfileRepository.findByUserId(barberId)
                .orElseThrow(() -> new BizException("理发师档案不存在"));
        if (Boolean.TRUE.equals(profile.getSuspended())) {
            throw new BizException("该理发师上门资格已暂停（待消毒培训与工具复检），暂不可排班");
        }
        BarberSchedule schedule = new BarberSchedule();
        schedule.setBarberId(barberId);
        schedule.setWorkDate(workDate);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setMaxOrders(maxOrders == null ? 4 : maxOrders);
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void deleteSchedule(Long id, Long currentBarberId, boolean isStaff) {
        BarberSchedule schedule = scheduleRepository.findById(id).orElseThrow(() -> new BizException("排班不存在"));
        if (!isStaff && !schedule.getBarberId().equals(currentBarberId)) {
            throw new BizException(403, "只能删除自己的排班");
        }
        scheduleRepository.delete(schedule);
    }

    // ---------------- 工具包 / 服务包 ----------------

    /** 理发师全部工具包（主包 + 备用服务包），附带实时核验结论 */
    public List<Map<String, Object>> listKits(Long barberId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ToolKit kit : toolKitRepository.findAll().stream()
                .filter(k -> k.getBarberId().equals(barberId)).toList()) {
            Map<String, Object> m = new HashMap<>();
            m.put("kit", kit);
            m.put("effectiveStatus", orderService.effectiveDisinfectionStatus(kit));
            m.put("checks", orderService.kitCheck(kit));
            result.add(m);
        }
        return result;
    }

    /** 主工具包（兼容旧页面） */
    public ToolKit getToolKit(Long barberId) {
        return toolKitRepository.findFirstByBarberIdAndKitType(barberId, KitType.MAIN).orElse(null);
    }

    /** 扫码核验：按封签编号定位服务包并返回实时核验结论 */
    public Map<String, Object> scanBySeal(Long barberId, String sealCode) {
        ToolKit kit = toolKitRepository.findBySealCode(sealCode.trim())
                .orElseThrow(() -> new BizException("未查询到封签编号 " + sealCode + " 对应的服务包，请人工核对"));
        if (!kit.getBarberId().equals(barberId)) {
            throw new BizException("该服务包不属于当前理发师，禁止使用");
        }
        Map<String, Object> m = new HashMap<>();
        m.put("kit", kit);
        m.put("effectiveStatus", orderService.effectiveDisinfectionStatus(kit));
        m.put("checks", orderService.kitCheck(kit));
        return m;
    }

    /** 当前可直接启用的备用服务包：封签完好、消毒有效、使用后已补录 */
    public List<ToolKit> usableSpareKits(Long barberId) {
        return toolKitRepository.findByBarberIdAndKitType(barberId, KitType.SPARE).stream()
                .filter(k -> orderService.kitCheck(k).isEmpty()).toList();
    }

    /** 兼容旧接口：保存主工具包档案 */
    @Transactional
    public ToolKit saveToolKit(Long barberId, String name, String items, String notes) {
        return saveMainToolKit(barberId, name, items, notes);
    }

    @Transactional
    public ToolKit saveMainToolKit(Long barberId, String name, String items, String notes) {
        ToolKit kit = toolKitRepository.findFirstByBarberIdAndKitType(barberId, KitType.MAIN)
                .orElseGet(() -> {
                    ToolKit k = new ToolKit();
                    k.setBarberId(barberId);
                    k.setKitType(KitType.MAIN);
                    return k;
                });
        if (name != null) {
            kit.setName(name);
        }
        if (items != null) {
            kit.setItems(items);
        }
        kit.setNotes(notes);
        return toolKitRepository.save(kit);
    }

    /** 新建上门备用服务包（空白包，需完成消毒并加封签后才可启用） */
    @Transactional
    public ToolKit createSpareKit(Long barberId, String name, String items) {
        ToolKit kit = new ToolKit();
        kit.setBarberId(barberId);
        kit.setKitType(KitType.SPARE);
        kit.setName(name == null || name.isBlank() ? "上门备用服务包" : name);
        kit.setItems(items);
        kit.setStatus(DisinfectionStatus.PENDING);
        return toolKitRepository.save(kit);
    }

    public List<DisinfectionRecord> listDisinfections(Long barberId, Long kitId) {
        if (kitId != null) {
            return disinfectionRecordRepository.findByKitIdOrderByDisinfectedAtDesc(kitId);
        }
        if (barberId != null) {
            return disinfectionRecordRepository.findByBarberIdOrderByDisinfectedAtDesc(barberId);
        }
        return disinfectionRecordRepository.findAllByOrderByDisinfectedAtDesc();
    }

    /**
     * 消毒登记 / 备用包使用后补录：记录消毒日期、方式、消毒柜编号、责任人、新封签编号，
     * 同步更新工具包状态；备用包补录后解除"已使用未补录"锁定，方可再次派单。
     */
    @Transactional
    public DisinfectionRecord recordDisinfection(Long kitId, DisinfectionMethod method, String cabinetNo,
                                                 String responsiblePerson, String sealCode, Integer validHours,
                                                 boolean supplementary, Long orderId, String note,
                                                 OrderService.LoginUserInfo operator) {
        ToolKit kit = toolKitRepository.findById(kitId).orElseThrow(() -> new BizException("工具包不存在"));
        if (operator.role() != Role.ADMIN && operator.role() != Role.STAFF
                && !kit.getBarberId().equals(operator.userId())) {
            throw new BizException(403, "无权限登记该工具包消毒记录");
        }
        if (method == null) {
            throw new BizException("请选择消毒方式");
        }
        if (cabinetNo == null || cabinetNo.isBlank()) {
            throw new BizException("请填写消毒柜编号");
        }
        if (responsiblePerson == null || responsiblePerson.isBlank()) {
            throw new BizException("请填写消毒责任人");
        }
        LocalDateTime now = LocalDateTime.now();
        DisinfectionRecord record = new DisinfectionRecord();
        record.setKitId(kitId);
        record.setBarberId(kit.getBarberId());
        record.setDisinfectedAt(now);
        record.setMethod(method);
        record.setCabinetNo(cabinetNo);
        record.setResponsiblePerson(responsiblePerson);
        record.setSealCode(sealCode);
        record.setValidHours(validHours == null || validHours <= 0 ? 48 : validHours);
        record.setSupplementary(supplementary);
        record.setOrderId(orderId);
        record.setOperatorId(operator.userId());
        record.setOperatorName(operator.realName());
        record.setNote(note);
        disinfectionRecordRepository.save(record);

        kit.setStatus(DisinfectionStatus.DISINFECTED);
        kit.setDisinfectedAt(now);
        kit.setDisinfectionMethod(method);
        kit.setCabinetNo(cabinetNo);
        kit.setResponsiblePerson(responsiblePerson);
        kit.setValidHours(record.getValidHours());
        if (sealCode != null && !sealCode.isBlank()) {
            kit.setSealCode(sealCode.trim());
            kit.setSealStatus(SealStatus.INTACT);
        }
        // 补录完成：解除备用包"使用后未补录"锁定
        if (kit.getLastUsedOrderId() != null) {
            kit.setLastUsedOrderId(null);
            kit.setLastUsedAt(null);
        }
        toolKitRepository.save(kit);

        String text = (supplementary ? "补录" : "") + "消毒登记：方式=" + methodName(method)
                + "，消毒柜 " + cabinetNo + "，责任人 " + responsiblePerson
                + (sealCode != null && !sealCode.isBlank() ? "，新封签 " + sealCode.trim() : "")
                + "，有效期 " + record.getValidHours() + " 小时";
        if (orderId != null) {
            orderService.addEvent(orderId, "TOOL_CONFIRM", operator.userId(), operator.realName(), operator.role(),
                    (kit.getKitType() == KitType.SPARE ? "备用服务包" : "工具包") + text
                            + (supplementary ? "；已补录，可再次派单" : ""));
        }
        if (kit.getKitType() == KitType.SPARE && supplementary) {
            notificationService.notifyRole(Role.STAFF, "备用服务包消毒已补录",
                    "备用包「" + kit.getName() + "」已" + text + "，恢复可用", "TOOL");
        }
        return record;
    }

    /** 兼容旧入口：一键完成主包消毒（默认紫外消毒、48 小时有效） */
    @Transactional
    public ToolKit disinfect(Long barberId) {
        ToolKit kit = toolKitRepository.findFirstByBarberIdAndKitType(barberId, KitType.MAIN)
                .orElseThrow(() -> new BizException("请先建立工具包档案"));
        User u = userRepository.findById(barberId).orElse(null);
        recordDisinfection(kit.getId(), DisinfectionMethod.UV, "XDG-01",
                u == null ? "理发师本人" : u.getRealName(),
                kit.getSealCode() == null ? "SEAL-NEW-" + kit.getId() : kit.getSealCode(),
                48, false, null, "工作台快捷消毒",
                new OrderService.LoginUserInfo(barberId, u == null ? "理发师" : u.getRealName(), Role.BARBER));
        return toolKitRepository.findById(kit.getId()).orElseThrow();
    }

    // ---------------- 培训 / 复检 / 资格 ----------------

    /** 社区确认消毒培训完成 */
    @Transactional
    public BarberProfile completeTraining(Long userId, String note, OrderService.LoginUserInfo operator) {
        BarberProfile p = requireProfile(userId);
        p.setDisinfectionTrained(true);
        p.setTrainedAt(LocalDateTime.now());
        barberProfileRepository.save(p);
        notificationService.notify(userId, "消毒培训已登记",
                "社区已登记你的工具消毒培训完成记录" + (note == null || note.isBlank() ? "" : "：" + note), "TOOL");
        return p;
    }

    /** 社区安排工具复检并登记结果 */
    @Transactional
    public BarberProfile recheckTools(Long userId, boolean passed, String note, OrderService.LoginUserInfo operator) {
        BarberProfile p = requireProfile(userId);
        p.setToolRechecked(passed);
        if (passed) {
            p.setRecheckedAt(LocalDateTime.now());
        }
        barberProfileRepository.save(p);
        notificationService.notify(userId, passed ? "工具复检通过" : "工具复检未通过",
                passed ? "你的工具复检已通过，可联系社区恢复上门资格"
                        : "你的工具复检未通过，请按要求整改后再次复检" + (note == null || note.isBlank() ? "" : "：" + note),
                "TOOL");
        return p;
    }

    /**
     * 恢复上门资格：须消毒培训完成且工具复检通过；派单权重维持下调后的水平。
     */
    @Transactional
    public BarberProfile reinstate(Long userId, OrderService.LoginUserInfo operator) {
        BarberProfile p = requireProfile(userId);
        if (!Boolean.TRUE.equals(p.getDisinfectionTrained())) {
            throw new BizException("消毒培训尚未完成，不能恢复上门资格");
        }
        if (!Boolean.TRUE.equals(p.getToolRechecked())) {
            throw new BizException("工具复检尚未通过，不能恢复上门资格");
        }
        p.setSuspended(false);
        p.setSuspendReason(null);
        p.setSuspendedAt(null);
        barberProfileRepository.save(p);
        notificationService.notify(userId, "上门资格已恢复",
                "消毒培训与工具复检均已完成，社区已恢复你的上门派单（派单权重已下调）", "TOOL");
        return p;
    }

    /** 登记服务包封签破损（须重新消毒加封后才能使用/派单） */
    @Transactional
    public ToolKit markSealBroken(Long kitId, OrderService.LoginUserInfo operator) {
        ToolKit kit = toolKitRepository.findById(kitId).orElseThrow(() -> new BizException("工具包不存在"));
        if (operator.role() != Role.ADMIN && operator.role() != Role.STAFF
                && !kit.getBarberId().equals(operator.userId())) {
            throw new BizException(403, "无权限操作该工具包");
        }
        kit.setSealStatus(SealStatus.BROKEN);
        kit.setStatus(DisinfectionStatus.PENDING);
        return toolKitRepository.save(kit);
    }

    private BarberProfile requireProfile(Long userId) {
        return barberProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException("理发师档案不存在"));
    }

    public static String methodName(DisinfectionMethod m) {
        return switch (m) {
            case UV -> "紫外线消毒柜";
            case HIGH_TEMP -> "高温蒸煮";
            case CHLORINE -> "含氯消毒剂浸泡";
            case DISPOSABLE -> "一次性用品";
        };
    }
}
