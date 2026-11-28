import request from '@/utils/request'

// 入库单
export const listInbound = q => request({ url: '/bonded/inbound/list', method: 'get', params: q })
export const getInbound = id => request({ url: '/bonded/inbound/' + id, method: 'get' })
export const addInbound = d => request({ url: '/bonded/inbound', method: 'post', data: d })
export const updInbound = d => request({ url: '/bonded/inbound', method: 'put', data: d })
export const delInbound = id => request({ url: '/bonded/inbound/' + id, method: 'delete' })
export const auditInbound = id => request({ url: '/bonded/inbound/audit/' + id, method: 'post' })
export const declareInbound = (t, ids) => request({ url: `/bonded/inbound/declare/${t}/${ids}`, method: 'post' })
export const nextInboundNo = () => request({ url: '/bonded/inbound/nextNo', method: 'get' })

// 工单发料
export const listIssue = q => request({ url: '/bonded/issue/list', method: 'get', params: q })
export const getIssue = id => request({ url: '/bonded/issue/' + id, method: 'get' })
export const addIssue = d => request({ url: '/bonded/issue', method: 'post', data: d })
export const updIssue = d => request({ url: '/bonded/issue', method: 'put', data: d })
export const delIssue = id => request({ url: '/bonded/issue/' + id, method: 'delete' })
export const auditIssue = id => request({ url: '/bonded/issue/audit/' + id, method: 'post' })
export const declareIssue = (t, ids) => request({ url: `/bonded/issue/declare/${t}/${ids}`, method: 'post' })
export const nextIssueNo = () => request({ url: '/bonded/issue/nextNo', method: 'get' })

// 工单收料
export const listReceive = q => request({ url: '/bonded/receive/list', method: 'get', params: q })
export const getReceive = id => request({ url: '/bonded/receive/' + id, method: 'get' })
export const addReceive = d => request({ url: '/bonded/receive', method: 'post', data: d })
export const updReceive = d => request({ url: '/bonded/receive', method: 'put', data: d })
export const delReceive = id => request({ url: '/bonded/receive/' + id, method: 'delete' })
export const auditReceive = id => request({ url: '/bonded/receive/audit/' + id, method: 'post' })
export const declareReceive = (t, ids) => request({ url: `/bonded/receive/declare/${t}/${ids}`, method: 'post' })
export const nextReceiveNo = () => request({ url: '/bonded/receive/nextNo', method: 'get' })
export const queryWorkOrder = workNo => request({ url: '/bonded/receive/workOrder/' + encodeURIComponent(workNo), method: 'get' })

// 出库单
export const listOutbound = q => request({ url: '/bonded/outbound/list', method: 'get', params: q })
export const getOutbound = id => request({ url: '/bonded/outbound/' + id, method: 'get' })
export const addOutbound = d => request({ url: '/bonded/outbound', method: 'post', data: d })
export const updOutbound = d => request({ url: '/bonded/outbound', method: 'put', data: d })
export const delOutbound = id => request({ url: '/bonded/outbound/' + id, method: 'delete' })
export const auditOutbound = id => request({ url: '/bonded/outbound/audit/' + id, method: 'post' })
export const declareOutbound = (t, ids) => request({ url: `/bonded/outbound/declare/${t}/${ids}`, method: 'post' })
export const nextOutboundNo = () => request({ url: '/bonded/outbound/nextNo', method: 'get' })

// 库存列表（每物料一行的结存汇总）
export const listStock = q => request({ url: '/bonded/stock/list', method: 'get', params: q })

// 物料台账流水（库存详情页"更新记录"）
export const listStockLedger = q => request({ url: '/bonded/stock/ledger', method: 'get', params: q })

// 物料库存详情（物料编号唯一）
export const getMaterialStock = id => request({ url: '/bonded/stock/material/' + id, method: 'get' })

// 联网日志（原申报管理）
export const declareBoard = () => request({ url: '/bonded/declare/board', method: 'get' })
export const listDeclareLog = q => request({ url: '/bonded/declare/log', method: 'get', params: q })
export const getPushSwitch = () => request({ url: '/bonded/declare/push-switch', method: 'get' })
export const setPushSwitch = enabled => request({ url: '/bonded/declare/push-switch/' + enabled, method: 'put' })
export const pushPending = () => request({ url: '/bonded/declare/push-pending', method: 'post' })
export const queryReceipt = () => request({ url: '/bonded/declare/query-receipt', method: 'post' })
export const queryStatus = () => request({ url: '/bonded/declare/query-status', method: 'get' })
export const syncBoard = () => request({ url: '/bonded/declare/sync-board', method: 'get' })

// 首页工作台统计
export const dashboardStats = () => request({ url: '/bonded/dashboard/stats', method: 'get' })
