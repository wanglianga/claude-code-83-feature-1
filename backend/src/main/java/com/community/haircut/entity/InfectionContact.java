package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交叉感染反查接触者：同一理发师、同一服务包/工具编号、同日服务的其他老人。
 * 记录家属与志愿者通知、就医建议和随访结果。
 */
@Data
@Entity
@Table(name = "infection_contact")
public class InfectionContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long caseId;

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

    /** 命中的共用工具编号 */
    @Column(length = 500)
    private String sharedTools;

    /** 是否已通知家属 */
    @Column(nullable = false)
    private Boolean familyNotified = false;

    /** 是否已通知志愿者 */
    @Column(nullable = false)
    private Boolean volunteerNotified = false;

    private LocalDateTime notifiedAt;

    /** 是否已建议就医 */
    @Column(nullable = false)
    private Boolean medicalAdvised = false;

    /** 就医/随访结果 */
    @Column(length = 1000)
    private String followUpResult;

    /** 是否确诊感染 */
    @Column(nullable = false)
    private Boolean confirmedInfected = false;

    private LocalDateTime updatedAt = LocalDateTime.now();
}
