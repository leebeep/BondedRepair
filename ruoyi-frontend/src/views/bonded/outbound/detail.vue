<template>
  <div class="app-container" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <span>出库单详情</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="出库单号">{{ form.outboundNo }}</el-descriptions-item>
        <el-descriptions-item label="申报批次号">{{ form.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="出库业务类型">
          <el-tag :type="{ '03': 'info', '04': 'primary', '05': 'success', '99': 'info' }[form.busiType]">{{ busiTypes.find(t => t.value === form.busiType)?.label || form.busiType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="出库业务单证编号">{{ form.busiFormNum }}</el-descriptions-item>
        <el-descriptions-item label="关联申报单证类型">{{ { '03': '03-报关单', '04': '04-公服出库单', '99': '99-其他' }[form.relaCustomsFormType] || form.relaCustomsFormType }}</el-descriptions-item>
        <el-descriptions-item label="关联申报单证编号">{{ form.relaCustomsFormNum }}</el-descriptions-item>
        <el-descriptions-item label="核扣记录编号">{{ form.calculatingRecordNo }}</el-descriptions-item>
        <el-descriptions-item label="出库时间">{{ form.inOutTime }}</el-descriptions-item>
        <el-descriptions-item label="单据状态">
          <el-tag :type="docStatusType(form.docStatus, form.customsStatus)">
            {{ docStatusLabel(form.docStatus, form.customsStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="海关状态">
          <el-tag :type="cstType(form.customsStatus)">{{ cstLabel(form.customsStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预录入编号">{{ form.lastUuid || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-card shadow="never" class="mt8">
      <template #header>
        <span>出库明细</span>
      </template>
      <el-table :data="form.items || []" size="small" border>
        <el-table-column label="物料编号" width="130" show-overflow-tooltip><template #default="s">{{ itemField(s.row, 'materialCd') }}</template></el-table-column>
        <el-table-column label="物料名称" min-width="150" show-overflow-tooltip><template #default="s">{{ materialNameWithMark(s.row.materialName, s.row.markType, s.row.isBonded) }}</template></el-table-column>
        <el-table-column label="版本号" width="80" align="center"><template #default="s">{{ itemField(s.row, 'version') }}</template></el-table-column>
        <el-table-column label="规格型号" width="140" show-overflow-tooltip><template #default="s">{{ itemSpec(s.row) }}</template></el-table-column>
        <el-table-column label="是否保税" width="90" align="center"><template #default="s">{{ itemBonded(s.row) }}</template></el-table-column>
        <el-table-column label="来源类型" width="100" align="center">
          <template #default="s">{{ { g: 'g-入库清单', i: 'i-工单收料' }[s.row.goodsSourceType] || s.row.goodsSourceType }}</template>
        </el-table-column>
        <el-table-column label="来源单号" prop="goodsSourceNo" width="150" />
        <el-table-column label="计量单位" width="110" align="center"><template #default="s">{{ itemUnit(s.row) }}</template></el-table-column>
        <el-table-column label="出库数量" prop="deliveryCnt" width="110" align="right" />
        <el-table-column label="单价" prop="price" width="110" align="right" />
        <el-table-column label="币制" width="110" align="center">
          <template #default="s">{{ currencyLabel(s.row.currency) }}</template>
        </el-table-column>
        <el-table-column label="出库说明" prop="busiDesc" width="140" show-overflow-tooltip />
      </el-table>
    </el-card>
    <el-row justify="end" class="mt16">
      <el-button @click="cancel">返 回</el-button>
    </el-row>
  </div>
</template>

<script setup name="OutboundDetail">
import { getOutbound } from '@/api/bonded/biz'
import { listMaterial } from '@/api/bonded/material'
import { cstLabel, cstType, docStatusLabel, docStatusType, outboundBusiTypes as busiTypes, materialNameWithMark } from '@/utils/bonded'
import { currencyLabel } from '@/utils/customsDict'
import { customsUnitLabel } from '@/utils/customsUnits'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const loading = ref(true)
const form = ref({})
const materials = ref([])

function findMaterial(row) {
  return materials.value.find(m => m.id === row?.materialId) || null
}
function itemField(row, key) {
  const v = row?.[key] ?? findMaterial(row)?.[key]
  return v == null || v === '' ? '-' : String(v)
}
function itemUnit(row) {
  const u = row?.calcUnit ?? findMaterial(row)?.calcUnit
  return u ? customsUnitLabel(u) : '-'
}
function itemBonded(row) {
  const v = row?.isBonded ?? findMaterial(row)?.isBonded
  return v == null || v === '' ? '-' : (v === '1' ? '保税' : '非保税')
}
function itemSpec(row) {
  return row?.gModel || row?.specificationsModels || findMaterial(row)?.gModel || '-'
}

Promise.all([
  getOutbound(route.params.id),
  listMaterial({ pageNum: 1, pageSize: 200, declareStatus: '2' }),
  listMaterial({ pageNum: 1, pageSize: 200, markType: '8' })
]).then(([r1, r2, r3]) => {
  form.value = r1.data
  const map = new Map()
  ;[...(r2.rows || []), ...(r3.rows || [])].forEach(m => map.set(m.id, m))
  materials.value = [...map.values()]
  loading.value = false
})

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/outbound' })
}
</script>
