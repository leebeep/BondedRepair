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
// 货物类型（六类标记）选项与品名后缀（与规范 v1.4 / 样例数据、后端 markSuffix 一致）
export const materialMarkTypes = [
  { value: '1', label: '待维修货物', suffix: '待修复' },
  { value: '2', label: '已维修货物', suffix: '已修复' },
  { value: '3', label: '无法维修货物', suffix: '无法修复' },
  { value: '4', label: '维修用料件', suffix: '维修用保税料件' },
  { value: '5', label: '旧件或坏件', suffix: '维修替换下的旧件或坏件' },
  { value: '6', label: '边角料', suffix: '维修产生的边角料' },
  { value: '8', label: '维修服务费', suffix: '维修服务费' }
]
/** 物料名称展示：拼接货物类型标记；服务费直接拼“维修服务费”，其余用括号 */
export function materialNameWithMark(name, markType) {
  if (!name) return name
  const t = materialMarkTypes.find(t => t.value === markType)
  const suffix = t?.suffix
  if (!suffix) return name
  if (markType === '8') return name.endsWith(suffix) ? name : name + suffix
  return name.endsWith('（' + suffix + '）') ? name : name + '（' + suffix + '）'
}

// 业务状态（doc_status）：02/04 当前实现不会写入，03 为审核通过（申报中由海关状态体现）
export const docStatusMap = {
  '01': { label: '草稿', type: 'info' },
  '02': { label: '待审核', type: 'warning' },
  '03': { label: '审核通过', type: 'primary' },
  '05': { label: '申报通过', type: 'success' },
  '06': { label: '退单', type: 'danger' },
  '07': { label: '已作废', type: 'info' }
}
export const docStatusOptions = Object.entries(docStatusMap).map(([value, v]) => ({ value, label: v.label }))
const declaringCustomsStatus = ['10', '20', '21']
export function docStatusLabel(status, customsStatus) {
  if (status === '03' && declaringCustomsStatus.includes(customsStatus)) return '申报中'
  return docStatusMap[status]?.label || status || '-'
}
export function docStatusType(status, customsStatus) {
  if (status === '03' && declaringCustomsStatus.includes(customsStatus)) return 'warning'
  return docStatusMap[status]?.type || 'info'
}
