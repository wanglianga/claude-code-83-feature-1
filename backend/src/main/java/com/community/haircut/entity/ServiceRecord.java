package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务完成档案：剪发照片、补贴金额、自费金额。
 */
@Data
@Entity
@Table(name = "service_record")
public class ServiceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    /** 剪发后照片（JSON 数组，base64 data URL） */
    @Column(columnDefinition = "TEXT")
    private String haircutPhotos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subsidyAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal selfPayAmount = BigDecimal.ZERO;

    /** 收款备注 */
    private String paymentNote;

    private LocalDateTime completedAt = LocalDateTime.now();
}
