package com.community.haircut.repository;

import com.community.haircut.entity.ExceptionRecord;
import com.community.haircut.enums.ExceptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExceptionRecordRepository extends JpaRepository<ExceptionRecord, Long> {

    List<ExceptionRecord> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<ExceptionRecord> findByStatusOrderByCreatedAtDesc(ExceptionStatus status);

    List<ExceptionRecord> findByFinanceInvolvedTrueAndStatusNot(ExceptionStatus status);

    List<ExceptionRecord> findAllByOrderByCreatedAtDesc();
}
