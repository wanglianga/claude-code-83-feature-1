package com.community.haircut.entity;

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

    @Column(nullable = false)
    private Integer rescheduleCount = 0;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
