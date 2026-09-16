package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 理发师上门前工具确认单：工具、围布、消毒用品、服务包。
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

    /** 遗漏物品说明 */
    private String missingItems;

    private LocalDateTime confirmedAt = LocalDateTime.now();
}
