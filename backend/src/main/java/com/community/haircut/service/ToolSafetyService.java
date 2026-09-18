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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 工具消毒失效与交叉感染追溯：
 * 1. 备用服务包管理（使用后补录消毒记录，未补录不得再派单）；
 * 2. 扫码核验不通过的处置（改约/备用包/取消）与空跑补偿责任认定；
 * 3. 交叉感染反查（理发师 + 服务包/工具编号 + 同日服务记录），通知同工具老人家属与志愿者；
 * 4. 确认交叉感染/消毒责任后暂停理发师上门资格、安排培训复检、下调派单权重。
 */
@Service
public class ToolSafetyService {

    /** 默认空跑社区补偿（元），按社区规则认定，可在审核时调整 */
    public static final BigDecimal DEFAULT_EMPTY_RUN_COMP = new BigDecimal("10");
    /** 确认消毒责任后派单权重下调到 */
    public static final double PENALTY_WEIGHT = 0.5;

    private final ToolKitRepository toolKitRepository;
    private final ToolIssueRecordRepository toolIssueRepository;
    private final InfectionTraceRepository traceRepository;
    private final InfectionContactRepository contactRepository;
    private final ServiceOrderRepository orderRepository;
    private final BarberProfileRepository barberProfileRepository;
    private final ElderRepository elderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;

    public ToolSafetyService(ToolKitRepository toolKitRepository, ToolIssueRecordRepository toolIssueRepository,
                             InfectionTraceRepository traceRepository, InfectionContactRepository contactRepository,
                             ServiceOrderRepository orderRepository, BarberProfileRepository barberProfileRepository,
                             ElderRepository elderRepository,
                             UserRepository userRepository, OrderService orderService,
                             NotificationService notificationService) {
        this.toolKitRepository = toolKitRepository;
        this.toolIssueRepository = toolIssueRepository;
        this.traceRepository = traceRepository;
        this.contactRepository = contactRepository;
        this.orderRepository = orderRepository;
        this.barberProfileRepository = barberProfileRepository;
        this.elderRepository = elderRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.notificationService = notificationService;
    }

    // ================= 备用服务包与消毒记录 =================

    public List<ToolKit> listBackupKits() {
        return toolKitRepository.findByKitType(KitType.BACKUP);
    }

    public List<ToolKit> listPendingDisinfection() {
        return toolKitRepository.findByDisinfectionPendingTrue();
    }

    /** 新建备用服务包（社区管理） */
    @Transactional
    public ToolKit createBackupKit(String name, String items, String sealNo, String method,
                                   String cabinetNo, String responsiblePerson) {
        ToolKit kit = new ToolKit();
        kit.setKitType(KitType.BACKUP);
        kit.setName(name);
        kit.setItems(items);
        kit.setSealNo(sealNo);
        kit.setSealIntact(true);
        kit.setDisinfectionMethod(method);
        kit.setCabinetNo(cabinetNo);
        kit.setResponsiblePerson(responsiblePerson);
        kit.setStatus(DisinfectionStatus.DISINFECTED);
        kit.setDisinfectedAt(LocalDateTime.now());
        kit.setDisinfectionPending(false);
        return toolKitRepository.save(kit);
    }

    /**
     * 补录/登记消毒记录：封签编号、消毒方式、消毒柜编号、责任人。
     * 备用服务包补录后解除「待补录」，方可再次派单。
     */
    @Transactional
    public ToolKit recordDisinfection(Long kitId, String sealNo, String method, String cabinetNo,
                                      String responsiblePerson, OrderService.LoginUserInfo operator) {
        ToolKit kit = toolKitRepository.findById(kitId).orElseThrow(() -> new BizException("服务包不存在"));
        if (sealNo != null) kit.setSealNo(sealNo);
        if (method != null) kit.setDisinfectionMethod(method);
        if (cabinetNo != null) kit.setCabinetNo(cabinetNo);
        if (responsiblePerson != null) kit.setResponsiblePerson(responsiblePerson);
        kit.setSealIntact(true);
        kit.setStatus(DisinfectionStatus.DISINFECTED);
        kit.setDisinfectedAt(LocalDateTime.now());
        boolean wasPending = Boolean.TRUE.equals(kit.getDisinfectionPending());
        kit.setDisinfectionPending(false);
        toolKitRepository.save(kit);
        if (wasPending) {
            notificationService.notifyRole(Role.STAFF, "备用服务包已补录消毒",
                    "备用服务包「" + kit.getName() + "」已补录消毒记录（封签 " + sealNo + "），恢复可派单状态", "ORDER");
        }
        return kit;
    }

    // ================= 核验不通过处置 =================

    /**
     * 扫码核验不通过时的处置：
     * RESCHEDULE 联系社区改约；BACKUP 启用备用服务包；CANCEL 取消（不算老人违约）。
     * 同步认定空跑补偿：工具遗漏/消毒失责由理发师承担，其余按社区规则补偿。
     */
    @Transactional
    public ToolIssueRecord handleIssue(Long orderId, String issueType, String issueDetail, String action,
                                       Long backupKitId, LocalDate rescheduleDate, String timeSlot,
                                       OrderService.LoginUserInfo operator) {
        ServiceOrder order = orderService.get(orderId);
        ToolIssueType type = ToolIssueType.valueOf(issueType);
        ToolIssueAction act = ToolIssueAction.valueOf(action);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BizException("服务单已完结，不可进行工具问题处置");
        }

        ToolIssueRecord record = new ToolIssueRecord();
        record.setOrderId(orderId);
        record.setBarberId(order.getBarberId());
        record.setKitId(order.getUsedKitId());
        record.setIssueType(type);
        record.setIssueDetail(issueDetail);
        record.setAction(act);
        record.setHandledById(operator.userId());
        record.setHandledByName(operator.realName());

        // 责任初判：工具遗漏或消毒失责（消毒过期且责任在理发师主包）由理发师承担
        boolean barberFault = type == ToolIssueType.TOOL_MISSING || type == ToolIssueType.DISINFECTION_EXPIRED
                || type == ToolIssueType.TOOL_STAINED;
        if (barberFault) {
            record.setCompensationStatus(CompensationStatus.BARBER_BEAR);
            record.setCompensationAmount(BigDecimal.ZERO);
            record.setCompensationNote("工具遗漏/消毒失责，理发师空跑自行承担");
        } else {
            record.setCompensationStatus(CompensationStatus.PENDING);
            record.setCompensationNote("等待社区按规则认定空跑补偿");
        }
        toolIssueRepository.save(record);

        String issueText = issueName(type) + (issueDetail == null || issueDetail.isBlank() ? "" : "：" + issueDetail);
        orderService.addEvent(orderId, "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                "上门扫码核验发现【" + issueText + "】，不允许开始服务");

        switch (act) {
            case BACKUP -> {
                ToolKit backup = toolKitRepository.findById(backupKitId)
                        .orElseThrow(() -> new BizException("备用服务包不存在"));
                if (backup.getKitType() != KitType.BACKUP) {
                    throw new BizException("所选工具包不是备用服务包");
                }
                if (Boolean.TRUE.equals(backup.getDisinfectionPending())) {
                    throw new BizException("该备用服务包消毒记录尚未补录，不可使用");
                }
                if (orderService.effectiveDisinfectionStatus(backup) != DisinfectionStatus.DISINFECTED) {
                    throw new BizException("备用服务包消毒已过期，请选择其他包或改约");
                }
                record.setBackupKitId(backup.getId());
                toolIssueRepository.save(record);
                // 服务单回到待核验，理发师改用备用包重新扫码；不更换理发师，不视为完成
                order.setStatus(OrderStatus.ASSIGNED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                orderService.addEvent(orderId, "TOOL_CONFIRM", operator.userId(), operator.realName(), operator.role(),
                        "已启用备用服务包「" + backup.getName() + "」（封签 " + backup.getSealNo()
                                + "），请重新扫码核验后开始服务");
                notificationService.notifyRole(Role.STAFF, "启用备用服务包 " + order.getOrderNo(),
                        "服务单启用备用服务包「" + backup.getName() + "」；使用后需补录消毒记录", "ORDER");
            }
            case RESCHEDULE -> {
                if (rescheduleDate == null) {
                    throw new BizException("请选择改约日期");
                }
                orderService.reschedule(orderId, rescheduleDate,
                        timeSlot == null ? order.getTimeSlot() : timeSlot,
                        "工具问题改期：" + issueText, true, operator);
                notificationService.notifyRole(Role.STAFF, "工具问题改约 " + order.getOrderNo(),
                        "老人「" + order.getElderName() + "」因" + issueText + "改约，不算老人违约、不扣补贴", "ORDER");
            }
            case CANCEL -> {
                orderService.cancel(orderId, "工具问题取消：" + issueText, true, operator);
            }
        }
        return record;
    }

    /** 社区/财务认定空跑补偿结果（进入公益资金复盘） */
    @Transactional
    public ToolIssueRecord reviewCompensation(Long issueId, String result, BigDecimal amount, String note,
                                              OrderService.LoginUserInfo operator) {
        ToolIssueRecord record = toolIssueRepository.findById(issueId)
                .orElseThrow(() -> new BizException("工具问题记录不存在"));
        CompensationStatus status = CompensationStatus.valueOf(result);
        record.setCompensationStatus(status);
        if (status == CompensationStatus.COMMUNITY || status == CompensationStatus.PAID) {
            record.setCompensationAmount(amount == null ? DEFAULT_EMPTY_RUN_COMP : amount);
        } else {
            record.setCompensationAmount(BigDecimal.ZERO);
        }
        record.setCompensationNote(note);
        toolIssueRepository.save(record);
        orderService.addEvent(record.getOrderId(), "PAYMENT", operator.userId(), operator.realName(), Role.STAFF,
                "理发师空跑补偿认定：" + compName(status)
                        + (record.getCompensationAmount().compareTo(BigDecimal.ZERO) > 0
                        ? " ¥" + record.getCompensationAmount() + "（公益资金）" : "")
                        + (note == null ? "" : "；" + note));
        if (record.getBarberId() != null && status != CompensationStatus.BARBER_BEAR) {
            notificationService.notify(record.getBarberId(), "空跑补偿认定结果",
                    "您的工具问题空跑已认定为：" + compName(status)
                            + (record.getCompensationAmount().compareTo(BigDecimal.ZERO) > 0
                            ? "，补偿 ¥" + record.getCompensationAmount() : ""), "FINANCE");
        }
        return record;
    }

    public List<ToolIssueRecord> listIssues(boolean pendingOnly) {
        List<ToolIssueRecord> all = toolIssueRepository.findAllByOrderByHandledAtDesc();
        if (pendingOnly) {
            return all.stream().filter(r -> r.getCompensationStatus() == CompensationStatus.PENDING).toList();
        }
        return all;
    }

    // ================= 交叉感染追溯 =================

    /**
     * 登记感染反馈并立即反查：按理发师、服务包、同日服务记录找出同工具服务的其他老人。
     */
    @Transactional
    public InfectionTrace reportInfection(Long orderId, String symptom, String description,
                                          String toolItemNo, OrderService.LoginUserInfo operator) {
        ServiceOrder order = orderService.get(orderId);
        InfectionTrace trace = new InfectionTrace();
        trace.setSourceOrderId(orderId);
        trace.setSourceElderName(order.getElderName());
        trace.setBarberId(order.getBarberId());
        trace.setBarberName(order.getBarberName());
        trace.setKitId(order.getUsedKitId());
        trace.setSealNo(order.getUsedSealNo());
        trace.setToolItemNo(toolItemNo);
        trace.setSymptom(symptom);
        trace.setDescription(description);
        trace.setReporterName(operator.role() == Role.FAMILY ? "家属" : operator.realName());
        trace.setStatus(InfectionStatus.OPEN);
        traceRepository.save(trace);

        orderService.addEvent(orderId, "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                "登记交叉感染追溯：反馈症状【" + symptom + "】" + (description == null ? "" : "：" + description)
                        + "；社区将按理发师、服务包/工具编号与同日服务记录反查");

        // 同工具/同日反查
        traceContacts(trace, order);
        return trace;
    }

    /** 反查同服务包或同理发师同日服务的其他老人，生成接触者名单 */
    @Transactional
    public int traceContacts(InfectionTrace trace, ServiceOrder source) {
        List<InfectionContact> existing = contactRepository.findByTraceId(trace.getId());
        Set<Long> seen = new HashSet<>();
        existing.forEach(c -> seen.add(c.getOrderId()));

        List<ServiceOrder> candidates = new ArrayList<>();
        for (ServiceOrder o : orderRepository.findAll()) {
            if (o.getId().equals(source.getId())) continue;
            if (o.getStatus() != OrderStatus.COMPLETED) continue;
            boolean sameKit = trace.getKitId() != null && trace.getKitId().equals(o.getUsedKitId());
            boolean sameBarberSameDay = trace.getBarberId() != null
                    && trace.getBarberId().equals(o.getBarberId())
                    && o.getScheduledDate() != null && o.getScheduledDate().equals(source.getScheduledDate());
            if (sameKit || sameBarberSameDay) {
                candidates.add(o);
            }
        }
        int created = 0;
        for (ServiceOrder o : candidates) {
            if (seen.contains(o.getId())) continue;
            InfectionContact c = new InfectionContact();
            c.setTraceId(trace.getId());
            c.setOrderId(o.getId());
            c.setElderId(o.getElderId());
            c.setElderName(o.getElderName());
            c.setVolunteerId(o.getVolunteerId());
            c.setVolunteerName(o.getVolunteerName());
            c.setServedAt(o.getCompletedAt() != null ? o.getCompletedAt()
                    : o.getScheduledDate() == null ? null : o.getScheduledDate().atTime(10, 0));
            c.setKitId(o.getUsedKitId());
            elderRepository.findById(o.getElderId()).ifPresent(e -> {
                c.setFamilyUserId(e.getFamilyUserId());
                c.setFamilyContactName(e.getFamilyContactName());
            });
            contactRepository.save(c);
            created++;
        }
        if (created > 0) {
            trace.setStatus(InfectionStatus.TRACING);
            traceRepository.save(trace);
            orderService.addEvent(source.getId(), "EXCEPTION", null, "系统", null,
                    "反查出同工具/同日接受服务的其他老人 " + created + " 人，待通知家属与志愿者");
            notificationService.notifyRole(Role.STAFF, "感染反查名单 " + source.getOrderNo(),
                    "已反查出同工具/同日服务老人 " + created + " 人，请尽快通知家属和志愿者并建议就医", "EXCEPTION");
        } else {
            trace.setStatus(InfectionStatus.TRACING);
            traceRepository.save(trace);
        }
        return created;
    }

    /** 通知某接触者家属与志愿者，并建议就医 */
    @Transactional
    public InfectionContact notifyContact(Long contactId, OrderService.LoginUserInfo operator) {
        InfectionContact c = contactRepository.findById(contactId)
                .orElseThrow(() -> new BizException("接触者记录不存在"));
        InfectionTrace trace = traceRepository.findById(c.getTraceId()).orElseThrow();
        if (c.getFamilyUserId() != null) {
            notificationService.notify(c.getFamilyUserId(), "健康风险提醒（服务追溯）",
                    "「" + c.getElderName() + "」近期由理发师「" + trace.getBarberName()
                            + "」使用同批次工具接受过理发服务，现反馈有皮肤不适情况，建议留意是否出现瘙痒、红疹，必要时及时就医", "EXCEPTION");
            c.setFamilyNotified(true);
        } else if (c.getFamilyContactName() != null) {
            // 无家属账号的老人，由社区/志愿者按档案电话通知家属
            c.setFamilyNotified(true);
        }
        if (c.getVolunteerId() != null) {
            notificationService.notify(c.getVolunteerId(), "协助健康排查（服务追溯）",
                    "请协助关注老人「" + c.getElderName() + "」服务后皮肤情况，如有瘙痒/红疹请协助就医并反馈社区", "EXCEPTION");
            c.setVolunteerNotified(true);
        }
        c.setMedicalAdvised(true);
        c.setNotifiedAt(LocalDateTime.now());
        contactRepository.save(c);

        ServiceOrder order = orderRepository.findById(c.getOrderId()).orElse(null);
        if (order != null) {
            orderService.addEvent(order.getId(), "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                    "交叉感染追溯：已通知家属和志愿者，建议就医排查（追溯来源：" + trace.getSourceElderName() + "）");
        }
        updateTraceNotified(trace);
        return c;
    }

    private void updateTraceNotified(InfectionTrace trace) {
        List<InfectionContact> contacts = contactRepository.findByTraceId(trace.getId());
        boolean all = !contacts.isEmpty() && contacts.stream()
                .allMatch(c -> Boolean.TRUE.equals(c.getFamilyNotified()) || c.getFamilyUserId() == null);
        if (all && !Boolean.TRUE.equals(trace.getNotified())) {
            trace.setNotified(true);
            traceRepository.save(trace);
        }
    }

    /** 记录接触者就医/排查结果与回访 */
    @Transactional
    public InfectionContact recordContactResult(Long contactId, String medicalResult, String followUpNote,
                                                OrderService.LoginUserInfo operator) {
        InfectionContact c = contactRepository.findById(contactId)
                .orElseThrow(() -> new BizException("接触者记录不存在"));
        c.setMedicalResult(medicalResult);
        c.setFollowUpNote(followUpNote);
        c.setResultRecordedAt(LocalDateTime.now());
        contactRepository.save(c);
        ServiceOrder order = orderRepository.findById(c.getOrderId()).orElse(null);
        if (order != null) {
            orderService.addEvent(order.getId(), "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                    "感染追溯排查结果：" + medicalResult + (followUpNote == null ? "" : "；回访：" + followUpNote));
        }
        return c;
    }

    /**
     * 确认交叉感染/消毒责任：暂停理发师上门资格、安排消毒培训与工具复检、下调派单权重。
     */
    @Transactional
    public InfectionTrace confirmResponsibility(Long traceId, boolean crossInfection, boolean disinfectionFault,
                                                double newWeight, String note, OrderService.LoginUserInfo operator) {
        InfectionTrace trace = traceRepository.findById(traceId)
                .orElseThrow(() -> new BizException("追溯记录不存在"));
        if (trace.getStatus() == InfectionStatus.RESOLVED) {
            throw new BizException("该追溯事件已结案，不可再认定责任");
        }
        trace.setCrossInfectionConfirmed(crossInfection);
        trace.setDisinfectionResponsibility(disinfectionFault);
        if (crossInfection || disinfectionFault) {
            trace.setBarberSuspended(true);
            trace.setTrainingArranged(true);
            trace.setReinspectionArranged(true);
            double w = newWeight > 0 ? newWeight : PENALTY_WEIGHT;
            trace.setWeightAdjustNote("派单权重下调至 " + w + "，暂停上门资格，复岗需培训通过+工具复检");
            barberProfileRepository.findByUserId(trace.getBarberId()).ifPresent(p -> {
                p.setVisitSuspended(true);
                p.setSuspendReason("确认交叉感染/消毒责任（追溯单 #" + trace.getId() + "）："
                        + (note == null ? "" : note));
                p.setTrainingPassed(false);
                p.setToolReinspected(false);
                p.setDispatchWeight(w);
                p.setInfectionCount(p.getInfectionCount() + 1);
                p.setCreditScore(Math.max(0, p.getCreditScore() - 20));
                barberProfileRepository.save(p);
            });
            if (trace.getBarberId() != null) {
                notificationService.notify(trace.getBarberId(), "上门资格暂停通知",
                        "因确认交叉感染/消毒责任，您的上门理发资格已暂停，派单权重下调至 " + w
                                + "；请参加消毒培训并配合工具复检，通过后方可复岗", "EXCEPTION");
            }
            // 问题服务包立即停用，等待复检补录
            if (trace.getKitId() != null) {
                toolKitRepository.findById(trace.getKitId()).ifPresent(kit -> {
                    kit.setStatus(DisinfectionStatus.PENDING);
                    kit.setDisinfectionPending(true);
                    kit.setSealIntact(false);
                    toolKitRepository.save(kit);
                });
            }
        }
        traceRepository.save(trace);
        orderService.addEvent(trace.getSourceOrderId(), "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                "确认交叉感染=" + (crossInfection ? "是" : "否") + "，消毒责任=" + (disinfectionFault ? "是" : "否")
                        + "；暂停理发师上门资格、安排消毒培训与工具复检、下调派单权重"
                        + (note == null ? "" : "；" + note));
        notificationService.notifyRole(Role.FINANCE, "感染责任确认，公益资金复盘",
                "理发师「" + trace.getBarberName() + "」确认感染/消毒责任，相关补偿与培训复检费用进入公益资金复盘", "FINANCE");
        return trace;
    }

    /**
     * 复检结案：培训通过 + 工具复检通过后方可恢复上门资格（权重保持下调后的水平，由社区视情恢复）。
     */
    @Transactional
    public InfectionTrace resolveTrace(Long traceId, boolean trainingPassed, boolean toolReinspected,
                                       boolean restoreWeight, String resolution,
                                       OrderService.LoginUserInfo operator) {
        InfectionTrace trace = traceRepository.findById(traceId)
                .orElseThrow(() -> new BizException("追溯记录不存在"));
        if (trace.getStatus() == InfectionStatus.RESOLVED) {
            throw new BizException("该追溯事件已结案，不可重复处理");
        }
        if (Boolean.TRUE.equals(trace.getBarberSuspended()) && (!trainingPassed || !toolReinspected)) {
            throw new BizException("复岗条件未满足：消毒培训与工具复检均需通过");
        }
        trace.setStatus(InfectionStatus.RESOLVED);
        trace.setResolution(resolution);
        trace.setResolvedAt(LocalDateTime.now());
        trace.setHandledById(operator.userId());
        trace.setHandledByName(operator.realName());
        traceRepository.save(trace);

        barberProfileRepository.findByUserId(trace.getBarberId()).ifPresent(p -> {
            if (Boolean.TRUE.equals(trace.getBarberSuspended())) {
                p.setTrainingPassed(trainingPassed);
                p.setToolReinspected(toolReinspected);
                p.setVisitSuspended(false);
                p.setSuspendReason(null);
                if (restoreWeight) {
                    p.setDispatchWeight(1.0);
                    trace.setWeightAdjustNote("培训复检通过，派单权重恢复为 1.0");
                    traceRepository.save(trace);
                }
                barberProfileRepository.save(p);
                if (trace.getBarberId() != null) {
                    notificationService.notify(trace.getBarberId(), "上门资格恢复",
                            "消毒培训与工具复检已通过，您的上门理发资格恢复，可重新接受派单", "ORDER");
                }
            }
        });
        orderService.addEvent(trace.getSourceOrderId(), "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                "交叉感染追溯结案：" + resolution);
        return trace;
    }

    public List<InfectionTrace> listTraces(boolean openOnly) {
        if (openOnly) {
            return traceRepository.findByStatusNotOrderByCreatedAtDesc(InfectionStatus.RESOLVED);
        }
        return traceRepository.findAllByOrderByCreatedAtDesc();
    }

    public InfectionTrace getTrace(Long id) {
        return traceRepository.findById(id).orElseThrow(() -> new BizException("追溯记录不存在"));
    }

    public List<InfectionContact> listContacts(Long traceId) {
        return contactRepository.findByTraceId(traceId);
    }

    // ---------------- 标签 ----------------

    public static String issueName(ToolIssueType t) {
        return switch (t) {
            case DISINFECTION_EXPIRED -> "消毒超过有效期";
            case SEAL_BROKEN -> "封签破损";
            case TOWEL_DAMP -> "毛巾围布受潮";
            case TOOL_STAINED -> "剪刀剃刀有污渍";
            case TOOL_MISSING -> "工具遗漏";
        };
    }

    public static String compName(CompensationStatus s) {
        return switch (s) {
            case PENDING -> "待认定";
            case COMMUNITY -> "社区规则补偿（待发放）";
            case PAID -> "社区补偿已发放";
            case BARBER_BEAR -> "理发师自行承担";
            case NONE -> "无需补偿";
        };
    }
}
