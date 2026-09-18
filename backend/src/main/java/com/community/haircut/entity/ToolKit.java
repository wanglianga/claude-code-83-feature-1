package com.community.haircut.entity;

import com.community.haircut.enums.DisinfectionMethod;
import com.community.haircut.enums.DisinfectionStatus;
import com.community.haircut.enums.KitType;
import com.community.haircut.enums.SealStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 理发工具包/上门服务包：记录封签编号、消毒日期、消毒方式、消毒柜编号、责任人。
 * 每位理发师可有一个主工具包（MAIN）和若干上门备用服务包（SPARE）。
 */
@Data
@Entity
@Table(name = "tool_kit")
public class ToolKit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long barberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private KitType kitType = KitType.MAIN;

    @Column(nullable = false, length = 100)
    private String name;

    /** 工具清单：剪刀、推子、围布、毛巾、消毒喷雾、一次性用品等 */
    @Column(length = 1000)
    private String items;

    /** 服务包封签编号（上门前扫码核验） */
    @Column(length = 60)
    private String sealCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SealStatus sealStatus = SealStatus.INTACT;

    private LocalDateTime disinfectedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DisinfectionMethod disinfectionMethod;

    /** 消毒柜编号 */
    @Column(length = 40)
    private String cabinetNo;

    /** 消毒责任人 */
    @Column(length = 50)
    private String responsiblePerson;

    /** 消毒有效时长（小时），默认 48 */
    @Column(nullable = false)
    private Integer validHours = 48;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisinfectionStatus status = DisinfectionStatus.PENDING;

    /** 最近一次使用该包的服务单（备用包使用后须补录消毒才能再次派单） */
    private Long lastUsedOrderId;

    private LocalDateTime lastUsedAt;

    private String notes;

    private LocalDateTime createdAt = LocalDateTime.now();
}
