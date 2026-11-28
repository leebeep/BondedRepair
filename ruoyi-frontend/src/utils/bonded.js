// ============================================================
// 保税维修公共展示工具（字段与展示规范）
// 1. 数量字段按单据语义命名：入库 storageCnt / 发料 issueQty /
//    收料 materialNum / 出库 deliveryCnt；列名对应「数量/发料数量/数量/出库数量」
// 2. 发料单不记币制（后端 BrIssueItem 无 currency 字段），其余单据金额列
//    统一「单价 | 币制 | 总价」顺序并用 fmtThousand 千分位展示
// 3. 空值规范：所有文本/数值列空值统一显示 '-'
// 4. 时间列规范：列表用 fmtTimeShort（MM-dd HH:mm）+ tooltip 全量（fmtTimeFull）
// 5. 表格密度：页面级表格标准密度，弹窗/嵌套子表用 size="small" 紧凑密度
// 6. 物料档案统一经 composables/useMaterialCache 读取，禁止页面自行 listMaterial
//    （出库表单的 markType='8' 服务费虚物料为例外）
// ============================================================

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
// 货物类型（六类标记）选项（标签仅用于类型筛选/展示；物料名称按字面原样存取，不拼接标记后缀）
export const materialMarkTypes = [
  { value: '1', label: '待维修货物' },
  { value: '2', label: '已维修货物' },
  { value: '3', label: '无法维修货物' },
  { value: '4', label: '维修用料件' },
  { value: '5', label: '旧件或坏件' },
  { value: '6', label: '边角料' },
  { value: '8', label: '维修服务费' }
]

// 业务状态（doc_status）：02待审核/04已申报 为历史遗留死状态，已移除；
// 03=审核通过（已生效扣账并推送申报，"申报中"由海关状态10/20/21联动显示）
export const docStatusMap = {
  '01': { label: '草稿', type: 'info' },
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

/**
 * 申报执行策略联动：返回单据当前允许的执行策略数组（'1'备案/'2'变更/'3'作废）
 * 规则：已申报过（有预录入编号）只能变更/作废，不允许再次备案；
 * 申报在途（03+10/20/21）与已作废（07）一律禁止申报
 */
export function allowedExecTypes(row) {
  if (!row) return []
  const ds = row.docStatus
  const cs = row.customsStatus
  if (ds === '07' || cs === '29') return []
  if (ds === '03' && declaringCustomsStatus.includes(cs)) return []
  const allowed = []
  if (ds === '03' && !row.lastUuid) allowed.push('1')
  if (row.lastUuid && ['03', '05', '06'].includes(ds)) allowed.push('2', '3')
  return allowed
}

// ===== 展示辅助：状态图标 / 时间短格式 / 数字千分位 =====
// 状态图标辅助色弱用户区分语义（颜色之外的第二编码）
export const docStatusIconMap = {
  '01': 'EditPen',
  '03': 'Finished',
  '05': 'CircleCheckFilled',
  '06': 'CircleCloseFilled',
  '07': 'CircleClose'
}
export function docStatusIcon(status, customsStatus) {
  if (status === '03' && declaringCustomsStatus.includes(customsStatus)) return 'Loading'
  return docStatusIconMap[status] || null
}
export const customsStatusIconMap = {
  '00': 'EditPen',
  '10': 'Clock',
  '20': 'UploadFilled',
  '90': 'Timer',
  '21': 'Loading',
  '2Y': 'CircleCheckFilled',
  '22': 'CircleCheckFilled',
  '2N': 'CircleCloseFilled',
  '23': 'WarningFilled',
  '24': 'RefreshLeft',
  '29': 'CircleClose'
}
export function cstIcon(s) { return customsStatusIconMap[s] || null }
// 申报回执结果图标
export const declareResultIconMap = {
  PENDING: 'Clock',
  Y: 'CircleCheckFilled',
  '1': 'CircleCheckFilled',
  N: 'CircleCloseFilled',
  '2': 'WarningFilled',
  '3': 'RefreshLeft'
}
export function declResultIcon(s) { return declareResultIconMap[s || 'PENDING'] || null }

/** 时间短格式 MM-dd HH:mm（完整时间用 fmtTimeFull 配 tooltip） */
export function fmtTimeShort(t) { return t ? String(t).replace('T', ' ').slice(5, 16) : '-' }
export function fmtTimeFull(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }

/** 数字千分位展示（最多保留 6 位小数） */
export function fmtThousand(v) {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
}

// ===== 表头列筛选（el-table filters + 远端 filter-change，按全量数据筛选） =====
/** 枚举映射({值:{label}})或选项数组([{value,label}]) → el-table-column 的 filters */
export function enumToFilters(source) {
  const entries = Array.isArray(source)
    ? source.map(o => [o.value, o.label])
    : Object.entries(source).map(([k, v]) => [k, v.label])
  return entries.map(([value, label]) => ({ text: String(label), value }))
}
/** filter-change 统一处理：勾选值写入 queryParams[key + 'List']，未勾选置 null */
export function applyFilterChange(queryParams, filters) {
  for (const [key, vals] of Object.entries(filters || {})) {
    queryParams[key + 'List'] = vals && vals.length ? [...vals] : null
  }
}
/** 发送查询前打包：数组参数折叠为逗号串（Spring 自动转换为 List），空数组置 null */
export function packListQuery(params) {
  const q = { ...params }
  for (const k of Object.keys(q)) {
    if (Array.isArray(q[k])) q[k] = q[k].length ? q[k].join(',') : null
  }
  return q
}
