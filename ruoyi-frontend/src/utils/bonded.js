// 保税维修公共展示工具：海关状态映射（PRD §4.2）
export const customsStatusMap = {
  '00': { label: '未申报', type: 'info' },
  '10': { label: '待采集', type: 'primary' },
  '20': { label: '已采集', type: 'primary' },
  '90': { label: '采集超时', type: 'danger' },
  '21': { label: '申报中', type: 'warning' },
  '2Y': { label: '入库成功', type: 'success' },
  '2N': { label: '入库失败', type: 'danger' },
  '22': { label: '通过', type: 'success' },
  '23': { label: '转人工', type: 'warning' },
  '24': { label: '退单', type: 'danger' },
  '29': { label: '已作废', type: 'info' }
}
export function cstLabel(s) { return customsStatusMap[s]?.label || s || '-' }
export function cstType(s) { return customsStatusMap[s]?.type || 'info' }
export const returnTypes = [
  { value: '1', label: '1-未使用料件' },
  { value: '5', label: '5-已维修货物' },
  { value: '6', label: '6-维修用料件' },
  { value: '7', label: '7-坏损零部件' },
  { value: '8', label: '8-维修服务费(自动生成)' },
  { value: '9', label: '9-边角料' },
  { value: 'a', label: 'a-残次品' }
]
// 出库业务类型（SDO2 busiType）
export const outboundBusiTypes = [
  { value: '03', label: '03-销毁' },
  { value: '04', label: '04-复运出境' },
  { value: '05', label: '05-复运回境内区外' },
  { value: '99', label: '99-其他' }
]
// 申报报文类型
export const declareBizTypes = [
  { value: 'EPD101', label: '料件成品备案' },
  { value: 'PTO2', label: '入库清单' },
  { value: 'RRD2', label: '工单发料' },
  { value: 'MSO2', label: '工单收料' },
  { value: 'SDO2', label: '出库清单' }
]
export const declareResultMap = {
  PENDING: { label: '待回执', type: 'primary' },
  Y: { label: '入库成功', type: 'success' },
  N: { label: '入库失败', type: 'danger' },
  '1': { label: '通过', type: 'success' },
  '2': { label: '转人工', type: 'warning' },
  '3': { label: '退单', type: 'danger' }
}
export function declResultLabel(s) { return declareResultMap[s]?.label || s || '-' }
export function declResultType(s) { return declareResultMap[s]?.type || 'info' }
