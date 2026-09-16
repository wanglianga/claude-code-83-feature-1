package com.community.haircut.repository;

import com.community.haircut.entity.CareTask;
import com.community.haircut.enums.CareTaskSource;
import com.community.haircut.enums.CareTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareTaskRepository extends JpaRepository<CareTask, Long> {

    boolean existsByElderIdAndSourceAndStatus(Long elderId, CareTaskSource source, CareTaskStatus status);

    List<CareTask> findByStatusOrderByCreatedAtDesc(CareTaskStatus status);

    List<CareTask> findByGridWorkerIdOrderByCreatedAtDesc(Long gridWorkerId);

    List<CareTask> findAllByOrderByCreatedAtDesc();
}
