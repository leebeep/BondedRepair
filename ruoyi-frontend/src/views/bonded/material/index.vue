<template>
  <div class="p2">
    <el-card shadow="never" class="mb8">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
        <el-form-item label="物料编号" prop="materialCd">
          <el-input v-model="queryParams.materialCd" placeholder="请输入物料编号" clearable style="width: 180px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="物料名称" prop="materialName">
          <el-input v-model="queryParams.materialName" placeholder="请输入物料名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="物料属性" prop="markType">
          <el-select v-model="queryParams.markType" placeholder="全部" clearable multiple collapse-tags style="width: 220px">
            <el-option v-for="d in markTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="海关状态" prop="customsStatus">
          <el-select v-model="queryParams.customsStatus" placeholder="全部" clearable multiple collapse-tags style="width: 150px">
            <el-option v-for="(v, k) in customsStatusMap" :key="k" :label="v.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" v-hasPermi="['bonded:material:add']" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Download" v-hasPermi="['bonded:material:export']" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="materialList" @filter-change="onFilterChange">
      <el-table-column label="物料编号" prop="materialCd" width="140" show-overflow-tooltip align="center">
        <template #default="scope"><el-link type="primary" :underline="false" @click="handleDetail(scope.row)">{{ scope.row.materialCd }}</el-link></template>
      </el-table-column>
      <el-table-column label="物料名称" show-overflow-tooltip min-width="160" align="center">
        <template #default="scope">{{ scope.row.materialName }}</template>
      </el-table-column>
      <el-table-column label="物料属性" width="170" align="center" column-key="markType" :filters="markTypeFilters">
        <template #default="scope">{{ markLabel(scope.row.markType) }}</template>
      </el-table-column>
      <el-table-column label="料件成品标志" prop="gType" width="150" align="center" column-key="gType" :filters="gTypeFilters">
        <template #default="scope">{{ { I: '料件', E: '成品', S: '半成品' }[scope.row.gType] }}</template>
      </el-table-column>
      <el-table-column label="HS编码" prop="codeTs" width="120" align="center"/>
      <el-table-column label="计量单位" width="120" align="center">
        <template #default="scope">{{ customsUnitLabel(scope.row.calcUnit) }}</template>
      </el-table-column>
      <el-table-column label="保税" prop="isBonded" width="70" align="center" column-key="isBonded" :filters="isBondedFilters">
        <template #default="scope">
          <el-tag :type="scope.row.isBonded === '1' ? 'primary' : 'info'">{{ scope.row.isBonded === '1' ? '保税' : '非保税' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备案状态" width="100" align="center" column-key="declareStatus" :filters="declareStatusFilters">
        <template #default="scope">
          <el-tag :type="['0', '1', '2', '3'][scope.row.declareStatus] === undefined ? 'info' : ['info', 'warning', 'success', 'danger'][scope.row.declareStatus]"
                  :icon="{ '0': 'EditPen', '1': 'Clock', '2': 'CircleCheckFilled', '3': 'CircleCloseFilled' }[scope.row.declareStatus]">
            {{ { '0': '未申报', '1': '已申报', '2': '通过', '3': '退单' }[scope.row.declareStatus] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="海关状态" width="110" align="center" column-key="customsStatus" :filters="customsFilters">
        <template #default="scope">
          <el-tag :type="customsStatusMap[scope.row.customsStatus]?.type || 'info'" :icon="cstIcon(scope.row.customsStatus)">
            {{ customsStatusMap[scope.row.customsStatus]?.label || scope.row.customsStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button v-if="canExec(scope.row, '1')" link type="success" icon="Stamp" v-hasPermi="['bonded:material:declare']" @click="handleDeclare('1', scope.row)">备案</el-button>
          <el-button v-if="canExec(scope.row, '2')" link type="warning" icon="EditPen" v-hasPermi="['bonded:material:declare']" @click="handleDeclare('2', scope.row)">变更</el-button>
          <el-button v-if="canExec(scope.row, '3')" link type="danger" icon="Delete" v-hasPermi="['bonded:material:declare']" @click="handleDeclare('3', scope.row)">作废</el-button>
          <el-button link type="primary" icon="Edit" v-hasPermi="['bonded:material:edit']" :disabled="scope.row.declareStatus === '2'" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="danger" icon="Delete" v-hasPermi="['bonded:material:remove']" :disabled="scope.row.declareStatus === '2'" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="Material">
import { listMaterial, delMaterial, declareMaterial } from '@/api/bonded/material'
import { customsUnitLabel } from '@/utils/customsUnits'
import { materialMarkTypes as markTypeOptions, cstIcon, customsStatusMap, enumToFilters, applyFilterChange, packListQuery } from '@/utils/bonded'

const { proxy } = getCurrentInstance()
const router = useRouter()

const materialList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const markTypeFilters = enumToFilters(markTypeOptions)
const gTypeFilters = enumToFilters([{ value: 'I', label: '料件' }, { value: 'E', label: '成品' }, { value: 'S', label: '半成品' }])
const isBondedFilters = enumToFilters([{ value: '1', label: '保税' }, { value: '0', label: '非保税' }])
const declareStatusFilters = enumToFilters([{ value: '0', label: '未申报' }, { value: '1', label: '已申报' }, { value: '2', label: '通过' }, { value: '3', label: '退单' }])
const customsFilters = enumToFilters(customsStatusMap)

// 行内申报按钮联动：与后端 checkDeclareAllowed 同规则
// 备案(1)限未申报(0)/退单(3)；变更(2)/作废(3)限备案通过(2)；服务费虚物料(markType=8)禁止申报
const canExec = (row, t) => {
  if (row.markType === '8') return false
  const st = row.declareStatus
  if (t === '1') return st === '0' || st === '3'
  return st === '2'
}

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, materialCd: null, materialName: null, markType: [], customsStatus: [] }
})

const { queryParams } = toRefs(data)

function markLabel(markType) {
  const item = markTypeOptions.find(d => d.value === markType)
  return item ? item.label : markType
}

function getList() {
  loading.value = true
  listMaterial(packListQuery(queryParams.value)).then(res => {
    materialList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function onFilterChange(filters) { applyFilterChange(queryParams.value, filters); handleQuery() }

function resetQuery() {
  Object.assign(queryParams.value, { pageNum: 1, pageSize: 10, materialCd: null, materialName: null, markType: [], customsStatus: [] })
  handleQuery()
}

function handleDeclare(executeType, row) {
  const nameMap = { '1': '备案申报', '2': '变更申报', '3': '作废申报' }
  proxy.$modal.confirm(`确认对物料【${row.materialCd}】执行【${nameMap[executeType]}】？申报数据将写入海关镜像表。`).then(() => {
    return declareMaterial(executeType, row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess(`${nameMap[executeType]}已提交，等待客户端采集传输`)
  }).catch(() => {})
}

// 新增：跳转录入页
function handleAdd() {
  router.push('/bonded/material-form/index')
}

// 修改：跳转录入页
function handleUpdate(row) {
  router.push('/bonded/material-form/index/' + row.id)
}

// 详情：跳转详情页
function handleDetail(row) {
  router.push('/bonded/material-detail/index/' + row.id)
}

function handleDelete(row) {
  if (row.declareStatus === '2') return proxy.$modal.msgError('备案通过的物料不可删除，如需调整请走变更或作废申报')
  proxy.$modal.confirm(`确认删除物料【${row.materialCd}】？删除后不可恢复，需重新备案。`).then(() => delMaterial(row.id)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

function handleExport() {
  proxy.download('bonded/material/export', { ...packListQuery(queryParams.value) }, `material_${new Date().getTime()}.xlsx`)
}

getList()
</script>
