package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.*;
import com.community.haircut.enums.*;
import com.community.haircut.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 关怀任务：长期未预约 / 连续取消的老人自动转给网格员做关怀确认。
 */
@Service
public class CareTaskService {

    /** 超过该天数无有效预约视为长期未预约 */
    public static final long NO_APPOINTMENT_DAYS = 60;
    /** 连续取消达到该次数触发关怀 */
    public static final int CANCEL_THRESHOLD = 2;

    private final CareTaskRepository careTaskRepository;
    private final ElderRepository elderRepository;
    private final ServiceOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private int gridRoundRobin = 0;

    public CareTaskService(CareTaskRepository careTaskRepository, ElderRepository elderRepository,
                           ServiceOrderRepository orderRepository, UserRepository userRepository,
                           NotificationService notificationService) {
        this.careTaskRepository = careTaskRepository;
        this.elderRepository = elderRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /** 每天凌晨 2 点自动扫描；也可手动触发 */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void scheduledScan() {
        scan();
    }

    @Transactional
    public int scan() {
        List<User> gridWorkers = userRepository.findByRoleAndStatus(Role.GRID, "ACTIVE");
        if (gridWorkers.isEmpty()) {
            return 0;
        }
        int created = 0;
        LocalDateTime threshold = LocalDateTime.now().minusDays(NO_APPOINTMENT_DAYS);
        for (Elder elder : elderRepository.findByStatus("ACTIVE")) {
            List<ServiceOrder> orders = orderRepository.findByElderIdOrderByCreatedAtDesc(elder.getId());

            // 1. 长期未预约
            boolean hasRecent = orders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .anyMatch(o -> o.getCreatedAt().isAfter(threshold));
            if (!hasRecent && !careTaskRepository.existsByElderIdAndSourceAndStatus(
                    elder.getId(), CareTaskSource.NO_APPOINTMENT, CareTaskStatus.PENDING)) {
                createTask(elder, CareTaskSource.NO_APPOINTMENT,
                        "超过 " + NO_APPOINTMENT_DAYS + " 天无有效理发预约，请关怀确认老人近况", gridWorkers);
                created++;
            }

            // 2. 连续取消（因工具问题取消/改约不算老人违约，不计入也不打断统计）
            int cancels = 0;
            for (ServiceOrder o : orders) {
                if (o.getStatus() == OrderStatus.CANCELLED) {
                    if ("TOOL_ISSUE".equals(o.getCancelSource())) {
                        continue;
                    }
                    cancels++;
                } else {
                    break;
                }
            }
            if (cancels >= CANCEL_THRESHOLD && !careTaskRepository.existsByElderIdAndSourceAndStatus(
                    elder.getId(), CareTaskSource.REPEATED_CANCEL, CareTaskStatus.PENDING)) {
                createTask(elder, CareTaskSource.REPEATED_CANCEL,
                        "连续 " + cancels + " 次取消理发预约，请上门或电话关怀确认原因", gridWorkers);
                created++;
            }
        }
        return created;
    }

    private void createTask(Elder elder, CareTaskSource source, String notes, List<User> gridWorkers) {
        CareTask task = new CareTask();
        task.setElderId(elder.getId());
        task.setElderName(elder.getName());
        task.setSource(source);
        task.setNotes(notes);
        User worker = gridWorkers.get(gridRoundRobin++ % gridWorkers.size());
        task.setGridWorkerId(worker.getId());
        careTaskRepository.save(task);
        notificationService.notify(worker.getId(), "新关怀任务",
                "老人「" + elder.getName() + "」：" + notes, "CARE");
    }

    @Transactional
    public CareTask createManual(Long elderId, String notes, Long gridWorkerId) {
        Elder elder = elderRepository.findById(elderId).orElseThrow(() -> new BizException("老人档案不存在"));
        CareTask task = new CareTask();
        task.setElderId(elder.getId());
        task.setElderName(elder.getName());
        task.setSource(CareTaskSource.MANUAL);
        task.setNotes(notes);
        if (gridWorkerId != null) {
            task.setGridWorkerId(gridWorkerId);
        } else {
            List<User> gridWorkers = userRepository.findByRoleAndStatus(Role.GRID, "ACTIVE");
            if (!gridWorkers.isEmpty()) {
                task.setGridWorkerId(gridWorkers.get(0).getId());
            }
        }
        CareTask saved = careTaskRepository.save(task);
        if (saved.getGridWorkerId() != null) {
            notificationService.notify(saved.getGridWorkerId(), "新关怀任务",
                    "老人「" + elder.getName() + "」：" + notes, "CARE");
        }
        return saved;
    }

    @Transactional
    public void confirm(Long taskId, String result, boolean needExtraCare, OrderService.LoginUserInfo operator) {
        CareTask task = careTaskRepository.findById(taskId).orElseThrow(() -> new BizException("关怀任务不存在"));
        if (task.getStatus() != CareTaskStatus.PENDING) {
            throw new BizException("该任务已处理");
        }
        task.setStatus(CareTaskStatus.CONFIRMED);
        task.setResult(result + (needExtraCare ? "（需要额外关怀）" : ""));
        task.setConfirmedAt(LocalDateTime.now());
        careTaskRepository.save(task);
        if (needExtraCare) {
            notificationService.notifyRole(Role.STAFF, "老人需要额外关怀",
                    "老人「" + task.getElderName() + "」关怀确认结果：" + result, "CARE");
        }
    }

    public List<CareTask> list(CareTaskStatus status, Long gridWorkerId) {
        if (gridWorkerId != null) {
            return careTaskRepository.findByGridWorkerIdOrderByCreatedAtDesc(gridWorkerId);
        }
        if (status != null) {
            return careTaskRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return careTaskRepository.findAllByOrderByCreatedAtDesc();
    }
}
