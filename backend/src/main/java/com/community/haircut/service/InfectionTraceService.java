package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.*;
import com.community.haircut.enums.*;
import com.community.haircut.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 交叉感染追溯：接报老人/家属皮肤瘙痒、红疹、感染反馈，或发现已用未达标工具完成服务后，
 * 社区按理发师、服务包、工具编号和同日服务记录反查，列出同工具服务的其他老人，
 * 通知家属和志愿者、建议就医、记录结果；确认交叉感染或消毒责任的，暂停理发师上门资格、
 * 安排消毒培训与工具复检、下调派单权重。
 */
@Service
public class InfectionTraceService {

    /** 确认责任后默认下调的派单权重与信用分 */
    public static final int DEFAULT_WEIGHT_DEDUCTION = 30;
    public static final int CREDIT_DEDUCTION = 20;

    private final InfectionCaseRepository caseRepository;
    private final InfectionContactRepository contactRepository;
    private final ServiceOrderRepository orderRepository;
    private final ElderRepository elderRepository;
    private final BarberProfileRepository barberProfileRepository;
    private final UserRepository userRepository;
    private final ToolKitRepository toolKitRepository;
    private final NotificationService notificationService;
    private final OrderService orderService;

    private int caseSeq = 0;

    public InfectionTraceService(InfectionCaseRepository caseRepository,
                                 InfectionContactRepository contactRepository,
                                 ServiceOrderRepository orderRepository, ElderRepository elderRepository,
                                 BarberProfileRepository barberProfileRepository, UserRepository userRepository,
                                 ToolKitRepository toolKitRepository, NotificationService notificationService,
                                 OrderService orderService) {
        this.caseRepository = caseRepository;
        this.contactRepository = contactRepository;
        this.orderRepository = orderRepository;
        this.elderRepository = elderRepository;
        this.barberProfileRepository = barberProfileRepository;
        this.userRepository = userRepository;
        this.toolKitRepository = toolKitRepository;
        this.notificationService = notificationService;
        this.orderService = orderService;
    }

    /**
     * 发起追溯：以触发服务单的理发师/服务包/工具编号快照 + 同日服务记录反查。
     */
    @Transactional
    public InfectionCase createCase(Long triggerOrderId, String symptom, String description,
                                    OrderService.LoginUserInfo operator) {
        ServiceOrder order = orderRepository.findById(triggerOrderId)
                .orElseThrow(() -> new BizException("服务单不存在"));
        if (order.getBarberId() == null) {
            throw new BizException("该服务单没有理发师信息，无法按理发师反查");
        }
        if (caseRepository.findAll().stream().anyMatch(c -> triggerOrderId.equals(c.getTriggerOrderId())
                && c.getStatus() != InfectionCaseStatus.RULED_OUT)) {
            throw new BizException("该服务单已存在进行中的追溯单");
        }

        InfectionCase c = new InfectionCase();
        c.setCaseNo(genCaseNo());
        c.setTriggerOrderId(order.getId());
        c.setElderId(order.getElderId());
        c.setElderName(order.getElderName());
        c.setBarberId(order.getBarberId());
        c.setBarberName(order.getBarberName());
        c.setKitId(order.getUsedKitId());
        c.setKitName(order.getUsedKitName());
        c.setToolItems(order.getUsedToolItems());
        c.setSymptom(symptom);
        c.setExposureDate(order.getScheduledDate());
        c.setDescription(description);
        if (operator != null) {
            c.setReportedById(operator.userId());
            c.setReportedByName(operator.realName());
            c.setReportedByRole(operator.role() == null ? "SYSTEM" : operator.role().name());
        } else {
            c.setReportedByRole("SYSTEM");
            c.setReportedByName("系统自动");
        }
        caseRepository.save(c);

        List<InfectionContact> contacts = traceContacts(c, order);
        contacts.forEach(contactRepository::save);

        orderService.addEvent(order.getId(), "EXCEPTION",
                operator == null ? null : operator.userId(),
                operator == null ? "系统自动" : operator.realName(),
                operator == null ? null : operator.role(),
                "发起交叉感染追溯（单号 " + c.getCaseNo() + "）：症状/原因=" + symptom
                        + "；按理发师「" + order.getBarberName() + "」、服务包「" + order.getUsedKitName()
                        + "」、工具编号（" + order.getUsedToolItems() + "）反查 " + c.getExposureDate()
                        + " 同日服务，命中其他老人 " + contacts.size() + " 人");
        notificationService.notifyRole(Role.STAFF, "新交叉感染追溯单 " + c.getCaseNo(),
                "老人「" + order.getElderName() + "」反馈" + symptom + "，已反查出同日同工具服务老人 "
                        + contacts.size() + " 人，请尽快通知家属和志愿者并建议就医", "INFECTION");
        return c;
    }

    /**
     * 反查：同一理发师、同一日期、同一服务包或工具编号有交集的其他服务单（排除已取消与触发单）。
     */
    private List<InfectionContact> traceContacts(InfectionCase c, ServiceOrder trigger) {
        Set<String> triggerTools = parseTools(trigger.getUsedToolItems());
        List<InfectionContact> result = new ArrayList<>();
        List<ServiceOrder> sameDay = orderRepository.findByScheduledDate(c.getExposureDate());
        for (ServiceOrder o : sameDay) {
            if (o.getId().equals(trigger.getId()) || o.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }
            if (!Objects.equals(o.getBarberId(), trigger.getBarberId())) {
                continue;
            }
            boolean sameKit = trigger.getUsedKitId() != null && trigger.getUsedKitId().equals(o.getUsedKitId());
            Set<String> shared = new LinkedHashSet<>(parseTools(o.getUsedToolItems()));
            shared.retainAll(triggerTools);
            if (!sameKit && shared.isEmpty()) {
                continue;
            }
            InfectionContact ct = new InfectionContact();
            ct.setCaseId(c.getId());
            ct.setOrderId(o.getId());
            ct.setElderId(o.getElderId());
            ct.setElderName(o.getElderName());
            ct.setVolunteerId(o.getVolunteerId());
            ct.setVolunteerName(o.getVolunteerName());
            ct.setSharedTools(sameKit && shared.isEmpty() ? "同一服务包" : String.join("、", shared));
            elderRepository.findById(o.getElderId()).ifPresent(e -> {
                ct.setFamilyUserId(e.getFamilyUserId());
                ct.setFamilyContactName(e.getFamilyContactName());
            });
            result.add(ct);
        }
        return result;
    }

    private Set<String> parseTools(String items) {
        Set<String> set = new LinkedHashSet<>();
        if (items == null || items.isBlank()) {
            return set;
        }
        for (String t : items.split("[,，、;；/]")) {
            String s = t.trim();
            if (!s.isEmpty()) {
                set.add(s);
            }
        }
        return set;
    }

    /** 一键通知所有接触老人的家属与陪同志愿者，并建议就医 */
    @Transactional
    public InfectionCase notifyContacts(Long caseId, OrderService.LoginUserInfo operator) {
        InfectionCase c = get(caseId);
        List<InfectionContact> contacts = contactRepository.findByCaseIdOrderByIdAsc(caseId);
        int families = 0, volunteers = 0;
        for (InfectionContact ct : contacts) {
            String content = "【交叉感染风险提醒】" + c.getExposureDate() + " 老人「" + ct.getElderName()
                    + "」与反馈「" + c.getSymptom() + "」的服务由同一理发师「" + c.getBarberName()
                    + "」使用同批工具（" + ct.getSharedTools() + "）服务。请留意是否有皮肤瘙痒、红疹等症状，建议尽早就医检查，结果请反馈社区。";
            if (ct.getFamilyUserId() != null) {
                notificationService.notify(ct.getFamilyUserId(), "就医建议与风险提醒 " + c.getCaseNo(), content, "INFECTION");
                families++;
            }
            if (ct.getVolunteerId() != null) {
                notificationService.notify(ct.getVolunteerId(), "协助关注老人状况 " + c.getCaseNo(), content, "INFECTION");
                volunteers++;
            }
            ct.setFamilyNotified(ct.getFamilyUserId() != null);
            ct.setVolunteerNotified(ct.getVolunteerId() != null);
            ct.setMedicalAdvised(true);
            ct.setNotifiedAt(LocalDateTime.now());
            ct.setUpdatedAt(LocalDateTime.now());
            contactRepository.save(ct);
        }
        c.setStatus(InfectionCaseStatus.NOTIFIED);
        caseRepository.save(c);
        orderService.addEvent(c.getTriggerOrderId(), "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                "已通知同工具服务老人家属 " + families + " 人、志愿者 " + volunteers
                        + " 人，均建议就医并等待随访结果");
        return c;
    }

    /** 记录某位接触老人的就医/随访结果 */
    @Transactional
    public InfectionContact recordContactResult(Long contactId, String followUpResult, boolean confirmedInfected,
                                                OrderService.LoginUserInfo operator) {
        InfectionContact ct = contactRepository.findById(contactId)
                .orElseThrow(() -> new BizException("接触者记录不存在"));
        ct.setFollowUpResult(followUpResult);
        ct.setConfirmedInfected(confirmedInfected);
        ct.setUpdatedAt(LocalDateTime.now());
        contactRepository.save(ct);
        InfectionCase c = get(ct.getCaseId());
        orderService.addEvent(c.getTriggerOrderId(), "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                "记录接触老人「" + ct.getElderName() + "」就医/随访结果：" + followUpResult
                        + (confirmedInfected ? "（确诊感染）" : "（未确诊）"));
        if (confirmedInfected) {
            notificationService.notifyRole(Role.STAFF, "接触老人确诊 " + c.getCaseNo(),
                    "老人「" + ct.getElderName() + "」确诊感染，请尽快确认交叉感染与消毒责任并处置理发师", "INFECTION");
        }
        return ct;
    }

    /**
     * 社区确认结论：确认交叉感染或消毒责任的，暂停理发师上门资格、安排消毒培训与工具复检、
     * 下调派单权重与信用分；否则排除。
     */
    @Transactional
    public InfectionCase conclude(Long caseId, boolean crossInfectionConfirmed, boolean disinfectionResponsible,
                                  Integer weightDeduction, String conclusion, OrderService.LoginUserInfo operator) {
        InfectionCase c = get(caseId);
        c.setCrossInfectionConfirmed(crossInfectionConfirmed);
        c.setDisinfectionResponsible(disinfectionResponsible);
        c.setConclusion(conclusion);
        c.setConcludedById(operator.userId());
        c.setConcludedByName(operator.realName());
        c.setConcludedAt(LocalDateTime.now());

        if (crossInfectionConfirmed || disinfectionResponsible) {
            c.setStatus(InfectionCaseStatus.CONFIRMED);
            c.setBarberSuspended(true);
            c.setTrainingRequired(true);
            c.setRecheckRequired(true);
            int deduct = weightDeduction == null ? DEFAULT_WEIGHT_DEDUCTION : Math.max(0, weightDeduction);
            c.setWeightDeduction(deduct);
            BarberProfile p = barberProfileRepository.findByUserId(c.getBarberId())
                    .orElseThrow(() -> new BizException("理发师档案不存在"));
            p.setSuspended(true);
            p.setSuspendReason("交叉感染追溯单 " + c.getCaseNo() + " 确认"
                    + (crossInfectionConfirmed ? "交叉感染" : "")
                    + (crossInfectionConfirmed && disinfectionResponsible ? "/" : "")
                    + (disinfectionResponsible ? "消毒责任" : ""));
            p.setSuspendedAt(LocalDateTime.now());
            p.setInfectionCount(p.getInfectionCount() + 1);
            p.setDispatchWeight(Math.max(0, p.getDispatchWeight() - deduct));
            p.setCreditScore(Math.max(0, p.getCreditScore() - CREDIT_DEDUCTION));
            // 培训与复检需重新完成后才能恢复资格
            p.setDisinfectionTrained(false);
            p.setToolRechecked(false);
            barberProfileRepository.save(p);

            orderService.addEvent(c.getTriggerOrderId(), "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                    "追溯结论：确认" + (crossInfectionConfirmed ? "交叉感染" : "")
                            + (crossInfectionConfirmed && disinfectionResponsible ? "、" : "")
                            + (disinfectionResponsible ? "消毒责任" : "")
                            + "；已暂停理发师「" + c.getBarberName() + "」上门资格，安排消毒培训和工具复检，派单权重 -"
                            + deduct + "、信用分 -" + CREDIT_DEDUCTION + "；结论：" + conclusion);
            notificationService.notify(c.getBarberId(), "上门资格已暂停 " + c.getCaseNo(),
                    "因交叉感染追溯确认责任，你的上门资格已暂停，须完成消毒培训并通过工具复检后，由社区恢复；派单权重已下调 "
                            + deduct, "INFECTION");
            notificationService.notifyRole(Role.FINANCE, "交叉感染处置复盘 " + c.getCaseNo(),
                    "理发师「" + c.getBarberName() + "」被暂停资格并下调派单权重，相关投诉、复检与补偿结果进入公益资金复盘", "FINANCE");
        } else {
            c.setStatus(InfectionCaseStatus.RULED_OUT);
            c.setBarberSuspended(false);
            orderService.addEvent(c.getTriggerOrderId(), "EXCEPTION", operator.userId(), operator.realName(), operator.role(),
                    "追溯结论：排除交叉感染与消毒责任；" + conclusion);
        }
        caseRepository.save(c);
        return c;
    }

    public InfectionCase get(Long id) {
        return caseRepository.findById(id).orElseThrow(() -> new BizException("追溯单不存在"));
    }

    public Map<String, Object> detail(Long id) {
        InfectionCase c = get(id);
        Map<String, Object> m = new HashMap<>();
        m.put("case", c);
        m.put("contacts", contactRepository.findByCaseIdOrderByIdAsc(id));
        m.put("triggerOrder", orderRepository.findById(c.getTriggerOrderId()).orElse(null));
        return m;
    }

    public List<InfectionCase> list(InfectionCaseStatus status) {
        if (status != null) {
            return caseRepository.findAll().stream()
                    .filter(c -> c.getStatus() == status)
                    .sorted(Comparator.comparing(InfectionCase::getCreatedAt).reversed())
                    .toList();
        }
        return caseRepository.findAllByOrderByCreatedAtDesc();
    }

    private String genCaseNo() {
        String date = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Set<String> existing = new HashSet<>();
        for (InfectionCase c : caseRepository.findAll()) {
            existing.add(c.getCaseNo());
        }
        int seq = ++caseSeq;
        String no = "JC" + date + "-" + String.format("%03d", seq);
        while (existing.contains(no)) {
            seq++;
            no = "JC" + date + "-" + String.format("%03d", seq);
        }
        return no;
    }
}
