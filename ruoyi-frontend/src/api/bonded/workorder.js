import request from '@/utils/request'

// 维修工单
export const listWorkOrder = q => request({ url: '/bonded/workorder/list', method: 'get', params: q })
export const getWorkOrder = id => request({ url: '/bonded/workorder/' + id, method: 'get' })
export const addWorkOrder = d => request({ url: '/bonded/workorder', method: 'post', data: d })
export const updWorkOrder = d => request({ url: '/bonded/workorder', method: 'put', data: d })
export const delWorkOrder = id => request({ url: '/bonded/workorder/' + id, method: 'delete' })
export const woOptions = () => request({ url: '/bonded/workorder/options', method: 'get' })
export const listWorkOrderOptions = woOptions
export const nextWorkOrderNo = () => request({ url: '/bonded/workorder/nextNo', method: 'get' })
export const closeWorkOrder = id => request({ url: '/bonded/workorder/close/' + id, method: 'post' })
