package com.community.haircut.repository;

import com.community.haircut.entity.InfectionTrace;
import com.community.haircut.enums.InfectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InfectionTraceRepository extends JpaRepository<InfectionTrace, Long> {

    List<InfectionTrace> findAllByOrderByCreatedAtDesc();

    List<InfectionTrace> findByStatusNotOrderByCreatedAtDesc(InfectionStatus status);

    List<InfectionTrace> findByBarberIdOrderByCreatedAtDesc(Long barberId);
}
