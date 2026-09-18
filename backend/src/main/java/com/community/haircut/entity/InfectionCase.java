package com.community.haircut.entity;

import com.community.haircut.enums.InfectionCaseStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交叉感染追溯单：接报老人/家属皮肤瘙痒、红疹、感染反馈，或发现已用未达标工具完成服务后，
 * 社区按理发师、服务包、工具编号和同日服务记录反查，并记录通知、就医建议与处置结果。
 */
@Data
@Entity
@Table(name = "infection_case")
public class InfectionCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String caseNo;

    /** 触发追溯的服务单 */
    @Column(nullable = false)
    private Long triggerOrderId;

    @Column(nullable = false)
    private Long elderId;

    @Column(nullable = false, length = 50)
    private String elderName;

    @Column(nullable = false)
    private Long barberId;

    @Column(nullable = false, length = 50)
    private String barberName;

    /** 涉事服务包 */
    private Long kitId;

    @Column(length = 100)
    private String kitName;

    /** 涉事工具编号快照 */
    @Column(length = 500)
    private String toolItems;

    /** 反馈症状：皮肤瘙痒 / 红疹 / 感染 / 未达标工具 */
    @Column(length = 200)
    private String symptom;

    /** 反查服务日期（默认触发单同日） */
    @Column(nullable = false)
    private LocalDate exposureDate;

    @Column(length = 1000)
    private String description;

    private Long reportedById;

    @Column(length = 50)
    private String reportedByName;

    @Column(length = 20)
    private String reportedByRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InfectionCaseStatus status = InfectionCaseStatus.OPEN;

    /** 是否确认交叉感染 */
    @Column(nullable = false)
    private Boolean crossInfectionConfirmed = false;

    /** 是否确认消毒责任 */
    @Column(nullable = false)
    private Boolean disinfectionResponsible = false;

    /** 处置：暂停上门资格 */
    @Column(nullable = false)
    private Boolean barberSuspended = false;

    /** 处置：安排消毒培训 */
    @Column(nullable = false)
    private Boolean trainingRequired = false;

    /** 处置：安排工具复检 */
    @Column(nullable = false)
    private Boolean recheckRequired = false;

    /** 处置：派单权重下调幅度 */
    private Integer weightDeduction;

    @Column(length = 1000)
    private String conclusion;

    private Long concludedById;

    @Column(length = 50)
    private String concludedByName;

    private LocalDateTime concludedAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
