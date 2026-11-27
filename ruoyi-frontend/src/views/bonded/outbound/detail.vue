<template>
  <div class="app-container" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <span>出库单详情</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="出库单号">{{ form.outboundNo }}</el-descriptions-item>
        <el-descriptions-item label="出库业务类型">{{ busiTypes.find(t => t.value === form.busiType)?.label || form.busiType }}</el-descriptions-item>
        <el-descriptions-item label="关联企业名称">{{ form.relaEntNm }}</el-descriptions-item>
        <el-descriptions-item label="出库业务单证编号">{{ form.busiFormNum }}</el-descriptions-item>
        <el-descriptions-item label="关联申报单证类型">{{ { '03': '03-报关单', '04': '04-公服出库单', '05': '05-异常调整', '06': '06-核扣清单', '99': '99-其他' }[form.relaCustomsFormType] || form.relaCustomsFormType }}</el-descriptions-item>
        <el-descriptions-item label="关联申报单证编号">{{ form.relaCustomsFormNum }}</el-descriptions-item>
        <el-descriptions-item label="核扣记录编号">{{ form.calculatingRecordNo }}</el-descriptions-item>
        <el-descriptions-item label="出库时间">{{ form.inOutTime }}</el-descriptions-item>
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
        <span>出库明细</span>
      </template>
      <el-table :data="form.items || []" size="small" border>
        <el-table-column label="来源类型" width="100" align="center">
          <template #default="s">{{ { g: 'g-入库清单', i: 'i-工单收料' }[s.row.goodsSourceType] || s.row.goodsSourceType }}</template>
        </el-table-column>
        <el-table-column label="来源单号" prop="goodsSourceNo" width="150" />
        <el-table-column label="物料">
          <template #default="s">{{ materialLabel(s.row.materialId) }}</template>
        </el-table-column>
        <el-table-column label="出库数量" prop="deliveryCnt" width="110" align="right" />
        <el-table-column label="单价" prop="price" width="110" align="right" />
        <el-table-column label="币制" prop="currency" width="80" align="center" />
        <el-table-column label="规格型号" prop="specificationsModels" width="140" show-overflow-tooltip />
        <el-table-column label="出库说明" prop="busiDesc" width="140" show-overflow-tooltip />
      </el-table>
    </el-card>
    <el-row justify="center" class="mt16">
      <el-button @click="cancel">返 回</el-button>
    </el-row>
  </div>
</template>

<script setup name="OutboundDetail">
import { getOutbound } from '@/api/bonded/biz'
import { listMaterial } from '@/api/bonded/material'
import { cstLabel, cstType, outboundBusiTypes as busiTypes } from '@/utils/bonded'

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
  getOutbound(route.params.id),
  listMaterial({ pageNum: 1, pageSize: 200, declareStatus: '2' })
]).then(([r1, r2]) => {
  form.value = r1.data
  materials.value = r2.rows
  loading.value = false
})

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/outbound' })
}
</script>
