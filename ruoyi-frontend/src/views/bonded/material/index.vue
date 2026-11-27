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
        <el-form-item label="六类标记" prop="markType">
          <el-select v-model="queryParams.markType" placeholder="全部" clearable style="width: 200px">
            <el-option v-for="d in markTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="海关状态" prop="customsStatus">
          <el-select v-model="queryParams.customsStatus" placeholder="全部" clearable style="width: 120px">
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
        <el-button type="success" plain icon="Stamp" :disabled="multiple" v-hasPermi="['bonded:material:declare']" @click="handleDeclare('1')">备案申报</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="EditPen" :disabled="multiple" v-hasPermi="['bonded:material:declare']" @click="handleDeclare('2')">变更申报</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" v-hasPermi="['bonded:material:declare']" @click="handleDeclare('3')">作废申报</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Download" v-hasPermi="['bonded:material:export']" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="materialList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="物料编号" prop="materialCd" width="140" show-overflow-tooltip />
      <el-table-column label="物料名称" prop="materialName" show-overflow-tooltip min-width="160" />
      <el-table-column label="六类标记" width="170">
        <template #default="scope">{{ markLabel(scope.row.markType) }}</template>
      </el-table-column>
      <el-table-column label="料件/成品" prop="gType" width="90" align="center">
        <template #default="scope">{{ { I: '料件', E: '成品', S: '半成品' }[scope.row.gType] }}</template>
      </el-table-column>
      <el-table-column label="HS编码" prop="codeTs" width="120" />
      <el-table-column label="计量单位" prop="calcUnit" width="90" align="center" />
      <el-table-column label="保税" prop="isBonded" width="70" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.isBonded === '1' ? 'primary' : 'info'">{{ scope.row.isBonded === '1' ? '保税' : '非保税' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备案状态" width="90" align="center">
        <template #default="scope">
          <el-tag :type="['0', '1', '2', '3'][scope.row.declareStatus] === undefined ? 'info' : ['info', 'warning', 'success', 'danger'][scope.row.declareStatus]">
            {{ { '0': '未申报', '1': '已申报', '2': '通过', '3': '退单' }[scope.row.declareStatus] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="海关状态" width="110" align="center">
        <template #default="scope">
          <el-tag :type="customsStatusMap[scope.row.customsStatus]?.type || 'info'">
            {{ customsStatusMap[scope.row.customsStatus]?.label || scope.row.customsStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预录入编号" prop="uuid" width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="220" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" v-hasPermi="['bonded:material:query']" @click="handleDetail(scope.row)">详情</el-button>
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

const { proxy } = getCurrentInstance()
const router = useRouter()

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

const materialList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const multiple = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, materialCd: null, materialName: null, markType: null, customsStatus: null }
})

const { queryParams } = toRefs(data)

function markLabel(markType) {
  const item = markTypeOptions.find(d => d.value === markType)
  return item ? item.label : markType
}

function getList() {
  loading.value = true
  listMaterial(queryParams.value).then(res => {
    materialList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
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
  proxy.$modal.confirm('是否确认删除该物料？').then(() => delMaterial(row.id)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

function handleDeclare(executeType) {
  const nameMap = { '1': '备案申报', '2': '变更申报', '3': '作废申报' }
  proxy.$modal.confirm(`确认对选中的 ${ids.value.length} 条物料执行【${nameMap[executeType]}】？申报数据将写入海关镜像表。`).then(() => {
    return declareMaterial(executeType, ids.value.join(','))
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess(`${nameMap[executeType]}已提交，等待客户端采集传输`)
  }).catch(() => {})
}

function handleExport() {
  proxy.download('bonded/material/export', { ...queryParams.value }, `material_${new Date().getTime()}.xlsx`)
}

getList()
</script>
