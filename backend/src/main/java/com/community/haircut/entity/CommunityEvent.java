package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 社区活动（集中理发服务）。
 */
@Data
@Entity
@Table(name = "community_event")
public class CommunityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(length = 20)
    private String timeSlot;

    private String location;

    @Column(nullable = false)
    private Integer maxElders = 20;

    /** OPEN / CLOSED / FINISHED */
    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(length = 500)
    private String notes;

    private LocalDateTime createdAt = LocalDateTime.now();
}
