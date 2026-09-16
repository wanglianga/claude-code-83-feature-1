package com.community.haircut.repository;

import com.community.haircut.entity.PatrolRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatrolRecordRepository extends JpaRepository<PatrolRecord, Long> {

    List<PatrolRecord> findByElderIdOrderByVisitAtDesc(Long elderId);
}
