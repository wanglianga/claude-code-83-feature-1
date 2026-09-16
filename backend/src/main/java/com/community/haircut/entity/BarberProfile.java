package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "barber_profile")
public class BarberProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    /** 技能标签，逗号分隔 */
    private String skillTags;

    /** 常驻服务楼栋，逗号分隔（用于楼栋距离匹配） */
    private String serviceBuildings;

    /** 信用分，初始 100 */
    @Column(nullable = false)
    private Integer creditScore = 100;

    @Column(nullable = false)
    private Integer completedCount = 0;

    @Column(nullable = false)
    private Integer cancelCount = 0;

    @Column(nullable = false)
    private Integer lateCount = 0;

    @Column(nullable = false)
    private Integer incidentCount = 0;

    @Column(nullable = false)
    private Boolean active = true;
}
