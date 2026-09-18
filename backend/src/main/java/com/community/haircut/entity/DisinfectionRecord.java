package com.community.haircut.entity;

import com.community.haircut.enums.DisinfectionMethod;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工具/服务包消毒记录：消毒日期、消毒方式、消毒柜编号、责任人。
 * 备用服务包使用后必须补录一条消毒记录，未补录前不得再次派单/启用。
 */
@Data
@Entity
@Table(name = "disinfection_record")
public class DisinfectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long kitId;

    @Column(nullable = false)
    private Long barberId;

    @Column(nullable = false)
    private LocalDateTime disinfectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisinfectionMethod method;

    /** 消毒柜编号 */
    @Column(length = 40)
    private String cabinetNo;

    /** 消毒责任人 */
    @Column(length = 50)
    private String responsiblePerson;

    /** 消毒后新封签编号 */
    @Column(length = 60)
    private String sealCode;

    /** 有效时长（小时） */
    @Column(nullable = false)
    private Integer validHours = 48;

    /** 是否为使用后补录（备用包回站补录） */
    @Column(nullable = false)
    private Boolean supplementary = false;

    /** 关联服务单（补录场景） */
    private Long orderId;

    private Long operatorId;

    @Column(length = 50)
    private String operatorName;

    @Column(length = 500)
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
