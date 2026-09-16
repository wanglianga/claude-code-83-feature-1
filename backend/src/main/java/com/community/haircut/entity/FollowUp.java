package com.community.haircut.entity;

import com.community.haircut.enums.FollowUpStatus;
import com.community.haircut.enums.FollowUpType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回访记录：服务回访 / 异常回访 / 关怀回访。
 */
@Data
@Entity
@Table(name = "follow_up")
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long elderId;

    @Column(nullable = false, length = 50)
    private String elderName;

    /** 关联服务单，可为空（关怀回访） */
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FollowUpType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FollowUpStatus status = FollowUpStatus.PENDING;

    @Column(length = 1000)
    private String content;

    /** 回访结果记录 */
    @Column(length = 1000)
    private String result;

    /** 是否需要额外关怀 */
    @Column(nullable = false)
    private Boolean needExtraCare = false;

    private Long createdById;

    @Column(length = 50)
    private String createdByName;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime doneAt;
}
