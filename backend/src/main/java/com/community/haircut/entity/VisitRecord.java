package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 志愿者陪同记录：陪同进门、核对老人状态、现场照片、家属授权。
 */
@Data
@Entity
@Table(name = "visit_record")
public class VisitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long volunteerId;

    /** 进门安全确认 */
    @Column(nullable = false)
    private Boolean entrySafe = false;

    /** 老人身体状态核对 */
    @Column(length = 500)
    private String elderState;

    /** 老人精神状态：良好/一般/萎靡/异常 */
    @Column(length = 20)
    private String mentalState;

    /** 家属授权已确认 */
    @Column(nullable = false)
    private Boolean familyAuthorized = false;

    /** 现场照片（JSON 数组，base64 data URL） */
    @Column(columnDefinition = "TEXT")
    private String photos;

    @Column(length = 1000)
    private String notes;

    private LocalDateTime createdAt = LocalDateTime.now();
}
