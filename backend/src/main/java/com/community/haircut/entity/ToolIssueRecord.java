package com.community.haircut.entity;

import com.community.haircut.enums.CompensationStatus;
import com.community.haircut.enums.ToolIssueAction;
import com.community.haircut.enums.ToolIssueType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 上门前扫码核验不通过的处置记录：
 * 联系社区改约 / 启用备用服务包 / 取消；并认定理发师空跑补偿责任。
 */
@Data
@Entity
@Table(name = "tool_issue_record")
public class ToolIssueRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long barberId;

    /** 问题工具包（常规包或备用包） */
    private Long kitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ToolIssueType issueType;

    /** 问题详情（可多选问题时的补充说明） */
    @Column(length = 1000)
    private String issueDetail;

    /** 处置动作 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ToolIssueAction action;

    /** 启用的备用服务包 id（action=BACKUP 时） */
    private Long backupKitId;

    /** 改约后的日期 */
    private LocalDateTime handledAt = LocalDateTime.now();

    /** 空跑补偿认定 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompensationStatus compensationStatus = CompensationStatus.PENDING;

    /** 补偿金额（社区规则补偿时由公益资金支出） */
    @Column(precision = 10, scale = 2)
    private BigDecimal compensationAmount = BigDecimal.ZERO;

    /** 补偿/责任认定说明 */
    @Column(length = 1000)
    private String compensationNote;

    private Long handledById;

    @Column(length = 50)
    private String handledByName;
}
