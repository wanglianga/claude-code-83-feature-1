package com.community.haircut.entity;

import com.community.haircut.enums.CareTaskSource;
import com.community.haircut.enums.CareTaskStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 网格员关怀任务：长期未预约 / 连续取消的老人自动转入。
 */
@Data
@Entity
@Table(name = "care_task")
public class CareTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long elderId;

    @Column(nullable = false, length = 50)
    private String elderName;

    private Long gridWorkerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CareTaskSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareTaskStatus status = CareTaskStatus.PENDING;

    @Column(length = 500)
    private String notes;

    /** 关怀确认结果 */
    @Column(length = 1000)
    private String result;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime confirmedAt;
}
