package com.community.haircut.repository;

import com.community.haircut.entity.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {

    Optional<VisitRecord> findByOrderId(Long orderId);
}
