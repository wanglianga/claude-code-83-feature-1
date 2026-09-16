package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务单事件时间线：所有角色在服务单上的操作与异常都记录在此。
 */
@Data
@Entity
@Table(name = "order_event")
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    /** 事件类型：CREATE / ASSIGN / TOOL_CONFIRM / CHECK_IN / START / COMPLETE / CANCEL / RESCHEDULE / EXCEPTION / RATE / PAYMENT / NOTE */
    @Column(nullable = false, length = 30)
    private String eventType;

    private Long actorId;

    @Column(length = 50)
    private String actorName;

    @Column(length = 20)
    private String actorRole;

    @Column(length = 1000)
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();
}
