package com.community.haircut.repository;

import com.community.haircut.entity.ToolKit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ToolKitRepository extends JpaRepository<ToolKit, Long> {

    Optional<ToolKit> findByBarberId(Long barberId);
}
