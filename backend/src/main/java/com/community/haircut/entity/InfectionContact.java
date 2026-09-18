package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交叉感染同工具接触者：反查出的、使用同一服务包/工具接受服务的其他老人。
 * 通知家属和志愿者，建议就医并记录结果。
 */
@Data
@Entity
@Table(name = "infection_contact")
public class InfectionContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long traceId;

    /** 同日/同工具的历史服务单 */
    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long elderId;

    @Column(nullable = false, length = 50)
    private String elderName;

    private Long familyUserId;

    @Column(length = 50)
    private String familyContactName;

    private Long volunteerId;

    @Column(length = 50)
    private String volunteerName;

    /** 服务时间 */
    private LocalDateTime servedAt;

    /** 使用的服务包 id */
    private Long kitId;

    /** 是否已通知家属 */
    @Column(nullable = false)
    private Boolean familyNotified = false;

    /** 是否已通知志愿者 */
    @Column(nullable = false)
    private Boolean volunteerNotified = false;

    /** 是否已建议就医 */
    @Column(nullable = false)
    private Boolean medicalAdvised = false;

    /** 就医/排查结果：无异常、皮疹、确诊感染等 */
    @Column(length = 500)
    private String medicalResult;

    /** 回访记录 */
    @Column(length = 1000)
    private String followUpNote;

    private LocalDateTime notifiedAt;

    private LocalDateTime resultRecordedAt;
}
