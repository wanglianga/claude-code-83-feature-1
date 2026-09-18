package com.community.haircut.enums;

/**
 * 感染风险来源：老人头癣/皮肤病、开放性伤口，或老人明确要求一次性用品。
 */
public enum InfectionRiskType {
    NONE,           // 无感染风险
    TINEA,          // 头癣
    SKIN_DISEASE,   // 皮肤病
    OPEN_WOUND,     // 开放性伤口
    DISPOSABLE_REQ  // 老人明确要求一次性用品
}
