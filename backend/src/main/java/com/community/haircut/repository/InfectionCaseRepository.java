package com.community.haircut.repository;

import com.community.haircut.entity.InfectionCase;
import com.community.haircut.enums.InfectionCaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InfectionCaseRepository extends JpaRepository<InfectionCase, Long> {

    List<InfectionCase> findAllByOrderByCreatedAtDesc();

    List<InfectionCase> findByStatusNotOrderByCreatedAtDesc(InfectionCaseStatus status);

    List<InfectionCase> findByBarberIdOrderByCreatedAtDesc(Long barberId);
}
