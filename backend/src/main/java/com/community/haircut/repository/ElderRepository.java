package com.community.haircut.repository;

import com.community.haircut.entity.Elder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ElderRepository extends JpaRepository<Elder, Long> {

    List<Elder> findByStatus(String status);

    List<Elder> findByNameContaining(String name);

    List<Elder> findByPatrolEnabledTrueAndStatus(String status);

    List<Elder> findByFamilyUserId(Long familyUserId);
}
