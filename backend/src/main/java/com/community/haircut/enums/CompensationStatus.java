package com.community.haircut.enums;

/** 理发师空跑补偿认定状态 */
public enum CompensationStatus {
    PENDING,            // 待社区/财务认定
    COMMUNITY_APPROVED, // 社区规则认定，公益资金补偿
    BARBER_BORNE,       // 工具遗漏/消毒失责，理发师自行承担
    WAIVED              // 认定无需补偿
}
