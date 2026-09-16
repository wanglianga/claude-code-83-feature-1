package com.community.haircut.repository;

import com.community.haircut.entity.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {

    Optional<ServiceRecord> findByOrderId(Long orderId);
}
