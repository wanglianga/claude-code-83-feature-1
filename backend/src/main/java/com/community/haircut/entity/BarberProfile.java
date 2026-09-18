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

    /** 交叉感染/消毒责任投诉次数 */
    @Column(nullable = false)
    private Integer infectionCount = 0;

    /** 派单权重，默认 1.0；确认消毒责任后下调（如 0.5），暂停上门资格时为 0 */
    @Column(nullable = false)
    private Double dispatchWeight = 1.0;

    /** 是否暂停上门资格（确认交叉感染或消毒责任） */
    @Column(nullable = false)
    private Boolean visitSuspended = false;

    /** 暂停原因 */
    @Column(length = 500)
    private String suspendReason;

    /** 消毒培训是否完成（暂停后复岗条件之一） */
    @Column(nullable = false)
    private Boolean trainingPassed = true;

    /** 工具复检是否通过（暂停后复岗条件之一） */
    @Column(nullable = false)
    private Boolean toolReinspected = true;

    @Column(nullable = false)
    private Boolean active = true;
}
