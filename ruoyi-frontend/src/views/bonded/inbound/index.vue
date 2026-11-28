<template>
  <div class="p2">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="入库单号"><el-input v-model="queryParams.inboundNo" placeholder="模糊查询" clearable style="width: 180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="入库业务类型">
        <el-select v-model="queryParams.busiType" placeholder="全部" clearable multiple collapse-tags style="width: 170px">
          <el-option label="04-待维修货物" value="04" /><el-option label="05-维修用料" value="05" /><el-option label="99-其他" value="99" />
        </el-select>
      </el-form-item>
      <el-form-item label="单据状态">
        <el-select v-model="queryParams.docStatus" placeholder="全部" clearable multiple collapse-tags style="width: 150px">
          <el-option v-for="opt in docStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <template v-if="searchExpanded">
        <el-form-item label="关联申报单证类型">
          <el-select v-model="queryParams.relaCustomsFormType" placeholder="全部" clearable multiple collapse-tags style="width: 190px">
            <el-option label="01-径予放行申明" value="01" /><el-option label="02-公服入库单" value="02" />
            <el-option label="03-报关单" value="03" /><el-option label="05-异常调整" value="05" /><el-option label="99-其他" value="99" />
          </el-select>
        </el-form-item>
        <el-form-item label="海关状态">
          <el-select v-model="queryParams.customsStatus" placeholder="全部" clearable multiple collapse-tags style="width: 160px">
            <el-option v-for="c in cstOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库时间">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="-" start-placeholder="开始" end-placeholder="结束" style="width: 240px" />
        </el-form-item>
      </template>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button link type="primary" @click="searchExpanded = !searchExpanded">
          {{ searchExpanded ? '收起' : '展开' }}<el-icon class="el-icon--right"><component :is="searchExpanded ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
        </el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button></el-col>
    </el-row>
    <el-table v-loading="loading" :data="rows" @filter-change="onFilterChange">
      <el-table-column label="入库单号" prop="inboundNo" width="140" align="center">
        <template #default="s"><el-link type="primary" :underline="false" @click="handleDetail(s.row)">{{ s.row.inboundNo }}</el-link></template>
      </el-table-column>
      <el-table-column label="入库业务类型" width="120" align="center" column-key="busiType" :filters="busiTypeFilters">
        <template #default="s"><el-tag :type="{ '04': 'primary', '05': 'success', '99': 'info' }[s.row.busiType]">{{ { '04': '待维修货物', '05': '维修用料', '99': '其他' }[s.row.busiType] || s.row.busiType }}</el-tag></template>
      </el-table-column>
      <el-table-column label="物料编号" width="130" show-overflow-tooltip align="center">
        <template #default="s">{{ itemCdSummary(s.row) }}</template>
      </el-table-column>
      <el-table-column label="物料名称" min-width="170" show-overflow-tooltip align="center">
        <template #default="s">{{ itemSummary(s.row) }}</template>
      </el-table-column>
      <el-table-column label="入库时间" width="110" align="center">
        <template #default="s">
          <el-tooltip :content="fmtTimeFull(s.row.inOutTime)" placement="top"><span>{{ fmtTimeShort(s.row.inOutTime) }}</span></el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="关联申报单证类型" width="180" align="center" show-overflow-tooltip column-key="relaCustomsFormType" :filters="relaFormTypeFilters">
        <template #default="s">{{ { '01': '01-径予放行申明', '02': '02-公服入库单', '03': '03-报关单', '05': '05-异常调整', '99': '99-其他' }[s.row.relaCustomsFormType] || s.row.relaCustomsFormType }}</template>
      </el-table-column>
      <el-table-column label="单据状态" width="110" align="center" column-key="docStatus" :filters="docStatusFilters">
        <template #default="s">
          <el-tag :type="docStatusType(s.row.docStatus, s.row.customsStatus)" :icon="docStatusIcon(s.row.docStatus, s.row.customsStatus)">
            {{ docStatusLabel(s.row.docStatus, s.row.customsStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="海关状态" width="110" align="center" column-key="customsStatus" :filters="customsFilters">
        <template #default="s"><el-tag :type="cstType(s.row.customsStatus)" :icon="cstIcon(s.row.customsStatus)" style="cursor:pointer" @click="openReceipt(s.row)">{{ cstLabel(s.row.customsStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="250" align="center" fixed="right">
        <template #default="s">
          <el-button v-if="canExec(s.row, '1')" link type="success" icon="Stamp" v-hasPermi="['bonded:in:declare']" @click="handleDeclare('1', s.row)">备案</el-button>
          <el-button v-if="canExec(s.row, '2')" link type="warning" icon="EditPen" v-hasPermi="['bonded:in:declare']" @click="handleDeclare('2', s.row)">变更</el-button>
          <el-button v-if="canExec(s.row, '3')" link type="danger" icon="Delete" v-hasPermi="['bonded:in:declare']" @click="handleDeclare('3', s.row)">作废</el-button>
          <el-button link type="primary" icon="Edit" v-hasPermi="['bonded:in:edit']" :disabled="!(s.row.docStatus === '01' || s.row.docStatus === '06' || (s.row.docStatus === '03' && (s.row.customsStatus === '00' || s.row.customsStatus === '10')))" @click="handleEdit(s.row)">修改</el-button>
          <el-button link type="danger" icon="Delete" v-hasPermi="['bonded:in:remove']" :disabled="!(s.row.docStatus === '01' || (s.row.docStatus === '03' && s.row.customsStatus === '00'))" @click="handleDelete(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <DeclareReceiptDialog v-model="receiptVisible" biz-table="br_inbound" :biz-id="receiptBizId" :biz-no="receiptBizNo" />
  </div>
</template>

<script setup name="Inbound">
import { listInbound, delInbound, declareInbound } from '@/api/bonded/biz'
import { cstLabel, cstType, cstIcon, customsStatusMap, docStatusLabel, docStatusOptions, docStatusType, docStatusIcon, allowedExecTypes, fmtTimeShort, fmtTimeFull, enumToFilters, applyFilterChange, packListQuery } from '@/utils/bonded'
import DeclareReceiptDialog from '../components/DeclareReceiptDialog.vue'
import { addDateRange } from '@/utils/ruoyi'
const { proxy } = getCurrentInstance()
const router = useRouter()
const rows = ref([]); const loading = ref(false); const total = ref(0)
// 行内申报按钮联动：按单据状态决定是否显示 备案(1)/变更(2)/作废(3)
const canExec = (row, t) => allowedExecTypes(row).includes(t)
const dateRange = ref([])
const searchExpanded = ref(false)
const cstOptions = Object.entries(customsStatusMap).map(([value, v]) => ({ value, label: v.label }))
const busiTypeFilters = enumToFilters([{ value: '04', label: '04-待维修货物' }, { value: '05', label: '05-维修用料' }, { value: '99', label: '99-其他' }])
const relaFormTypeFilters = enumToFilters([
  { value: '01', label: '01-径予放行申明' }, { value: '02', label: '02-公服入库单' }, { value: '03', label: '03-报关单' },
  { value: '05', label: '05-异常调整' }, { value: '99', label: '99-其他' }
])
const docStatusFilters = enumToFilters(docStatusOptions)
const customsFilters = enumToFilters(customsStatusMap)
const queryParams = reactive({ pageNum: 1, pageSize: 10, inboundNo: null, busiType: [], relaCustomsFormType: [], docStatus: [], customsStatus: [] })

function getList() {
  loading.value = true
  const params = addDateRange(packListQuery(queryParams), dateRange.value, 'InoutTime')
  listInbound(params).then(r => {
    rows.value = r.rows; total.value = r.total; loading.value = false
  })
}
function handleQuery() { queryParams.pageNum = 1; getList() }
function onFilterChange(filters) { applyFilterChange(queryParams, filters); handleQuery() }
function resetQuery() {
  Object.assign(queryParams, { pageNum: 1, pageSize: 10, inboundNo: null, busiType: [], relaCustomsFormType: [], docStatus: [], customsStatus: [] })
  dateRange.value = []
  handleQuery()
}

// ===== 行操作（保存即生效：单据保存成功即登记台账并按开关写入镜像库，无独立审核环节） =====

// 明细摘要（列表接口已带 items：materialCd/materialName/storageCnt/price/currency/specificationsModels）
function itemSummary(row) {
  const items = row.items || []
  const first = items[0]?.materialName || ''
  return items.length > 1 ? first + ' 等 ' + items.length + ' 行' : (first || '-')
}
function itemCdSummary(row) {
  const items = row.items || []
  const first = items[0]?.materialCd || ''
  return items.length > 1 ? first + ' 等 ' + items.length + ' 项' : (first || '-')
}

function handleAdd() { router.push('/bonded/inbound-form/index') }
function handleEdit(row) { router.push('/bonded/inbound-form/index/' + row.id) }
function handleDetail(row) { router.push('/bonded/inbound-detail/index/' + row.id) }
function handleDeclare(executeType, row) {
  const nameMap = { '1': '备案申请', '2': '变更申请', '3': '作废申报' }
  proxy.$modal.confirm(`确认对入库单【${row.inboundNo}】执行【${nameMap[executeType]}】？申报数据将写入海关镜像表。`).then(() => declareInbound(executeType, row.id)).then(() => { getList(); proxy.$modal.msgSuccess(`${nameMap[executeType]}已提交`) }).catch(() => {})
}
function handleDelete(row) {
  const deletable = row.docStatus === '01' || (row.docStatus === '03' && row.customsStatus === '00')
  if (!deletable) return proxy.$modal.msgError('单据已写入海关镜像库，不可删除，请走变更或作废申报流程')
  proxy.$modal.confirm(`确认删除入库单【${row.inboundNo}】？生效未申报单据删除时将自动冲销已登记的库存台账。`).then(() => delInbound(row.id)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
const receiptVisible = ref(false)
const receiptBizId = ref(null)
const receiptBizNo = ref('')
function openReceipt(row) {
  receiptBizId.value = row.id
  receiptBizNo.value = row.inboundNo
  receiptVisible.value = true
}

getList()
</script>
