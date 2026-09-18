package com.community.haircut.enums;

/**
 * 核验不通过时的处置动作：联系社区改约 / 启用备用服务包 / 无法服务取消。
 */
public enum ToolIssueAction {
    RESCHEDULE, // 联系社区改约
    BACKUP,     // 启用备用服务包
    CANCEL      // 无法继续，取消（不算老人违约）
}
