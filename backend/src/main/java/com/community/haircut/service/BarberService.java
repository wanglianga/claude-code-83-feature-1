package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.*;
import com.community.haircut.enums.DisinfectionStatus;
import com.community.haircut.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BarberService {

    private final BarberProfileRepository barberProfileRepository;
    private final BarberScheduleRepository scheduleRepository;
    private final ToolKitRepository toolKitRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    public BarberService(BarberProfileRepository barberProfileRepository,
                         BarberScheduleRepository scheduleRepository, ToolKitRepository toolKitRepository,
                         UserRepository userRepository, OrderService orderService) {
        this.barberProfileRepository = barberProfileRepository;
        this.scheduleRepository = scheduleRepository;
        this.toolKitRepository = toolKitRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    /** 理发师列表（含信用信息） */
    public List<Map<String, Object>> listBarbers() {
        return barberProfileRepository.findAll().stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("profile", p);
            userRepository.findById(p.getUserId()).ifPresent(u -> {
                map.put("realName", u.getRealName());
                map.put("phone", u.getPhone());
                map.put("status", u.getStatus());
            });
            toolKitRepository.findByBarberId(p.getUserId()).ifPresent(kit -> {
                map.put("toolKit", kit);
                map.put("effectiveDisinfection", orderService.effectiveDisinfectionStatus(kit));
            });
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
        BarberProfile profile = barberProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException("理发师档案不存在"));
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

    // ---------------- 工具包 ----------------

    public ToolKit getToolKit(Long barberId) {
        return toolKitRepository.findByBarberId(barberId).orElse(null);
    }

    @Transactional
    public ToolKit saveToolKit(Long barberId, String name, String items, String notes) {
        ToolKit kit = toolKitRepository.findByBarberId(barberId).orElseGet(() -> {
            ToolKit k = new ToolKit();
            k.setBarberId(barberId);
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

    /** 完成消毒：状态置为已消毒并记录时间（48 小时有效） */
    @Transactional
    public ToolKit disinfect(Long barberId) {
        ToolKit kit = toolKitRepository.findByBarberId(barberId)
                .orElseThrow(() -> new BizException("请先建立工具包档案"));
        kit.setStatus(DisinfectionStatus.DISINFECTED);
        kit.setDisinfectedAt(LocalDateTime.now());
        return toolKitRepository.save(kit);
    }
}
