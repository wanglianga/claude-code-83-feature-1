package com.community.haircut.entity;

import com.community.haircut.enums.ExceptionStatus;
import com.community.haircut.enums.ExceptionType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务异常：老人不适、家属改约、理发师迟到、皮肤划伤、拒绝付款、补贴资格变化、工具遗漏。
 */
@Data
@Entity
@Table(name = "order_exception")
public class ExceptionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExceptionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExceptionStatus status = ExceptionStatus.OPEN;

    private Long reportedById;

    @Column(length = 50)
    private String reportedByName;

    @Column(length = 20)
    private String reportedByRole;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String resolution;

    private Long handledById;

    /** 是否需要财务介入 */
    @Column(nullable = false)
    private Boolean financeInvolved = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;
}
