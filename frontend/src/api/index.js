import request from './request'

// 认证
export const login = (data) => request.post('/auth/login', data)
export const getProfile = () => request.get('/auth/profile')

// 老人档案
export const listElders = (params) => request.get('/elders', { params })
export const getElder = (id) => request.get(`/elders/${id}`)
export const createElder = (data) => request.post('/elders', data)
export const updateElder = (id, data) => request.put(`/elders/${id}`, data)
export const elderOrders = (id) => request.get(`/elders/${id}/orders`)
export const upcomingBirthdays = (days = 7) => request.get('/elders/birthdays', { params: { days } })
export const patrolList = () => request.get('/elders/patrol')
export const recordPatrol = (id, data) => request.post(`/elders/${id}/patrol`, data)

// 服务单
export const createOrder = (data) => request.post('/orders', data)
export const listOrders = (params) => request.get('/orders', { params })
export const orderDetail = (id) => request.get(`/orders/${id}`)
export const confirmTools = (id, data) => request.post(`/orders/${id}/confirm-tools`, data)
export const activateSpareKit = (id, data) => request.post(`/orders/${id}/activate-spare`, data)
export const rescheduleToolIssue = (id, data) => request.post(`/orders/${id}/reschedule-tool-issue`, data)
export const cancelToolIssue = (id, data) => request.post(`/orders/${id}/cancel-tool-issue`, data)
export const adjudicateCompensation = (id, data) => request.post(`/orders/${id}/compensation`, data)
export const checkIn = (id, data) => request.post(`/orders/${id}/check-in`, data)
export const startService = (id) => request.post(`/orders/${id}/start`)
export const completeOrder = (id, data) => request.post(`/orders/${id}/complete`, data)
export const cancelOrder = (id, data) => request.post(`/orders/${id}/cancel`, data)
export const rescheduleOrder = (id, data) => request.post(`/orders/${id}/reschedule`, data)
export const rateOrder = (id, data) => request.post(`/orders/${id}/rate`, data)
export const confirmPayment = (id) => request.post(`/orders/${id}/confirm-payment`)

// 理发师 / 排班 / 工具包
export const listBarbers = () => request.get('/barbers')
export const listVolunteers = () => request.get('/barbers/volunteers')
export const updateBarberProfile = (userId, data) => request.put(`/barbers/${userId}/profile`, data)
export const adjustCredit = (userId, data) => request.post(`/barbers/${userId}/credit`, data)
export const listSchedules = (params) => request.get('/barbers/schedules', { params })
export const addSchedule = (data) => request.post('/barbers/schedules', data)
export const deleteSchedule = (id) => request.delete(`/barbers/schedules/${id}`)
export const getToolKit = (barberId) => request.get(`/barbers/${barberId}/toolkit`)
export const listKits = (barberId) => request.get(`/barbers/${barberId}/kits`)
export const scanSeal = (barberId, sealCode) => request.get(`/barbers/${barberId}/scan`, { params: { sealCode } })
export const saveToolKit = (barberId, data) => request.put(`/barbers/${barberId}/toolkit`, data)
export const disinfectToolKit = (barberId) => request.post(`/barbers/${barberId}/toolkit/disinfect`)
export const createSpareKit = (barberId, data) => request.post(`/barbers/${barberId}/spare-kits`, data)
export const markSealBroken = (kitId) => request.post(`/barbers/kits/${kitId}/seal-broken`)
export const listDisinfections = (params) => request.get('/barbers/disinfections', { params })
export const recordDisinfection = (data) => request.post('/barbers/disinfections', data)
export const completeTraining = (userId, data) => request.post(`/barbers/${userId}/training`, data || {})
export const recheckTools = (userId, data) => request.post(`/barbers/${userId}/recheck`, data)
export const reinstateBarber = (userId) => request.post(`/barbers/${userId}/reinstate`)

// 交叉感染追溯
export const listInfectionCases = (params) => request.get('/infection-cases', { params })
export const infectionCaseDetail = (id) => request.get(`/infection-cases/${id}`)
export const createInfectionCase = (data) => request.post('/infection-cases', data)
export const notifyInfectionContacts = (id) => request.post(`/infection-cases/${id}/notify`)
export const recordContactResult = (contactId, data) => request.post(`/infection-cases/contacts/${contactId}/result`, data)
export const concludeInfectionCase = (id, data) => request.post(`/infection-cases/${id}/conclude`, data)
export const statsInfectionReview = () => request.get('/stats/infection-review')

// 异常
export const reportException = (data) => request.post('/exceptions', data)
export const listExceptions = (params) => request.get('/exceptions', { params })
export const listOrderExceptions = (orderId) => request.get(`/exceptions/order/${orderId}`)
export const handleException = (id) => request.post(`/exceptions/${id}/handle`)
export const resolveException = (id, data) => request.post(`/exceptions/${id}/resolve`, data)

// 财务
export const listSubsidies = (params) => request.get('/finance/subsidies', { params })
export const reviewSubsidy = (id, data) => request.post(`/finance/subsidies/${id}/review`, data)

// 回访
export const listFollowUps = (params) => request.get('/follow-ups', { params })
export const createFollowUp = (data) => request.post('/follow-ups', data)
export const completeFollowUp = (id, data) => request.post(`/follow-ups/${id}/complete`, data)

// 关怀任务
export const listCareTasks = (params) => request.get('/care-tasks', { params })
export const scanCareTasks = () => request.post('/care-tasks/scan')
export const createCareTask = (data) => request.post('/care-tasks', data)
export const confirmCareTask = (id, data) => request.post(`/care-tasks/${id}/confirm`, data)

// 社区活动
export const listEvents = () => request.get('/events')
export const createEvent = (data) => request.post('/events', data)
export const updateEventStatus = (id, status) => request.post(`/events/${id}/status`, null, { params: { status } })
export const bookEvent = (id, data) => request.post(`/events/${id}/book`, data)

// 统计
export const statsDashboard = () => request.get('/stats/dashboard')
export const statsCoverage = () => request.get('/stats/coverage')
export const statsBarbers = () => request.get('/stats/barbers')
export const statsFunds = () => request.get('/stats/funds')
export const statsAlerts = () => request.get('/stats/alerts')

// 通知
export const listNotifications = () => request.get('/notifications')
export const unreadCount = () => request.get('/notifications/unread-count')
export const markRead = (id) => request.post(`/notifications/${id}/read`)
export const readAll = () => request.post('/notifications/read-all')

// 用户管理
export const listUsers = () => request.get('/users')
export const createUser = (data) => request.post('/users', data)
export const changeUserStatus = (id, status) => request.post(`/users/${id}/status`, null, { params: { status } })
export const resetPassword = (id, data) => request.post(`/users/${id}/reset-password`, data)
