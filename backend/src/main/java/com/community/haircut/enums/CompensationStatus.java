package com.community.haircut.enums;

/**
 * 理发师空跑补偿认定结果。
 */
public enum CompensationStatus {
    PENDING,      // 待社区/财务认定
    COMMUNITY,    // 社区按规则补偿（公益资金支出）
    PAID,         // 社区补偿已发放
    BARBER_BEAR,  // 工具遗漏/消毒失责，理发师自行承担
    NONE          // 无需补偿
}
