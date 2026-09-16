package com.community.haircut.repository;

import com.community.haircut.entity.FollowUp;
import com.community.haircut.enums.FollowUpStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    List<FollowUp> findByElderIdOrderByCreatedAtDesc(Long elderId);

    List<FollowUp> findByOrderId(Long orderId);

    List<FollowUp> findByStatusOrderByCreatedAtDesc(FollowUpStatus status);

    List<FollowUp> findAllByOrderByCreatedAtDesc();
}
