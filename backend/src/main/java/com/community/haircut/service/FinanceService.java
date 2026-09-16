package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.SubsidyRecord;
import com.community.haircut.enums.Role;
import com.community.haircut.enums.SubsidyStatus;
import com.community.haircut.repository.SubsidyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FinanceService {

    private final SubsidyRecordRepository subsidyRepository;
    private final OrderService orderService;

    public FinanceService(SubsidyRecordRepository subsidyRepository, OrderService orderService) {
        this.subsidyRepository = subsidyRepository;
        this.orderService = orderService;
    }

    public List<SubsidyRecord> list(SubsidyStatus status) {
        if (status != null) {
            return subsidyRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return subsidyRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void review(Long id, boolean approve, String note, OrderService.LoginUserInfo operator) {
        SubsidyRecord record = subsidyRepository.findById(id).orElseThrow(() -> new BizException("补贴记录不存在"));
        if (record.getStatus() != SubsidyStatus.PENDING) {
            throw new BizException("该补贴已审核");
        }
        record.setStatus(approve ? SubsidyStatus.APPROVED : SubsidyStatus.REJECTED);
        record.setReviewedBy(operator.realName());
        record.setReviewedAt(LocalDateTime.now());
        record.setNote(note);
        subsidyRepository.save(record);
        orderService.addEvent(record.getOrderId(), "PAYMENT", operator.userId(), operator.realName(), Role.FINANCE,
                "公益补贴 ¥" + record.getAmount() + (approve ? " 审核通过并发放" : " 被驳回：" + note));
    }
}
