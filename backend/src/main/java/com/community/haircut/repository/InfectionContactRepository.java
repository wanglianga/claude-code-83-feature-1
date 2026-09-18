package com.community.haircut.repository;

import com.community.haircut.entity.InfectionContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InfectionContactRepository extends JpaRepository<InfectionContact, Long> {

    List<InfectionContact> findByCaseIdOrderByIdAsc(Long caseId);
}
