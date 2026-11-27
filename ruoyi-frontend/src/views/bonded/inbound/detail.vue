<template>
  <div class="app-container" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <span>入库单详情</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="入库单号">{{ form.inboundNo }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">
          <el-tag :type="form.busiType === '04' ? 'primary' : 'success'">{{ { '04': '04-待维修货物', '05': '05-维修用料' }[form.busiType] || form.busiType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="关联企业名称">{{ form.relaEntNm }}</el-descriptions-item>
        <el-descriptions-item label="入库时间">{{ form.inOutTime }}</el-descriptions-item>
        <el-descriptions-item label="关联申报单证类型">{{ { '01': '01-径予放行申明', '02': '02-公服入库单', '03': '03-报关单', '05': '05-异常调整', '99': '99-其他' }[form.relaCustomsFormType] || form.relaCustomsFormType }}</el-descriptions-item>
        <el-descriptions-item label="关联申报单证编号">{{ form.relaCustomsFormNum }}</el-descriptions-item>
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
        <span>物料明细</span>
      </template>
      <el-table :data="form.items || []" size="small" border>
        <el-table-column label="物料">
          <template #default="s">{{ materialLabel(s.row.materialId) }}</template>
        </el-table-column>
        <el-table-column label="数量" prop="storageCnt" width="110" align="right" />
        <el-table-column label="单价" prop="price" width="110" align="right" />
        <el-table-column label="币制" prop="currency" width="80" align="center" />
        <el-table-column label="规格型号" prop="specificationsModels" width="140" show-overflow-tooltip />
        <el-table-column label="来源" width="150">
          <template #default="s">{{ s.row.goodsSourceType }}-{{ s.row.goodsSourceNo }}</template>
        </el-table-column>
        <el-table-column label="法定数量1" prop="unit1Num" width="100" align="right" />
        <el-table-column label="法定数量2" prop="unit2Num" width="100" align="right" />
        <el-table-column label="品牌" prop="brandType" width="90" />
        <el-table-column label="监管方式" prop="tradeModeCd" width="90" />
        <el-table-column label="启抵国" prop="arrivingCountryCode" width="80" />
        <el-table-column label="原产国" prop="endCountryCode" width="80" />
        <el-table-column label="业务单证编号" prop="busiFormNum" width="130" />
        <el-table-column label="入库说明" prop="busiDesc" width="140" show-overflow-tooltip />
      </el-table>
    </el-card>
    <el-row justify="end" class="mt16">
      <el-button @click="cancel">返 回</el-button>
    </el-row>
  </div>
</template>

<script setup name="InboundDetail">
import { getInbound } from '@/api/bonded/biz'
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
  getInbound(route.params.id),
  listMaterial({ pageNum: 1, pageSize: 200, declareStatus: '2' })
]).then(([r1, r2]) => {
  form.value = r1.data
  materials.value = r2.rows
  loading.value = false
})

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/inbound' })
}
</script>
