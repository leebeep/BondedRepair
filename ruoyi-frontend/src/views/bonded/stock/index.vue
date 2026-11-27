<template>
  <div class="p2">
    <el-form :inline="true" label-width="80px">
      <el-form-item label="物料编号"><el-input v-model="queryParams.materialCd" clearable style="width: 140px" @keyup.enter="getList" /></el-form-item>
      <el-form-item label="物料名称"><el-input v-model="queryParams.materialName" clearable style="width: 150px" @keyup.enter="getList" /></el-form-item>
      <el-form-item label="规格型号"><el-input v-model="queryParams.gModel" clearable style="width: 130px" @keyup.enter="getList" /></el-form-item>
      <el-form-item label="HS编码"><el-input v-model="queryParams.codeTs" clearable style="width: 130px" @keyup.enter="getList" /></el-form-item>
      <el-form-item label="货物类型">
        <el-select v-model="queryParams.markType" clearable placeholder="全部" style="width: 130px">
          <el-option v-for="d in markTypes" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width: 220px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetLedgerQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows">
      <el-table-column type="index" label="序号" width="55" align="center" :index="indexMethod" />
      <el-table-column label="物料编号" prop="materialCd" min-width="150" show-overflow-tooltip />
      <el-table-column label="物料名称" prop="materialName" min-width="220" show-overflow-tooltip />
      <el-table-column label="规格型号" prop="gModel" min-width="120" show-overflow-tooltip />
      <el-table-column label="HS编码" prop="codeTs" min-width="110" show-overflow-tooltip />
      <el-table-column label="货物类型" width="110" align="center">
        <template #default="s">{{ markTypes.find(d => d.value === s.row.markType)?.label || s.row.markType }}</template>
      </el-table-column>
      <el-table-column label="是否保税" width="90" align="center">
        <template #default="s"><el-tag size="small" :type="s.row.isBonded === '1' ? 'primary' : 'info'">{{ s.row.isBonded === '1' ? '保税' : '非保税' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="当前库存" prop="balance" width="120" align="right">
        <template #default="s"><span :class="{ 'qty-minus': Number(s.row.balance) < 0 }">{{ fmtQty(s.row.balance) }}</span></template>
      </el-table-column>
      <el-table-column label="在库时间" prop="stockDays" width="100" align="right">
        <template #default="s">{{ s.row.stockDays === null || s.row.stockDays === undefined ? '-' : s.row.stockDays + ' 天' }}</template>
      </el-table-column>
      <el-table-column label="最新更新时间" prop="createTime" width="150" align="center" />
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template #default="s"><el-button link type="primary" @click="goDetail(s.row.materialId)">物料详情</el-button></template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="Stock">
import { listStock } from '@/api/bonded/biz'

const markTypes = [
  { value: '1', label: '待维修货物' }, { value: '2', label: '已维修货物' }, { value: '3', label: '无法维修货物' },
  { value: '4', label: '维修用料件' }, { value: '5', label: '旧件或坏件' }, { value: '6', label: '边角料' },
  { value: '8', label: '维修服务费' }
]
const { proxy } = getCurrentInstance()

const rows = ref([]); const loading = ref(false); const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, materialCd: null, materialName: null, gModel: null, codeTs: null, markType: null })
const dateRange = ref([])

function fmtQty(v) {
  if (v === null || v === undefined) return ''
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
}
function indexMethod(index) { return (queryParams.pageNum - 1) * queryParams.pageSize + index + 1 }
function getList() {
  loading.value = true
  const params = { ...queryParams }
  if (dateRange.value && dateRange.value.length === 2) { params.beginTime = dateRange.value[0]; params.endTime = dateRange.value[1] }
  listStock(params).then(r => { rows.value = r.rows; total.value = r.total; loading.value = false })
}
function resetLedgerQuery() {
  Object.assign(queryParams, { pageNum: 1, materialCd: null, materialName: null, gModel: null, codeTs: null, markType: null })
  dateRange.value = []
  getList()
}
function goDetail(materialId) { proxy.$router.push({ path: '/bonded/stockDetail', query: { materialId } }) }

getList()
</script>

<style scoped>
.qty-minus { color: #f56c6c; font-weight: 600; }
</style>
