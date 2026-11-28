<template>
  <div class="app-container" v-loading="loading">
    <el-card shadow="never">
      <div class="hd">
        <b>{{ wo.workNo }}</b>
        <el-tag :type="woStatusMap[wo.orderStatus]?.type || 'info'">{{ woStatusMap[wo.orderStatus]?.label || wo.orderStatus }}</el-tag>
        <span class="m">计划完工 {{ (wo.finishDate || '').slice(0, 10) || '-' }}</span>
        <span class="m" v-if="wo.remark">{{ wo.remark }}</span>
        <el-button link type="primary" icon="Back" class="back" @click="cancel">返回列表</el-button>
      </div>
      <div class="sumline">
        <span class="si">发料合计<b>{{ fmt(sums.issued) }}</b></span>
        <span class="si">收回合计（已修+无法修）<b>{{ fmt(sums.receivedRepair) }}</b></span>
        <span class="si">未使用退回<b>{{ fmt(sums.unused) }}</b></span>
        <span class="si">待出清<b>{{ fmt(sums.toClear) }}</b></span>
        <span class="si">已出库<b>{{ fmt(sums.outbound) }}</b></span>
        <span class="si">维修服务费<b v-if="wo.serviceFee != null && Number(wo.serviceFee) > 0">¥{{ wo.serviceFee }}（{{ wo.currency || '-' }}）</b><b v-else>—</b></span>
      </div>
    </el-card>
    <el-card shadow="never" class="mt8">
      <el-tabs v-model="tab">
        <el-tab-pane label="发料单" name="issue">
          <el-table :data="issues" border>
            <el-table-column label="发料单号" width="200">
              <template #default="s">
                <span class="docno">{{ s.row.issueNo }}</span>
                <div class="sub">{{ s.row.issueTime }}</div>
              </template>
            </el-table-column>
            <el-table-column label="发料明细" min-width="320">
              <template #default="s"><div class="lines">{{ issueLines(s.row.items) }}</div></template>
            </el-table-column>
            <el-table-column label="单据状态" width="120" align="center">
              <template #default="s"><el-tag :type="docStatusType(s.row.docStatus, s.row.customsStatus)">{{ docStatusLabel(s.row.docStatus, s.row.customsStatus) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="海关状态" width="120" align="center">
              <template #default="s"><el-tag :type="cstType(s.row.customsStatus)">{{ cstLabel(s.row.customsStatus) }}</el-tag></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="收料单" name="receive">
          <el-table :data="receives" border>
            <el-table-column label="收料单号" width="200">
              <template #default="s">
                <span class="docno">{{ s.row.receiveNo }}</span>
                <div class="sub">{{ s.row.receiveTime }}</div>
              </template>
            </el-table-column>
            <el-table-column label="收料明细" min-width="360">
              <template #default="s"><div class="lines">{{ receiveLines(s.row.items) }}</div></template>
            </el-table-column>
            <el-table-column label="单据状态" width="120" align="center">
              <template #default="s"><el-tag :type="docStatusType(s.row.docStatus, s.row.customsStatus)">{{ docStatusLabel(s.row.docStatus, s.row.customsStatus) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="海关状态" width="120" align="center">
              <template #default="s"><el-tag :type="cstType(s.row.customsStatus)">{{ cstLabel(s.row.customsStatus) }}</el-tag></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="出库单" name="outbound">
          <el-table :data="outbounds" border>
            <el-table-column label="出库单号" width="200">
              <template #default="s">
                <span class="docno">{{ s.row.outboundNo }}</span>
                <div class="sub">{{ s.row.inOutTime }}</div>
              </template>
            </el-table-column>
            <el-table-column label="业务类型" width="140" align="center">
              <template #default="s">{{ { '03': '03 销毁', '04': '04 复运出境', '05': '05 复运回境内区外', '99': '99 其他' }[s.row.busiType] || s.row.busiType }}</template>
            </el-table-column>
            <el-table-column label="单据状态" width="120" align="center">
              <template #default="s"><el-tag :type="docStatusType(s.row.docStatus, s.row.customsStatus)">{{ docStatusLabel(s.row.docStatus, s.row.customsStatus) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="海关状态" width="120" align="center">
              <template #default="s"><el-tag :type="cstType(s.row.customsStatus)">{{ cstLabel(s.row.customsStatus) }}</el-tag></template>
            </el-table-column>
          </el-table>
          <div class="tab-note">注：出库单经 出库明细来源(i/收料单号) 推导关联本工单；直接来源（g/入库清单）的出库不在此列。</div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup name="WorkOrderDetail">
import { getWorkOrder } from '@/api/bonded/workorder'
import { returnTypes, docStatusLabel, docStatusType, cstLabel, cstType } from '@/utils/bonded'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const woStatusMap = {
  '01': { label: '01 立项', type: 'info' },
  '02': { label: '02 维修中', type: 'primary' },
  '03': { label: '03 已收齐', type: 'success' },
  '04': { label: '04 已出清', type: 'warning' },
  '05': { label: '05 已结案', type: 'danger' }
}

const loading = ref(false)
const tab = ref('issue')
const wo = ref({})
const issues = ref([])
const receives = ref([])
const outbounds = ref([])
const sums = ref({})

function fmt(v) { const n = Number(v); return Number.isFinite(n) ? parseFloat(n.toFixed(6)) : '-' }
function rtLabel(t) { return returnTypes.find(x => x.value === t)?.label || t }
function issueLines(items) {
  return (items || []).map(i => `${i.materialName || i.materialCd || '-'} × ${i.issueQty}`).join('\n') || '—'
}
function receiveLines(items) {
  return (items || []).map(i => `${rtLabel(i.returnType)}：${i.materialName || i.materialCd || '-'} × ${i.materialNum}`).join('\n') || '—'
}

const id = route.params.id
loading.value = true
getWorkOrder(id).then(r => {
  const d = r.data || {}
  wo.value = d.workOrder || {}
  issues.value = d.issues || []
  receives.value = d.receives || []
  outbounds.value = d.outbounds || []
  sums.value = d.sums || {}
  loading.value = false
}).catch(() => { loading.value = false })

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/workorder' })
}
</script>

<style scoped>
.hd { display: flex; align-items: center; gap: 14px; padding-bottom: 12px; }
.hd b { font-size: 16px; }
.hd .m { color: #909399; font-size: 13px; }
.hd .back { margin-left: auto; }
.sumline { display: flex; gap: 28px; padding: 10px 0 4px; flex-wrap: wrap; border-top: 1px solid #ebeef5; }
.si { font-size: 13px; color: #909399; }
.si b { font-size: 15px; margin-left: 6px; color: #1e293b; font-variant-numeric: tabular-nums; }
.lines { white-space: pre-line; font-size: 13px; line-height: 1.7; }
.sub { font-size: 12px; color: #909399; }
.docno { color: var(--el-color-primary); font-weight: 500; }
.tab-note { font-size: 12px; color: #909399; padding: 8px 4px 0; }
</style>
