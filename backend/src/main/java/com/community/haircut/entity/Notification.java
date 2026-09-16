package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String content;

    /** ORDER / EXCEPTION / FINANCE / CARE / SYSTEM */
    @Column(length = 20)
    private String type = "SYSTEM";

    @Column(nullable = false)
    private Boolean readFlag = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
