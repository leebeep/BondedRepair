<template>
  <div class="app-container" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <span>入库单详情</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="入库单号">{{ form.inboundNo }}</el-descriptions-item>
        <el-descriptions-item label="入库业务类型">
          <el-tag :type="{ '04': 'primary', '05': 'success', '99': 'info' }[form.busiType]">{{ { '04': '04-待维修货物', '05': '05-维修用料', '99': '99-其他' }[form.busiType] || form.busiType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="入库时间">{{ form.inOutTime }}</el-descriptions-item>
        <el-descriptions-item label="关联申报单证类型">{{ { '01': '01-径予放行申明', '02': '02-公服入库单', '03': '03-报关单', '05': '05-异常调整', '99': '99-其他' }[form.relaCustomsFormType] || form.relaCustomsFormType }}</el-descriptions-item>
        <el-descriptions-item label="关联申报单证编号">{{ form.relaCustomsFormNum }}</el-descriptions-item>
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
        <span>物料明细</span>
      </template>
      <el-table :data="form.items || []" border>
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column label="物料编号" width="130" show-overflow-tooltip align="center">
          <template #default="s">{{ s.row.materialCd || '-' }}</template>
        </el-table-column>
        <el-table-column label="物料名称" min-width="170" show-overflow-tooltip align="center">
          <template #default="s">{{ s.row.materialName || '-' }}</template>
        </el-table-column>
        <el-table-column label="规格型号" prop="specificationsModels" width="140" show-overflow-tooltip align="center"/>
        <el-table-column label="数量" width="100" align="center">
          <template #default="s">{{ fmtThousand(s.row.storageCnt) }}</template>
        </el-table-column>
        <el-table-column label="计量单位" width="100" align="center">
          <template #default="s">{{ s.row.calcUnit ? customsUnitLabel(s.row.calcUnit) : '-' }}</template>
        </el-table-column>
        <el-table-column label="单价" width="100" align="center">
          <template #default="s">{{ fmtThousand(s.row.price) }}</template>
        </el-table-column>
        <el-table-column label="币制" width="90" align="center">
          <template #default="s">{{ currencyLabel(s.row.currency) }}</template>
        </el-table-column>
        <el-table-column label="总价" width="120" align="center">
          <template #default="s">{{ fmtThousand(itemTotal(s.row)) }}</template>
        </el-table-column>
        <el-table-column label="法定计量单位1" width="120" align="center">
          <template #default="s">{{ s.row.unit1 ? customsUnitLabel(s.row.unit1) : '-' }}</template>
        </el-table-column>
        <el-table-column label="第一法定数量" width="110" align="center">
          <template #default="s">{{ fmtThousand(s.row.unit1Num) }}</template>
        </el-table-column>
        <el-table-column label="法定计量单位2" width="120" align="center">
          <template #default="s">{{ s.row.unit2 ? customsUnitLabel(s.row.unit2) : '-' }}</template>
        </el-table-column>
        <el-table-column label="第二法定数量" width="110" align="center">
          <template #default="s">{{ fmtThousand(s.row.unit2Num) }}</template>
        </el-table-column>
        <el-table-column v-if="form.relaCustomsFormType === '01'" label="品牌类型" prop="brandType" width="100" show-overflow-tooltip align="center"/>
        <el-table-column v-if="form.relaCustomsFormType === '01'" label="监管方式" prop="tradeModeCd" width="100" show-overflow-tooltip align="center"/>
        <el-table-column v-if="form.relaCustomsFormType === '01'" label="启抵国" prop="arrivingCountryCode" width="90" show-overflow-tooltip align="center"/>
        <el-table-column v-if="form.relaCustomsFormType === '01'" label="原产国" prop="endCountryCode" width="90" show-overflow-tooltip align="center"/>
        <el-table-column v-if="form.busiType === '99'" label="入库说明" prop="busiDesc" width="140" show-overflow-tooltip align="center"/>
      </el-table>
    </el-card>
    <el-row justify="end" class="mt16">
      <el-button @click="cancel">返 回</el-button>
    </el-row>
  </div>
</template>

<script setup name="InboundDetail">
import { getInbound } from '@/api/bonded/biz'
import { cstLabel, cstType, docStatusLabel, docStatusType, fmtThousand } from '@/utils/bonded'
import { currencyLabel } from '@/utils/customsDict'
import { customsUnitLabel } from '@/utils/customsUnits'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const loading = ref(true)
const form = ref({})

function itemTotal(row) {
  const t = (Number(row.storageCnt) || 0) * (Number(row.price) || 0)
  return row.storageCnt != null && row.price != null ? String(parseFloat(t.toFixed(6))) : '-'
}

getInbound(route.params.id).then(r => {
  form.value = r.data
  loading.value = false
})

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/inbound' })
}
</script>
