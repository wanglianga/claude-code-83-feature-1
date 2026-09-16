package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "barber_schedule")
public class BarberSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long barberId;

    @Column(nullable = false)
    private LocalDate workDate;

    /** 格式 HH:mm，如 08:00 */
    @Column(nullable = false, length = 5)
    private String startTime;

    @Column(nullable = false, length = 5)
    private String endTime;

    @Column(nullable = false)
    private Integer maxOrders = 4;

    /** ACTIVE / OFF */
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}
