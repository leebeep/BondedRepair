<template>
  <div class="p2">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="出库单号"><el-input v-model="queryParams.outboundNo" placeholder="模糊查询" clearable style="width: 180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="业务类型">
        <el-select v-model="queryParams.busiType" placeholder="全部" clearable style="width: 170px">
          <el-option v-for="t in busiTypes" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="单据状态">
        <el-select v-model="queryParams.docStatus" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="opt in docStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="海关状态">
        <el-select v-model="queryParams.customsStatus" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="c in cstOptions" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" v-hasPermi="['bonded:out:add']" @click="handleAdd">新增</el-button></el-col>
    </el-row>
    <el-table v-loading="loading" :data="rows">
      <el-table-column label="出库单号" prop="outboundNo" width="160" header-align="center" />
      <el-table-column label="出库业务类型" width="160" align="center" header-align="center">
        <template #default="s"><el-tag :type="{ '03': 'info', '04': 'primary', '05': 'success', '99': 'info' }[s.row.busiType]">{{ busiTypes.find(t => t.value === s.row.busiType)?.label || s.row.busiType }}</el-tag></template>
      </el-table-column>
      <el-table-column label="物料编号" width="130" header-align="center" show-overflow-tooltip>
        <template #default="s">
          <el-tooltip v-if="(s.row.items || []).length" placement="top" effect="light">
            <template #content>
              <div v-for="(it, i) in s.row.items" :key="i">{{ itemCd(it) }}　{{ materialNameWithMark(it.materialName, it.markType, it.isBonded) }}</div>
            </template>
            <span style="cursor: default">{{ itemCdSummary(s.row) }}</span>
          </el-tooltip>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="物料名称" min-width="170" header-align="center" show-overflow-tooltip>
        <template #default="s">
          <el-tooltip v-if="(s.row.items || []).length" placement="top" effect="light">
            <template #content>
              <div v-for="(it, i) in s.row.items" :key="i">{{ materialNameWithMark(it.materialName, it.markType, it.isBonded) }}　出库数量 {{ fmtNum(it.deliveryCnt) }}　单价 {{ fmtNum(it.price) }} {{ it.currency }}</div>
            </template>
            <span style="cursor: default">{{ itemSummary(s.row) }}</span>
          </el-tooltip>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="出库时间" prop="inOutTime" width="160" header-align="center" />
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
      <el-table-column label="预录入编号" prop="lastUuid" width="170" header-align="center" show-overflow-tooltip>
        <template #default="s">{{ s.row.lastUuid || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="320" align="center" fixed="right">
        <template #default="s">
          <el-button link type="primary" icon="View" @click="handleDetail(s.row)">详情</el-button>
          <el-button link type="primary" icon="Edit" v-hasPermi="['bonded:out:edit']" :disabled="s.row.docStatus !== '01' && s.row.docStatus !== '06'" @click="handleEdit(s.row)">修改</el-button>
          <el-button link type="warning" icon="Stamp" v-hasPermi="['bonded:out:audit']" :disabled="!['01', '02', '06'].includes(s.row.docStatus)" @click="handleAudit(s.row)">审核</el-button>
          <el-button v-if="canExec(s.row, '1')" link type="success" icon="Stamp" v-hasPermi="['bonded:out:declare']" @click="handleDeclare('1', s.row)">备案</el-button>
          <el-button v-if="canExec(s.row, '2')" link type="warning" icon="EditPen" v-hasPermi="['bonded:out:declare']" @click="handleDeclare('2', s.row)">变更</el-button>
          <el-button v-if="canExec(s.row, '3')" link type="danger" icon="Delete" v-hasPermi="['bonded:out:declare']" @click="handleDeclare('3', s.row)">作废</el-button>
          <el-button link type="danger" icon="Delete" v-hasPermi="['bonded:out:remove']" :disabled="s.row.docStatus !== '01'" @click="handleDelete(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <DeclareReceiptDialog v-model="receiptVisible" biz-table="br_outbound" :biz-id="receiptBizId" :biz-no="receiptBizNo" />
  </div>
</template>

<script setup name="Outbound">
import { listOutbound, delOutbound, auditOutbound, declareOutbound } from '@/api/bonded/biz'
import { listMaterial } from '@/api/bonded/material'
import { cstLabel, cstType, customsStatusMap, docStatusLabel, docStatusOptions, docStatusType, outboundBusiTypes as busiTypes, materialNameWithMark, allowedExecTypes } from '@/utils/bonded'
import DeclareReceiptDialog from '../components/DeclareReceiptDialog.vue'
const { proxy } = getCurrentInstance()
const router = useRouter()
const rows = ref([]); const loading = ref(false); const total = ref(0)
// 行内申报按钮联动：按单据状态决定是否显示 备案(1)/变更(2)/作废(3)
const canExec = (row, t) => allowedExecTypes(row).includes(t)
const cstOptions = Object.entries(customsStatusMap).map(([value, v]) => ({ value, label: v.label }))
const materialCds = ref({})
const queryParams = reactive({ pageNum: 1, pageSize: 10, outboundNo: null, busiType: null, docStatus: null, customsStatus: null })
listMaterial({ pageNum: 1, pageSize: 500, declareStatus: '2' }).then(r => {
  materialCds.value = (r.rows || []).reduce((m, item) => { m[item.id] = item.materialCd; return m }, {})
})

function getList() { loading.value = true; listOutbound(queryParams).then(r => { rows.value = r.rows; total.value = r.total; loading.value = false }) }
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() {
  Object.assign(queryParams, { pageNum: 1, pageSize: 10, outboundNo: null, busiType: null, docStatus: null, customsStatus: null })
  handleQuery()
}

// 列表接口已带 items：materialId/materialName/deliveryCnt/price/currency；物料编号由物料档案补齐
function itemCd(it) { return it.materialCd || materialCds.value[it.materialId] || '-' }
function itemSummary(row) {
  const items = row.items || []
  const first = materialNameWithMark(items[0]?.materialName, items[0]?.markType, items[0]?.isBonded) || ''
  return items.length > 1 ? first + ' 等 ' + items.length + ' 行' : (first || '-')
}
function itemCdSummary(row) {
  const items = row.items || []
  const first = items.length ? itemCd(items[0]) : ''
  return items.length > 1 ? first + ' 等 ' + items.length + ' 项' : (first || '-')
}
function fmtNum(n) { return n == null || isNaN(n) ? '-' : String(parseFloat(Number(n).toFixed(6))) }

function handleAdd() { router.push('/bonded/outbound-form/index') }
function handleEdit(row) { router.push('/bonded/outbound-form/index/' + row.id) }
function handleDetail(row) { router.push('/bonded/outbound-detail/index/' + row.id) }
function handleAudit(row) { proxy.$modal.confirm('审核将校验来源余额、登记出库台账并自动写入海关镜像库（自动申报开启时），确认？').then(() => auditOutbound(row.id)).then(() => { getList(); proxy.$modal.msgSuccess('审核通过') }).catch(() => {}) }
function handleDeclare(executeType, row) {
  const nameMap = { '1': '备案申请', '2': '变更申请', '3': '作废申报' }
  proxy.$modal.confirm(`确认对出库单【${row.outboundNo}】执行【${nameMap[executeType]}】？申报数据将写入海关镜像表。`).then(() => declareOutbound(executeType, row.id)).then(() => { getList(); proxy.$modal.msgSuccess(`${nameMap[executeType]}已提交`) }).catch(() => {})
}
function handleDelete(row) { proxy.$modal.confirm('确认删除？').then(() => delOutbound(row.id)).then(() => { getList() }).catch(() => {}) }
const receiptVisible = ref(false)
const receiptBizId = ref(null)
const receiptBizNo = ref('')
function openReceipt(row) {
  receiptBizId.value = row.id
  receiptBizNo.value = row.outboundNo
  receiptVisible.value = true
}

getList()
</script>
