<template>
  <div class="app-container" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <span>发料单详情</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="发料单号">{{ form.issueNo }}</el-descriptions-item>
        <el-descriptions-item label="工单号">{{ form.workNo }}</el-descriptions-item>
        <el-descriptions-item label="发料日期">{{ form.issueTime }}</el-descriptions-item>
        <el-descriptions-item label="申报批次号">{{ form.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="单据状态">
          <el-tag :type="{ '01': 'info', '03': 'primary', '04': 'warning', '05': 'success', '06': 'danger' }[form.docStatus]">
            {{ { '01': '草稿', '03': '审核通过', '04': '已申报', '05': '申报通过', '06': '退单' }[form.docStatus] || form.docStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="海关状态">
          <el-tag :type="cstType(form.customsStatus)">{{ cstLabel(form.customsStatus) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-card shadow="never" class="mt8">
      <template #header>
        <span>发料明细</span>
      </template>
      <el-table :data="form.items || []" size="small" border>
        <el-table-column label="物料">
          <template #default="s">{{ materialLabel(s.row.materialId) }}</template>
        </el-table-column>
        <el-table-column label="来源类型" width="120" align="center">
          <template #default="s">{{ { g: 'g-入库清单', i: 'i-工单收料' }[s.row.goodsSourceType] || s.row.goodsSourceType }}</template>
        </el-table-column>
        <el-table-column label="来源单号" prop="goodsSourceNo" width="180" />
        <el-table-column label="发料数量" prop="issueQty" width="120" align="right" />
        <el-table-column label="单价" prop="price" width="120" align="right" />
      </el-table>
    </el-card>
    <el-row justify="end" class="mt16">
      <el-button @click="cancel">返 回</el-button>
    </el-row>
  </div>
</template>

<script setup name="IssueDetail">
import { getIssue } from '@/api/bonded/biz'
import { listMaterial } from '@/api/bonded/material'
import { cstLabel, cstType } from '@/utils/bonded'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const loading = ref(true)
const form = ref({})
const materials = ref([])

function materialLabel(materialId) {
  const m = materials.value.find(x => x.id === materialId)
  return m ? m.materialName + '（' + m.materialCd + '）' : materialId
}

Promise.all([
  getIssue(route.params.id),
  listMaterial({ pageNum: 1, pageSize: 200, declareStatus: '2' })
]).then(([r1, r2]) => {
  form.value = r1.data
  materials.value = r2.rows
  loading.value = false
})

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/issue' })
}
</script>
