package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

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

    /** 交叉感染/消毒责任事故数 */
    @Column(nullable = false)
    private Integer infectionCount = 0;

    /** 派单权重（0-100，默认 100；确认消毒责任后下调） */
    @Column(nullable = false)
    private Integer dispatchWeight = 100;

    /** 是否暂停上门资格（确认交叉感染或消毒责任后暂停，复检通过前不可派单） */
    @Column(nullable = false)
    private Boolean suspended = false;

    private String suspendReason;

    private LocalDateTime suspendedAt;

    /** 消毒培训是否完成 */
    @Column(nullable = false)
    private Boolean disinfectionTrained = false;

    private LocalDateTime trainedAt;

    /** 工具复检是否通过 */
    @Column(nullable = false)
    private Boolean toolRechecked = false;

    private LocalDateTime recheckedAt;

    @Column(nullable = false)
    private Boolean active = true;
}
