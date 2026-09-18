package com.community.haircut.enums;

/**
 * 上门前扫码核验发现的工具/服务包问题类型。
 */
public enum ToolIssueType {
    DISINFECTION_EXPIRED, // 消毒超过有效期
    SEAL_BROKEN,          // 封签破损
    TOWEL_DAMP,           // 毛巾围布受潮
    TOOL_STAINED,         // 剪刀剃刀有污渍
    TOOL_MISSING          // 工具遗漏
}
