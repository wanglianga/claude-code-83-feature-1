package com.community.haircut.repository;

import com.community.haircut.entity.ToolConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ToolConfirmationRepository extends JpaRepository<ToolConfirmation, Long> {

    Optional<ToolConfirmation> findByOrderId(Long orderId);
}
