package com.community.haircut.repository;

import com.community.haircut.entity.DisinfectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisinfectionRecordRepository extends JpaRepository<DisinfectionRecord, Long> {

    List<DisinfectionRecord> findByKitIdOrderByDisinfectedAtDesc(Long kitId);

    List<DisinfectionRecord> findByBarberIdOrderByDisinfectedAtDesc(Long barberId);

    List<DisinfectionRecord> findAllByOrderByDisinfectedAtDesc();
}
