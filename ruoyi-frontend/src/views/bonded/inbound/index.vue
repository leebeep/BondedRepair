<template>
  <div class="p2">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="入库单号"><el-input v-model="queryParams.inboundNo" placeholder="模糊查询" clearable style="width: 180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="入库业务类型">
        <el-select v-model="queryParams.busiType" placeholder="全部" clearable style="width: 140px">
          <el-option label="04-待维修货物" value="04" /><el-option label="05-维修用料" value="05" /><el-option label="99-其他" value="99" />
        </el-select>
      </el-form-item>
      <el-form-item label="关联申报单证类型">
        <el-select v-model="queryParams.relaCustomsFormType" placeholder="全部" clearable style="width: 160px">
          <el-option label="01-径予放行申明" value="01" /><el-option label="02-公服入库单" value="02" />
          <el-option label="03-报关单" value="03" /><el-option label="05-异常调整" value="05" /><el-option label="99-其他" value="99" />
        </el-select>
      </el-form-item>
      <el-form-item label="单据状态">
        <el-select v-model="queryParams.docStatus" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="opt in docStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="海关状态">
        <el-select v-model="queryParams.customsStatus" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="c in cstOptions" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="入库时间">
        <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="-" start-placeholder="开始" end-placeholder="结束" style="width: 240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Stamp" v-hasPermi="['bonded:in:declare']" :disabled="!ids.length" @click="handleDeclare('1')">备案申请</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="EditPen" v-hasPermi="['bonded:in:declare']" :disabled="!ids.length" @click="handleDeclare('2')">变更申请</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" v-hasPermi="['bonded:in:declare']" :disabled="!ids.length" @click="handleDeclare('3')">作废申报</el-button></el-col>
    </el-row>
    <el-table v-loading="loading" :data="rows" @selection-change="s => ids = s.map(i => i.id)">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="入库单号" prop="inboundNo" width="140" />
      <el-table-column label="入库业务类型" width="120" align="center">
        <template #default="s"><el-tag :type="{ '04': 'primary', '05': 'success', '99': 'info' }[s.row.busiType]">{{ { '04': '待维修货物', '05': '维修用料', '99': '其他' }[s.row.busiType] || s.row.busiType }}</el-tag></template>
      </el-table-column>
      <el-table-column label="关联申报单证类型" width="140" align="center" show-overflow-tooltip>
        <template #default="s">{{ { '01': '01-径予放行申明', '02': '02-公服入库单', '03': '03-报关单', '05': '05-异常调整', '99': '99-其他' }[s.row.relaCustomsFormType] || s.row.relaCustomsFormType }}</template>
      </el-table-column>
      <el-table-column label="关联申报单证编号" prop="relaCustomsFormNum" width="150" show-overflow-tooltip />
      <el-table-column label="物料编号" width="130" show-overflow-tooltip>
        <template #default="s">
          <el-tooltip v-if="(s.row.items || []).length" placement="top" effect="light">
            <template #content>
              <div v-for="(it, i) in s.row.items" :key="i">{{ it.materialCd }}　{{ it.materialName }}</div>
            </template>
            <span style="cursor: default">{{ itemCdSummary(s.row) }}</span>
          </el-tooltip>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="物料名称" min-width="170" show-overflow-tooltip>
        <template #default="s">
          <el-tooltip v-if="(s.row.items || []).length" placement="top" effect="light">
            <template #content>
              <div v-for="(it, i) in s.row.items" :key="i">{{ it.materialName }}　数量 {{ fmtNum(it.storageCnt) }}　单价 {{ fmtNum(it.price) }} {{ it.currency }}</div>
            </template>
            <span style="cursor: default">{{ itemSummary(s.row) }}</span>
          </el-tooltip>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="规格型号" width="140" show-overflow-tooltip>
        <template #default="s">{{ firstItem(s.row).specificationsModels || '-' }}</template>
      </el-table-column>
      <el-table-column label="入库时间" prop="inOutTime" width="160" />
      <el-table-column label="单据状态" width="100" align="center">
        <template #default="s">
          <el-tag :type="docStatusType(s.row.docStatus, s.row.customsStatus)">
            {{ docStatusLabel(s.row.docStatus, s.row.customsStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="海关状态" width="100" align="center">
        <template #default="s"><el-tag :type="cstType(s.row.customsStatus)" style="cursor:pointer" @click="openReceipt(s.row)">{{ cstLabel(s.row.customsStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="s">
          <el-button link type="primary" icon="View" @click="handleDetail(s.row)">详情</el-button>
          <el-button link type="primary" icon="Edit" :disabled="s.row.docStatus !== '01' && s.row.docStatus !== '06'" @click="handleEdit(s.row)">修改</el-button>
          <el-button link type="warning" icon="Stamp" v-hasPermi="['bonded:in:audit']" :disabled="s.row.docStatus !== '01'" @click="handleAudit(s.row)">审核</el-button>
          <el-button link type="danger" icon="Delete" v-hasPermi="['bonded:in:remove']" :disabled="s.row.docStatus !== '01'" @click="handleDelete(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <DeclareReceiptDialog v-model="receiptVisible" biz-table="br_inbound" :biz-id="receiptBizId" :biz-no="receiptBizNo" />
  </div>
</template>

<script setup name="Inbound">
import { listInbound, delInbound, auditInbound, declareInbound } from '@/api/bonded/biz'
import { cstLabel, cstType, customsStatusMap, docStatusLabel, docStatusOptions, docStatusType } from '@/utils/bonded'
import DeclareReceiptDialog from '../components/DeclareReceiptDialog.vue'
import { addDateRange } from '@/utils/ruoyi'
const { proxy } = getCurrentInstance()
const router = useRouter()
const rows = ref([]); const loading = ref(false); const total = ref(0); const ids = ref([])
const dateRange = ref([])
const cstOptions = Object.entries(customsStatusMap).map(([value, v]) => ({ value, label: v.label }))
const queryParams = reactive({ pageNum: 1, pageSize: 10, inboundNo: null, busiType: null, relaCustomsFormType: null, docStatus: null, customsStatus: null })

function getList() {
  loading.value = true
  listInbound(addDateRange(queryParams, dateRange.value, 'InoutTime')).then(r => {
    rows.value = r.rows; total.value = r.total; loading.value = false
  })
}
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { dateRange.value = []; proxy.resetForm('queryParams'); handleQuery() }

// 明细摘要（列表接口已带 items：materialCd/materialName/storageCnt/price/currency/specificationsModels）
function firstItem(row) { return (row.items || [])[0] || {} }
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
function fmtNum(n) { return n == null || isNaN(n) ? '-' : String(parseFloat(Number(n).toFixed(6))) }

function handleAdd() { router.push('/bonded/inbound-form/index') }
function handleEdit(row) { router.push('/bonded/inbound-form/index/' + row.id) }
function handleDetail(row) { router.push('/bonded/inbound-detail/index/' + row.id) }
function handleAudit(row) { proxy.$modal.confirm('审核通过后登记库存台账，并将申报数据自动写入海关镜像库（自动推送开启时），确认？').then(() => auditInbound(row.id)).then(() => { getList(); proxy.$modal.msgSuccess('审核通过') }).catch(() => {}) }
function handleDeclare(executeType) {
  const nameMap = { '1': '备案申请', '2': '变更申请', '3': '作废申报' }
  if (!ids.value.length) { proxy.$modal.msgWarning('请勾选入库单'); return }
  proxy.$modal.confirm(`确认对选中的 ${ids.value.length} 条入库单执行【${nameMap[executeType]}】？申报数据将写入海关镜像表。`).then(() => declareInbound(executeType, ids.value.join(','))).then(() => { getList(); proxy.$modal.msgSuccess(`${nameMap[executeType]}已提交`) }).catch(() => {})
}
function handleDelete(row) { proxy.$modal.confirm('确认删除？').then(() => delInbound(row.id)).then(() => { getList() }).catch(() => {}) }
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
