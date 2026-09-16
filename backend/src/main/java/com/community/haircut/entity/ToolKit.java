package com.community.haircut.entity;

import com.community.haircut.enums.DisinfectionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tool_kit")
public class ToolKit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long barberId;

    @Column(nullable = false, length = 100)
    private String name;

    /** 工具清单：剪刀、推子、围布、消毒用品、服务包等 */
    @Column(length = 1000)
    private String items;

    private LocalDateTime disinfectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisinfectionStatus status = DisinfectionStatus.PENDING;

    private String notes;
}
