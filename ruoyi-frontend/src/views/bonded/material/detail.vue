<template>
  <div class="app-container" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <span>物料备案详情</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="物料编号">{{ form.materialCd }}</el-descriptions-item>
        <el-descriptions-item label="版本号">{{ form.version }}</el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ form.materialName }}</el-descriptions-item>
        <el-descriptions-item label="六类标记">{{ markLabel(form.markType) }}</el-descriptions-item>
        <el-descriptions-item label="料件成品标志">{{ { I: 'I-料件', E: 'E-成品', S: 'S-半成品' }[form.gType] }}</el-descriptions-item>
        <el-descriptions-item label="是否保税">
          <el-tag :type="form.isBonded === '1' ? 'primary' : 'info'">{{ form.isBonded === '1' ? '保税料件' : '非保税料件' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="HS编码">{{ form.codeTs }}</el-descriptions-item>
        <el-descriptions-item label="HS商品名称">{{ form.gName }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ form.gModel }}</el-descriptions-item>
        <el-descriptions-item label="计量单位">{{ form.calcUnit }}</el-descriptions-item>
        <el-descriptions-item label="法定计量单位">{{ form.unit1 }}</el-descriptions-item>
        <el-descriptions-item label="法定第二单位">{{ form.unit2 }}</el-descriptions-item>
        <el-descriptions-item label="货物毛重">{{ form.totalGrossWt }}</el-descriptions-item>
        <el-descriptions-item label="货物净重">{{ form.totalNetWt }}</el-descriptions-item>
        <el-descriptions-item label="征免方式">{{ dutyLabel(form.dutyMode) }}</el-descriptions-item>
        <el-descriptions-item label="备案状态">
          <el-tag :type="['info', 'warning', 'success', 'danger'][form.declareStatus] || 'info'">
            {{ { '0': '未申报', '1': '已申报', '2': '通过', '3': '退单' }[form.declareStatus] || form.declareStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="海关状态">
          <el-tag :type="customsStatusMap[form.customsStatus]?.type || 'info'">
            {{ customsStatusMap[form.customsStatus]?.label || form.customsStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预录入编号">{{ form.uuid }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ form.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-row justify="center" class="mt16">
      <el-button @click="cancel">返 回</el-button>
    </el-row>
  </div>
</template>

<script setup name="MaterialDetail">
import { getMaterial } from '@/api/bonded/material'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const loading = ref(true)
const form = ref({})

const markTypeOptions = [
  { value: '1', label: '待维修货物' },
  { value: '2', label: '已维修货物' },
  { value: '3', label: '无法维修货物' },
  { value: '4', label: '维修用料件' },
  { value: '5', label: '旧件或坏件' },
  { value: '6', label: '边角料' },
  { value: '8', label: '维修服务费' }
]

const customsStatusMap = {
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

const dutyMap = {
  '1': '1-照章征税', '2': '2-折半征税', '3': '3-全免', '4': '4-特案', '5': '5-免税',
  '6': '6-保证金', '7': '7-保函', '8': '8-全额退税', '9': '9-其他'
}

function markLabel(markType) {
  const item = markTypeOptions.find(d => d.value === markType)
  return item ? item.label : markType
}

function dutyLabel(dutyMode) {
  return dutyMap[dutyMode] || dutyMode
}

getMaterial(route.params.id).then(res => {
  form.value = res.data
  loading.value = false
})

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/material' })
}
</script>
