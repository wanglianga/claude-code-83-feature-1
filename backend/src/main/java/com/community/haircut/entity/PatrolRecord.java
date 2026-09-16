package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 定期巡访记录。
 */
@Data
@Entity
@Table(name = "patrol_record")
public class PatrolRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long elderId;

    private Long visitorId;

    @Column(length = 50)
    private String visitorName;

    private LocalDate visitAt = LocalDate.now();

    @Column(length = 1000)
    private String content;

    @Column(length = 500)
    private String result;
}
