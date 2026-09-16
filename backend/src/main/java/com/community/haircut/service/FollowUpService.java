package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.FollowUp;
import com.community.haircut.enums.FollowUpStatus;
import com.community.haircut.enums.FollowUpType;
import com.community.haircut.repository.FollowUpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FollowUpService {

    private final FollowUpRepository followUpRepository;

    public FollowUpService(FollowUpRepository followUpRepository) {
        this.followUpRepository = followUpRepository;
    }

    public List<FollowUp> list(FollowUpStatus status, Long elderId) {
        if (elderId != null) {
            return followUpRepository.findByElderIdOrderByCreatedAtDesc(elderId);
        }
        if (status != null) {
            return followUpRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return followUpRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public FollowUp create(FollowUp followUp) {
        followUp.setId(null);
        followUp.setStatus(FollowUpStatus.PENDING);
        return followUpRepository.save(followUp);
    }

    @Transactional
    public void complete(Long id, String result, boolean needExtraCare) {
        FollowUp followUp = followUpRepository.findById(id).orElseThrow(() -> new BizException("回访记录不存在"));
        if (followUp.getStatus() == FollowUpStatus.DONE) {
            throw new BizException("该回访已完成");
        }
        followUp.setStatus(FollowUpStatus.DONE);
        followUp.setResult(result);
        followUp.setNeedExtraCare(needExtraCare);
        followUp.setDoneAt(LocalDateTime.now());
        followUpRepository.save(followUp);
    }
}
