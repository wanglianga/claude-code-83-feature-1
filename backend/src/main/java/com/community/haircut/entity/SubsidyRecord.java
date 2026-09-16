package com.community.haircut.entity;

import com.community.haircut.enums.SubsidyStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 公益补贴记录，财务审核发放。
 */
@Data
@Entity
@Table(name = "subsidy_record")
public class SubsidyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long elderId;

    @Column(nullable = false, length = 50)
    private String elderName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubsidyStatus status = SubsidyStatus.PENDING;

    @Column(length = 50)
    private String reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
