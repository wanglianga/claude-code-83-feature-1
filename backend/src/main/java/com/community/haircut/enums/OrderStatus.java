package com.community.haircut.enums;

public enum OrderStatus {
    ASSIGNED,        // 已派单，待理发师确认工具
    TOOL_CONFIRMED,  // 工具已确认，待上门
    ON_SITE,         // 志愿者已进门核对，待开始服务
    IN_SERVICE,      // 服务中
    COMPLETED,       // 已完成
    CANCELLED        // 已取消
}
