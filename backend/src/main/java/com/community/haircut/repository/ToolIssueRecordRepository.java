package com.community.haircut.repository;

import com.community.haircut.entity.ToolIssueRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolIssueRecordRepository extends JpaRepository<ToolIssueRecord, Long> {

    List<ToolIssueRecord> findByOrderId(Long orderId);

    List<ToolIssueRecord> findAllByOrderByHandledAtDesc();
}
