package com.community.haircut.enums;

/**
 * 感染事件处置状态。
 */
public enum InfectionStatus {
    OPEN,           // 已登记，待反查
    TRACING,        // 反查中（已列出同工具老人并通知）
    CONFIRMED,      // 已确认交叉感染或消毒责任
    RESOLVED        // 已结案（培训复检完成、补偿结果落定）
}
