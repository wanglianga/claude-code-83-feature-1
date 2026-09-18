package com.community.haircut.enums;

public enum ExceptionType {
    ELDER_UNWELL,       // 老人临时不适
    FAMILY_RESCHEDULE,  // 家属改约
    BARBER_LATE,        // 理发师迟到
    SKIN_CUT,           // 服务中皮肤划伤
    REFUSE_PAYMENT,     // 老人拒绝付款
    SUBSIDY_CHANGE,     // 补贴资格变化
    TOOL_MISSING,       // 工具遗漏
    TOOL_DISINFECTION,  // 工具消毒失效（过期/封签破损/受潮/污渍）
    INFECTION_COMPLAINT // 交叉感染投诉（瘙痒/红疹/感染反馈）
}
