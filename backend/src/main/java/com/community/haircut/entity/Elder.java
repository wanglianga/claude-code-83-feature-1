package com.community.haircut.entity;

import com.community.haircut.enums.RiskLevel;
import com.community.haircut.enums.SubsidyType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "elder")
public class Elder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 18)
    private String idCard;

    @Column(length = 10)
    private String gender;

    private LocalDate birthDate;

    private String phone;

    /** 所属社区 */
    private String community;

    /** 楼栋，如 3栋 */
    @Column(length = 20)
    private String building;

    @Column(length = 20)
    private String unit;

    @Column(length = 20)
    private String room;

    /** 完整住址快照 */
    private String address;

    /** 楼栋距社区服务点距离（米），用于派单距离因子 */
    private Integer buildingDistance = 0;

    /** 是否独居 */
    @Column(nullable = false)
    private Boolean livingAlone = false;

    /** 身体状况描述 */
    @Column(length = 1000)
    private String healthCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RiskLevel riskLevel = RiskLevel.LOW;

    /** 行动能力：自如/拄拐/轮椅/卧床 */
    @Column(length = 20)
    private String mobility;

    /** 服务时是否需要家属在场 */
    @Column(nullable = false)
    private Boolean needFamilyPresent = false;

    /** 理发偏好 */
    @Column(length = 500)
    private String hairPreference;

    /** 上门时间偏好 */
    private String preferredTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SubsidyType subsidyType = SubsidyType.NONE;

    private String familyContactName;

    private String familyContactPhone;

    /** 关联家属账号 */
    private Long familyUserId;

    /** 是否纳入定期巡访名单 */
    @Column(nullable = false)
    private Boolean patrolEnabled = false;

    /** 巡访间隔天数 */
    private Integer patrolIntervalDays = 30;

    private LocalDate lastPatrolAt;

    @Column(length = 1000)
    private String notes;

    /** ACTIVE / DISABLED */
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    private Long createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
