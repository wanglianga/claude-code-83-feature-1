package com.community.haircut.entity;

import com.community.haircut.enums.CompensationStatus;
import com.community.haircut.enums.OrderStatus;
import com.community.haircut.enums.OrderType;
import com.community.haircut.enums.PaymentStatus;
import com.community.haircut.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 服务单：老人、家属、理发师、志愿者、社区、财务在同一服务单里协同处理。
 */
@Data
@Entity
@Table(name = "service_order")
public class ServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String orderNo;

    @Column(nullable = false)
    private Long elderId;

    @Column(nullable = false, length = 50)
    private String elderName;

    private Long barberId;

    @Column(length = 50)
    private String barberName;

    private Long volunteerId;

    @Column(length = 50)
    private String volunteerName;

    private Long createdById;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderType type = OrderType.NORMAL;

    /** 社区活动 id（type=EVENT 时） */
    private Long eventId;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    /** 时段，如 09:00-11:00 */
    @Column(nullable = false, length = 20)
    private String timeSlot;

    /** 上门地址快照 */
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.ASSIGNED;

    @Column(nullable = false)
    private Boolean needFamilyPresent = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private Boolean livingAlone = false;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subsidyAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal selfPayAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    private Integer satisfactionRating;

    @Column(length = 500)
    private String satisfactionComment;

    @Column(nullable = false)
    private Boolean hasException = false;

    private String cancelReason;

    /** 取消/改约来源：TOOL_ISSUE（工具问题，非老人违约不扣补贴）/ ELDER / FAMILY / BARBER_ISSUE */
    @Column(length = 20)
    private String cancelSource;

    // ---------------- 工具消毒与感染风险（工具消毒失效与交叉感染追溯） ----------------

    /** 实际使用的工具包（主包/备用包）快照，服务开始时锁定 */
    private Long usedKitId;

    @Column(length = 100)
    private String usedKitName;

    /** 使用的工具编号快照（剪刀、剃刀等，逗号分隔），用于交叉感染反查 */
    @Column(length = 500)
    private String usedToolItems;

    /** 实际消毒时间快照 */
    private LocalDateTime usedKitDisinfectedAt;

    /** 实际使用包是否消毒达标（封签/有效期/外观）；false 表示使用未达标工具完成服务 */
    @Column(nullable = false)
    private Boolean usedKitCompliant = true;

    /** 是否标记感染风险（老人头癣/皮肤病/开放性伤口/明确要求一次性用品） */
    @Column(nullable = false)
    private Boolean infectionRisk = false;

    @Column(length = 500)
    private String infectionRiskReason;

    /** 工具单独分装 + 用后处理记录 */
    @Column(length = 500)
    private String postUseHandling;

    /** 是否启用了社区备用服务包 */
    @Column(nullable = false)
    private Boolean spareUsed = false;

    /** 工具问题导致空跑（未服务）；由社区规则认定补偿 */
    @Column(nullable = false)
    private Boolean emptyRun = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CompensationStatus compensationStatus;

    /** 空跑补偿金额 */
    @Column(precision = 10, scale = 2)
    private BigDecimal compensationAmount;

    private String compensationNote;

    @Column(nullable = false)
    private Integer rescheduleCount = 0;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
