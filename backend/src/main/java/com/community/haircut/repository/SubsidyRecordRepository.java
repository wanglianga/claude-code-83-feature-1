package com.community.haircut.repository;

import com.community.haircut.entity.SubsidyRecord;
import com.community.haircut.enums.SubsidyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubsidyRecordRepository extends JpaRepository<SubsidyRecord, Long> {

    List<SubsidyRecord> findByStatusOrderByCreatedAtDesc(SubsidyStatus status);

    List<SubsidyRecord> findByOrderId(Long orderId);

    List<SubsidyRecord> findAllByOrderByCreatedAtDesc();
}
