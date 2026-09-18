package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 理发师上门前工具核验单：扫码核验服务包封签、消毒日期、消毒方式、消毒柜编号、责任人，
 * 并逐项检查工具、围布/毛巾、消毒用品是否齐备达标。
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

    /** 本次核验使用的工具包（主包/备用包） */
    private Long kitId;

    // ---------------- 扫码核验（服务包封签 + 消毒信息） ----------------

    /** 扫码读取的封签编号 */
    @Column(length = 60)
    private String sealCode;

    /** 封签是否完好 */
    @Column(nullable = false)
    private Boolean sealIntact = true;

    /** 消毒是否在有效期内 */
    @Column(nullable = false)
    private Boolean disinfectionValid = true;

    /** 消毒方式是否可查且合规 */
    @Column(nullable = false)
    private Boolean methodOk = true;

    /** 消毒柜编号是否可查 */
    @Column(nullable = false)
    private Boolean cabinetOk = true;

    /** 消毒责任人是否可查 */
    @Column(nullable = false)
    private Boolean responsibleOk = true;

    // ---------------- 逐项外观/齐备检查 ----------------

    /** 理发工具齐备（无遗漏） */
    @Column(nullable = false)
    private Boolean toolsOk = false;

    /** 剪刀/剃刀无污渍、清洁达标 */
    @Column(nullable = false)
    private Boolean toolsClean = true;

    /** 围布齐备 */
    @Column(nullable = false)
    private Boolean capeOk = false;

    /** 毛巾/围布干燥未受潮 */
    @Column(nullable = false)
    private Boolean clothDry = true;

    /** 消毒用品齐备 */
    @Column(nullable = false)
    private Boolean disinfectantOk = false;

    /** 服务包齐备 */
    @Column(nullable = false)
    private Boolean packOk = false;

    /** 不达标/遗漏项说明（消毒过期、封签破损、受潮、污渍、遗漏等） */
    @Column(length = 1000)
    private String missingItems;

    // ---------------- 感染风险处置 ----------------

    /** 服务单是否标记感染风险（头癣/皮肤病/开放性伤口/要求一次性用品） */
    @Column(nullable = false)
    private Boolean infectionRisk = false;

    /** 感染风险原因 */
    @Column(length = 500)
    private String infectionRiskReason;

    /** 工具是否单独分装 */
    @Column(nullable = false)
    private Boolean toolsSeparated = false;

    /** 用后处理方式（单独回收/医废处理/二次加强消毒等） */
    @Column(length = 500)
    private String postUseHandling;

    /** 是否使用一次性用品 */
    @Column(nullable = false)
    private Boolean disposableUsed = false;

    private LocalDateTime confirmedAt = LocalDateTime.now();
}
