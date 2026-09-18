package com.community.haircut.repository;

import com.community.haircut.entity.ToolKit;
import com.community.haircut.enums.KitType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ToolKitRepository extends JpaRepository<ToolKit, Long> {

    Optional<ToolKit> findByBarberId(Long barberId);

    Optional<ToolKit> findFirstByBarberIdAndKitType(Long barberId, KitType kitType);

    List<ToolKit> findByKitType(KitType kitType);

    List<ToolKit> findByDisinfectionPendingTrue();
}
