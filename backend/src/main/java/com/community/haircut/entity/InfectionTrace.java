package com.community.haircut.entity;

import com.community.haircut.enums.InfectionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交叉感染追溯事件：
 * 已用未达标工具完成服务，或老人/家属后续反馈皮肤瘙痒、红疹、感染时，
 * 社区按理发师、服务包、工具编号和同日服务记录反查。
 */
@Data
@Entity
@Table(name = "infection_trace")
public class InfectionTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 触发追溯的服务单 */
    @Column(nullable = false)
    private Long sourceOrderId;

    @Column(nullable = false, length = 50)
    private String sourceElderName;

    @Column(nullable = false)
    private Long barberId;

    @Column(length = 50)
    private String barberName;

    /** 涉及服务包 id */
    private Long kitId;

    /** 涉及工具编号（剪刀/剃刀单件编号） */
    @Column(length = 40)
    private String toolItemNo;

    /** 涉及封签编号 */
    @Column(length = 40)
    private String sealNo;

    /** 反馈症状：皮肤瘙痒 / 红疹 / 感染等 */
    @Column(length = 500)
    private String symptom;

    /** 反馈说明 */
    @Column(length = 1000)
    private String description;

    /** 反馈人（老人本人/家属/社区排查） */
    @Column(length = 50)
    private String reporterName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InfectionStatus status = InfectionStatus.OPEN;

    /** 是否已通知同工具服务老人家属与志愿者 */
    @Column(nullable = false)
    private Boolean notified = false;

    /** 是否确认交叉感染 */
    @Column(nullable = false)
    private Boolean crossInfectionConfirmed = false;

    /** 是否确认消毒责任 */
    @Column(nullable = false)
    private Boolean disinfectionResponsibility = false;

    /** 是否已暂停理发师上门资格 */
    @Column(nullable = false)
    private Boolean barberSuspended = false;

    /** 消毒培训是否已安排/完成 */
    @Column(nullable = false)
    private Boolean trainingArranged = false;

    /** 工具复检是否已安排/通过 */
    @Column(nullable = false)
    private Boolean reinspectionArranged = false;

    /** 派单权重调整说明 */
    @Column(length = 200)
    private String weightAdjustNote;

    /** 处置结论（就医建议落实、补偿结果等） */
    @Column(length = 1000)
    private String resolution;

    private Long handledById;

    @Column(length = 50)
    private String handledByName;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;
}
