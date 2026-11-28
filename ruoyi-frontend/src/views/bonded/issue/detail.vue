<template>
  <div class="app-container" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <span>发料单详情</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="发料单号">{{ form.issueNo }}</el-descriptions-item>
        <el-descriptions-item label="维修工单号">{{ form.workNo }}</el-descriptions-item>
        <el-descriptions-item label="发料日期">{{ form.issueTime }}</el-descriptions-item>
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
        <span>发料明细</span>
      </template>
      <el-table :data="form.items || []" border>
        <el-table-column label="物料编号" width="130" show-overflow-tooltip><template #default="s">{{ itemField(s.row, 'materialCd') }}</template></el-table-column align="center">
        <el-table-column label="物料名称" min-width="150" show-overflow-tooltip><template #default="s">{{ materialNameWithMark(s.row.materialName, s.row.markType, s.row.isBonded) }}</template></el-table-column align="center">
        <el-table-column label="版本号" width="80" align="center"><template #default="s">{{ itemField(s.row, 'version') }}</template></el-table-column>
        <el-table-column label="规格型号" width="140" show-overflow-tooltip><template #default="s">{{ itemSpec(s.row) }}</template></el-table-column align="center">
        <el-table-column label="是否保税" width="90" align="center"><template #default="s">{{ itemBonded(s.row) }}</template></el-table-column>
        <el-table-column label="来源类型" width="120" align="center">
          <template #default="s">{{ { g: 'g-入库清单', i: 'i-工单收料' }[s.row.goodsSourceType] || s.row.goodsSourceType }}</template>
        </el-table-column>
        <el-table-column label="来源单号" prop="goodsSourceNo" width="180" align="center"/>
        <el-table-column label="计量单位" width="110" align="center"><template #default="s">{{ itemUnit(s.row) }}</template></el-table-column>
        <el-table-column label="发料数量" width="120" align="center">
          <template #default="s">{{ fmtThousand(s.row.issueQty) }}</template>
        </el-table-column>
        <el-table-column label="单价" width="120" align="center">
          <template #default="s">{{ fmtThousand(s.row.price) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-row justify="end" class="mt16">
      <el-button @click="cancel">返 回</el-button>
    </el-row>
  </div>
</template>

<script setup name="IssueDetail">
import { getIssue } from '@/api/bonded/biz'
import { cstLabel, cstType, docStatusLabel, docStatusType, materialNameWithMark, fmtThousand } from '@/utils/bonded'
import { useMaterialCache } from '@/composables/useMaterialCache'
import { customsUnitLabel } from '@/utils/customsUnits'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const loading = ref(true)
const form = ref({})

// 物料档案统一走全局缓存
const { load: loadMaterialCache, materials, byId } = useMaterialCache()
loadMaterialCache()

function findMaterial(row) {
  return byId(row?.materialId)
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
  getIssue(route.params.id),
  loadMaterialCache()
]).then(([r1]) => {
  form.value = r1.data
  loading.value = false
})

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/issue' })
}
</script>
