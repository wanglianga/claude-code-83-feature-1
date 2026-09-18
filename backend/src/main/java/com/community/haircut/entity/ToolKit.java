package com.community.haircut.entity;

import com.community.haircut.enums.DisinfectionStatus;
import com.community.haircut.enums.KitType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tool_kit")
public class ToolKit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 常规包绑定理发师；备用服务包归属社区，barberId 为空 */
    private Long barberId;

    @Column(nullable = false, length = 100)
    private String name;

    /** 工具清单：剪刀、推子、围布、消毒用品、服务包等 */
    @Column(length = 1000)
    private String items;

    /** 常规工具包 / 备用服务包 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KitType kitType = KitType.NORMAL;

    // ---------------- 消毒封签信息（扫码核验项） ----------------

    /** 服务包封签编号（一客一封签） */
    @Column(length = 40)
    private String sealNo;

    /** 封签是否完好 */
    @Column(nullable = false)
    private Boolean sealIntact = true;

    private LocalDateTime disinfectedAt;

    /** 消毒方式：紫外线消毒柜 / 高温蒸煮 / 含氯消毒剂浸泡等 */
    @Column(length = 50)
    private String disinfectionMethod;

    /** 消毒柜编号 */
    @Column(length = 40)
    private String cabinetNo;

    /** 消毒责任人 */
    @Column(length = 50)
    private String responsiblePerson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisinfectionStatus status = DisinfectionStatus.PENDING;

    /** 备用服务包使用后是否待补录消毒记录；未补录前不得再次派单 */
    @Column(nullable = false)
    private Boolean disinfectionPending = false;

    private String notes;
}
