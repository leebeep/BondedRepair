<template>
  <div class="app-container" v-loading="loading">
    <el-card shadow="never">
      <template #header><span>物料库存详情</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="物料编号">{{ stock.materialCd }}</el-descriptions-item>
        <el-descriptions-item label="版本号">{{ stock.version }}</el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ materialNameWithMark(stock.materialName, stock.markType, stock.isBonded) }}</el-descriptions-item>
        <el-descriptions-item label="物料属性">{{ markLabel(stock.markType) }}</el-descriptions-item>
        <el-descriptions-item label="是否保税">
          <el-tag :type="stock.isBonded === '1' ? 'primary' : 'info'">{{ stock.isBonded === '1' ? '保税' : '非保税' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="HS编码">{{ stock.codeTs || '-' }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ stock.gModel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计量单位">{{ customsUnitLabel(stock.calcUnit) }}</el-descriptions-item>
        <el-descriptions-item label="备案状态">
          <el-tag :type="['info', 'warning', 'success', 'danger'][stock.declareStatus] || 'info'">
            {{ { '0': '未申报', '1': '已申报', '2': '通过', '3': '退单' }[stock.declareStatus] || stock.declareStatus || '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前库存">
          <span class="stock-qty">{{ fmtQty(stock.qty) }}</span> {{ customsUnitLabel(stock.calcUnit) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="head-row">
          <span>更新记录</span>
          <el-button icon="Download" size="small" @click="handleExport">导出更新记录</el-button>
        </div>
      </template>
      <el-form :inline="true" size="small">
        <el-form-item label="发生时间">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
          <el-button icon="Refresh" @click="resetList">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="listLoading" :data="rows">
        <el-table-column type="index" label="序号" width="55" align="center" :index="indexMethod" />
        <el-table-column label="业务环节" width="110" align="center">
          <template #default="s"><el-tag size="small" :type="{ IN: 'success', ISSUE: 'warning', RCV: 'primary', OUT: 'danger' }[s.row.bizType]">{{ bizLabel[s.row.bizType] || s.row.bizType }}</el-tag></template>
        </el-table-column>
        <el-table-column label="数量" min-width="120" align="center">
          <template #default="s"><span :class="Number(s.row.qty) >= 0 ? 'qty-plus' : 'qty-minus'">{{ fmtQty(s.row.qty) }}</span></template>
        </el-table-column>
        <el-table-column label="计量单位" width="90" align="center">
          <template #default="s">{{ customsUnitLabel(stock.calcUnit) }}</template>
        </el-table-column>
        <el-table-column label="发生时间" width="110" align="center">
          <template #default="s">
            <el-tooltip :content="fmtTimeFull(s.row.createTime)" placement="top"><span>{{ fmtTimeShort(s.row.createTime) }}</span></el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="listQuery.pageNum" v-model:limit="listQuery.pageSize" @pagination="getList" />
    </el-card>

    <el-row justify="end" class="mt16">
      <el-button @click="cancel">返 回</el-button>
    </el-row>
  </div>
</template>

<script setup name="StockDetail">
import { materialNameWithMark, fmtTimeShort, fmtTimeFull } from '@/utils/bonded'
import { listStock, getMaterialStock } from '@/api/bonded/biz'
import { customsUnitLabel } from '@/utils/customsUnits'

const { proxy } = getCurrentInstance()
const route = useRoute()
const materialId = route.query.materialId

const markTypeOptions = [
  { value: '1', label: '待维修货物' }, { value: '2', label: '已维修货物' }, { value: '3', label: '无法维修货物' },
  { value: '4', label: '维修用料件' }, { value: '5', label: '旧件或坏件' }, { value: '6', label: '边角料' },
  { value: '8', label: '维修服务费' }
]
const bizLabel = { IN: '入库', ISSUE: '发料', RCV: '收料', OUT: '出库' }

const loading = ref(true)
const stock = ref({})
const rows = ref([]); const listLoading = ref(false); const total = ref(0)
const listQuery = reactive({ pageNum: 1, pageSize: 10 })
const dateRange = ref([])

function markLabel(markType) {
  const item = markTypeOptions.find(d => d.value === markType)
  return item ? item.label : markType
}
function fmtQty(v) { return v === null || v === undefined ? '0' : Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 6 }) }
function indexMethod(index) { return (listQuery.pageNum - 1) * listQuery.pageSize + index + 1 }

function getList() {
  listLoading.value = true
  const params = { ...listQuery, materialId }
  if (dateRange.value && dateRange.value.length === 2) { params.beginTime = dateRange.value[0]; params.endTime = dateRange.value[1] }
  listStock(params).then(r => { rows.value = r.rows; total.value = r.total; listLoading.value = false })
}
function resetList() {
  Object.assign(listQuery, { pageNum: 1 })
  dateRange.value = []
  getList()
}
function cancel() { proxy.$tab.closeOpenPage({ path: '/bonded/stock' }) }
function handleExport() {
  const params = { materialId }
  if (dateRange.value && dateRange.value.length === 2) { params.beginTime = dateRange.value[0]; params.endTime = dateRange.value[1] }
  proxy.download('/bonded/stock/export', params, `${stock.value.materialCd || 'material'}_update_records.xlsx`)
}

getMaterialStock(materialId).then(r => { stock.value = r.data.stock; loading.value = false })
getList()
</script>

<style scoped>
.head-row { display: flex; align-items: center; justify-content: space-between; }
.stock-qty { font-size: 15px; font-weight: bold; color: #e6a23c; }
.qty-plus { color: #67c23a; font-weight: 600; }
.qty-minus { color: #f56c6c; font-weight: 600; }
</style>
