package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.Elder;
import com.community.haircut.entity.PatrolRecord;
import com.community.haircut.entity.ServiceOrder;
import com.community.haircut.enums.OrderStatus;
import com.community.haircut.repository.ElderRepository;
import com.community.haircut.repository.PatrolRecordRepository;
import com.community.haircut.repository.ServiceOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElderService {

    private final ElderRepository elderRepository;
    private final ServiceOrderRepository orderRepository;
    private final PatrolRecordRepository patrolRecordRepository;

    public ElderService(ElderRepository elderRepository, ServiceOrderRepository orderRepository,
                        PatrolRecordRepository patrolRecordRepository) {
        this.elderRepository = elderRepository;
        this.orderRepository = orderRepository;
        this.patrolRecordRepository = patrolRecordRepository;
    }

    public List<Elder> list(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return elderRepository.findByNameContaining(keyword.trim());
        }
        return elderRepository.findAll();
    }

    public Elder get(Long id) {
        return elderRepository.findById(id).orElseThrow(() -> new BizException("老人档案不存在"));
    }

    @Transactional
    public Elder create(Elder elder, Long operatorId) {
        elder.setId(null);
        elder.setCreatedBy(operatorId);
        elder.setCreatedAt(java.time.LocalDateTime.now());
        elder.setUpdatedAt(java.time.LocalDateTime.now());
        return elderRepository.save(elder);
    }

    @Transactional
    public Elder update(Long id, Elder input) {
        Elder elder = get(id);
        elder.setName(input.getName());
        elder.setIdCard(input.getIdCard());
        elder.setGender(input.getGender());
        elder.setBirthDate(input.getBirthDate());
        elder.setPhone(input.getPhone());
        elder.setCommunity(input.getCommunity());
        elder.setBuilding(input.getBuilding());
        elder.setUnit(input.getUnit());
        elder.setRoom(input.getRoom());
        elder.setAddress(input.getAddress());
        elder.setBuildingDistance(input.getBuildingDistance());
        elder.setLivingAlone(input.getLivingAlone());
        elder.setHealthCondition(input.getHealthCondition());
        elder.setRiskLevel(input.getRiskLevel());
        elder.setMobility(input.getMobility());
        elder.setNeedFamilyPresent(input.getNeedFamilyPresent());
        elder.setHairPreference(input.getHairPreference());
        elder.setPreferredTime(input.getPreferredTime());
        elder.setSubsidyType(input.getSubsidyType());
        elder.setFamilyContactName(input.getFamilyContactName());
        elder.setFamilyContactPhone(input.getFamilyContactPhone());
        elder.setFamilyUserId(input.getFamilyUserId());
        elder.setPatrolEnabled(input.getPatrolEnabled());
        elder.setPatrolIntervalDays(input.getPatrolIntervalDays());
        elder.setNotes(input.getNotes());
        elder.setStatus(input.getStatus());
        elder.setUpdatedAt(java.time.LocalDateTime.now());
        return elderRepository.save(elder);
    }

    /** 未来 days 天内过生日的老人 */
    public List<Map<String, Object>> upcomingBirthdays(int days) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Elder e : elderRepository.findByStatus("ACTIVE")) {
            if (e.getBirthDate() == null) {
                continue;
            }
            LocalDate birthday = e.getBirthDate().withYear(today.getYear());
            if (birthday.isBefore(today)) {
                birthday = birthday.plusYears(1);
            }
            long diff = ChronoUnit.DAYS.between(today, birthday);
            if (diff <= days) {
                Map<String, Object> item = new HashMap<>();
                item.put("elder", e);
                item.put("birthday", birthday);
                item.put("daysLeft", diff);
                item.put("age", birthday.getYear() - e.getBirthDate().getYear());
                result.add(item);
            }
        }
        result.sort((a, b) -> Long.compare((long) a.get("daysLeft"), (long) b.get("daysLeft")));
        return result;
    }

    /** 定期巡访名单：含下次应巡访日期 */
    public List<Map<String, Object>> patrolList() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Elder e : elderRepository.findByPatrolEnabledTrueAndStatus("ACTIVE")) {
            int interval = e.getPatrolIntervalDays() == null ? 30 : e.getPatrolIntervalDays();
            LocalDate next = e.getLastPatrolAt() == null ? today : e.getLastPatrolAt().plusDays(interval);
            Map<String, Object> item = new HashMap<>();
            item.put("elder", e);
            item.put("nextPatrolDate", next);
            item.put("due", !next.isAfter(today));
            List<PatrolRecord> records = patrolRecordRepository.findByElderIdOrderByVisitAtDesc(e.getId());
            item.put("records", records.stream().limit(5).toList());
            result.add(item);
        }
        result.sort((a, b) -> ((LocalDate) a.get("nextPatrolDate")).compareTo((LocalDate) b.get("nextPatrolDate")));
        return result;
    }

    @Transactional
    public PatrolRecord recordPatrol(Long elderId, Long visitorId, String visitorName, String content, String result) {
        Elder elder = get(elderId);
        PatrolRecord record = new PatrolRecord();
        record.setElderId(elderId);
        record.setVisitorId(visitorId);
        record.setVisitorName(visitorName);
        record.setContent(content);
        record.setResult(result);
        patrolRecordRepository.save(record);
        elder.setLastPatrolAt(LocalDate.now());
        elderRepository.save(elder);
        return record;
    }

    /** 老人服务历史 */
    public List<ServiceOrder> elderOrders(Long elderId) {
        return orderRepository.findByElderIdOrderByCreatedAtDesc(elderId);
    }

    /** 连续取消次数（从最近订单往前数） */
    public int consecutiveCancels(Long elderId) {
        List<ServiceOrder> orders = orderRepository.findByElderIdOrderByCreatedAtDesc(elderId);
        int count = 0;
        for (ServiceOrder o : orders) {
            if (o.getStatus() == OrderStatus.CANCELLED) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }
}
