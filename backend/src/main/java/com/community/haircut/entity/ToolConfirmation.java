package com.community.haircut.entity;

import com.community.haircut.enums.InfectionRiskType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 理发师上门前工具确认单：扫码核验服务包封签、消毒日期、消毒方式、消毒柜编号、责任人，
 * 并逐项确认工具、围布、消毒用品、服务包；记录感染风险与工具用后处理。
 */
@Data
@Entity
@Table(name = "tool_confirmation")
public class ToolConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long barberId;

    // ---------------- 扫码核验的服务包信息快照 ----------------

    /** 实际核验使用的工具包/服务包 id */
    private Long kitId;

    /** 是否为备用服务包 */
    @Column(nullable = false)
    private Boolean backupKitUsed = false;

    /** 封签编号 */
    @Column(length = 40)
    private String sealNo;

    /** 封签是否完好 */
    @Column(nullable = false)
    private Boolean sealIntact = true;

    /** 消毒日期/时间 */
    private LocalDateTime disinfectedAt;

    /** 消毒方式 */
    @Column(length = 50)
    private String disinfectionMethod;

    /** 消毒柜编号 */
    @Column(length = 40)
    private String cabinetNo;

    /** 消毒责任人 */
    @Column(length = 50)
    private String responsiblePerson;

    /** 消毒是否在有效期内 */
    @Column(nullable = false)
    private Boolean disinfectionValid = true;

    // ---------------- 逐项外观检查 ----------------

    /** 理发工具齐备 */
    @Column(nullable = false)
    private Boolean toolsOk = false;

    /** 围布齐备 */
    @Column(nullable = false)
    private Boolean capeOk = false;

    /** 消毒用品齐备 */
    @Column(nullable = false)
    private Boolean disinfectantOk = false;

    /** 服务包齐备 */
    @Column(nullable = false)
    private Boolean packOk = false;

    /** 毛巾围布是否干燥（未受潮） */
    @Column(nullable = false)
    private Boolean towelDry = true;

    /** 剪刀剃刀是否无污渍 */
    @Column(nullable = false)
    private Boolean toolClean = true;

    /** 遗漏物品说明 */
    private String missingItems;

    // ---------------- 感染风险与用后处理 ----------------

    /** 感染风险类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InfectionRiskType infectionRisk = InfectionRiskType.NONE;

    /** 感染风险情况说明 */
    @Column(length = 500)
    private String infectionNote;

    /** 风险工具是否单独分装 */
    @Column(nullable = false)
    private Boolean separatelyPacked = false;

    /** 是否使用一次性用品 */
    @Column(nullable = false)
    private Boolean disposableUsed = false;

    /** 用后处理方式（就地封装消毒/带回集中消毒/按医废处理等） */
    @Column(length = 500)
    private String postHandling;

    /** 用后处理是否已记录 */
    @Column(nullable = false)
    private Boolean postHandlingRecorded = false;

    /** 本次核验是否全部通过（通过才允许开始服务） */
    @Column(nullable = false)
    private Boolean passed = false;

    private LocalDateTime confirmedAt = LocalDateTime.now();
}
