package com.community.haircut.enums;

public enum ExceptionType {
    ELDER_UNWELL,              // 老人临时不适
    FAMILY_RESCHEDULE,         // 家属改约
    BARBER_LATE,               // 理发师迟到
    SKIN_CUT,                  // 服务中皮肤划伤
    REFUSE_PAYMENT,            // 老人拒绝付款
    SUBSIDY_CHANGE,            // 补贴资格变化
    TOOL_MISSING,              // 工具遗漏
    TOOL_DISINFECTION_EXPIRED, // 消毒超过有效期
    TOOL_SEAL_BROKEN,          // 服务包封签破损
    TOOL_DAMP,                 // 毛巾/围布受潮
    TOOL_STAINED,              // 剪刀/剃刀有污渍
    TOOL_NONCOMPLIANT_USE,     // 已用未达标工具完成服务
    INFECTION_FEEDBACK         // 服务后皮肤瘙痒/红疹/感染反馈
}
