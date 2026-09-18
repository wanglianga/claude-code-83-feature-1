package com.community.haircut.enums;

/** 交叉感染追溯单状态 */
public enum InfectionCaseStatus {
    OPEN,       // 已接报，待反查/通知
    NOTIFIED,   // 已通知同工具服务老人家属与志愿者，随访中
    CONFIRMED,  // 确认交叉感染或消毒责任，已处置
    RULED_OUT   // 已排除
}
