// 枚举与中文标签映射
export const ROLE_LABELS = {
  ADMIN: '管理员',
  STAFF: '社区工作人员',
  BARBER: '理发师',
  VOLUNTEER: '志愿者',
  FAMILY: '家属',
  FINANCE: '财务',
  GRID: '网格员'
}

export const RISK_LABELS = { LOW: '低风险', MID: '中风险', HIGH: '高风险' }
export const RISK_TYPES = { LOW: 'success', MID: 'warning', HIGH: 'danger' }

export const SUBSIDY_LABELS = { NONE: '无补贴', PARTIAL: '部分补贴', FULL: '全额补贴' }

export const ORDER_TYPE_LABELS = { NORMAL: '普通预约', BIRTHDAY: '生日理发', EVENT: '社区活动', PATROL: '巡访理发' }

export const ORDER_STATUS_LABELS = {
  ASSIGNED: '待上门',
  TOOL_CONFIRMED: '工具已确认',
  ON_SITE: '已到达',
  IN_SERVICE: '服务中',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}
export const ORDER_STATUS_TYPES = {
  ASSIGNED: 'info',
  TOOL_CONFIRMED: 'primary',
  ON_SITE: 'warning',
  IN_SERVICE: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'danger'
}

export const PAYMENT_LABELS = { UNPAID: '待支付', PAID: '已支付', WAIVED: '无需自费', DISPUTED: '费用争议' }
export const PAYMENT_TYPES = { UNPAID: 'warning', PAID: 'success', WAIVED: 'info', DISPUTED: 'danger' }

export const DISINFECTION_LABELS = { DISINFECTED: '已消毒', PENDING: '待消毒', EXPIRED: '消毒过期' }
export const DISINFECTION_TYPES = { DISINFECTED: 'success', PENDING: 'warning', EXPIRED: 'danger' }

export const EXCEPTION_TYPE_LABELS = {
  ELDER_UNWELL: '老人临时不适',
  FAMILY_RESCHEDULE: '家属改约',
  BARBER_LATE: '理发师迟到',
  SKIN_CUT: '皮肤划伤',
  REFUSE_PAYMENT: '老人拒绝付款',
  SUBSIDY_CHANGE: '补贴资格变化',
  TOOL_MISSING: '工具遗漏',
  TOOL_DISINFECTION: '工具消毒失效',
  INFECTION_COMPLAINT: '交叉感染投诉'
}
export const EXCEPTION_STATUS_LABELS = { OPEN: '待处理', PROCESSING: '处理中', RESOLVED: '已解决' }
export const EXCEPTION_STATUS_TYPES = { OPEN: 'danger', PROCESSING: 'warning', RESOLVED: 'success' }

export const SUBSIDY_STATUS_LABELS = { PENDING: '待审核', APPROVED: '已发放', REJECTED: '已驳回' }
export const SUBSIDY_STATUS_TYPES = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }

export const FOLLOWUP_TYPE_LABELS = { SERVICE: '服务回访', EXCEPTION: '异常回访', CARE: '关怀回访' }
export const FOLLOWUP_STATUS_LABELS = { PENDING: '待回访', DONE: '已回访' }

export const CARE_SOURCE_LABELS = { NO_APPOINTMENT: '长期未预约', REPEATED_CANCEL: '连续取消', MANUAL: '手动创建' }
export const CARE_STATUS_LABELS = { PENDING: '待确认', CONFIRMED: '已确认', CLOSED: '已关闭' }

export const TIME_SLOTS = ['08:00-10:00', '09:00-11:00', '10:00-12:00', '14:00-16:00', '15:00-17:00', '16:00-18:00']

// 工具消毒安全与感染追溯
export const INFECTION_RISK_LABELS = {
  NONE: '无感染风险',
  TINEA: '头癣',
  SKIN_DISEASE: '皮肤病',
  OPEN_WOUND: '开放性伤口',
  DISPOSABLE_REQ: '老人要求一次性用品'
}
export const TOOL_ISSUE_LABELS = {
  DISINFECTION_EXPIRED: '消毒超过有效期',
  SEAL_BROKEN: '封签破损',
  TOWEL_DAMP: '毛巾围布受潮',
  TOOL_STAINED: '剪刀剃刀有污渍',
  TOOL_MISSING: '工具遗漏'
}
export const TOOL_ACTION_LABELS = { RESCHEDULE: '联系社区改约', BACKUP: '启用备用服务包', CANCEL: '无法服务取消' }
export const COMPENSATION_LABELS = {
  PENDING: '待社区认定',
  COMMUNITY: '社区补偿（待发放）',
  PAID: '社区补偿已发放',
  BARBER_BEAR: '理发师承担',
  NONE: '无需补偿'
}
export const COMPENSATION_TYPES = {
  PENDING: 'warning', COMMUNITY: 'primary', PAID: 'success', BARBER_BEAR: 'danger', NONE: 'info'
}
export const INFECTION_STATUS_LABELS = { OPEN: '待反查', TRACING: '反查中', CONFIRMED: '已确认责任', RESOLVED: '已结案' }
export const INFECTION_STATUS_TYPES = { OPEN: 'danger', TRACING: 'warning', CONFIRMED: 'danger', RESOLVED: 'success' }

export const EVENT_TYPE_LABELS = {
  CREATE: '创建',
  ASSIGN: '派单',
  TOOL_CONFIRM: '工具确认',
  CHECK_IN: '进门核对',
  START: '开始服务',
  COMPLETE: '完成服务',
  CANCEL: '取消',
  RESCHEDULE: '改约',
  EXCEPTION: '异常',
  RATE: '评价',
  PAYMENT: '费用',
  NOTE: '备注'
}
