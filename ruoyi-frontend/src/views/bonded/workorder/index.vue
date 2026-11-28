<template>
  <div class="p2">
    <el-form :inline="true" :model="queryParams" class="mb8">
      <el-form-item label="工单号"><el-input v-model="queryParams.workNo" placeholder="模糊查询" clearable style="width: 180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.orderStatus" placeholder="全部" clearable multiple collapse-tags style="width: 160px">
          <el-option v-for="(v, k) in woStatusMap" :key="k" :label="v.label" :value="k" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" v-hasPermi="['bonded:wo:add']" @click="handleAdd">新建工单</el-button></el-col>
    </el-row>
    <el-table v-loading="loading" :data="rows" @filter-change="onFilterChange">
      <el-table-column label="工单号" prop="workNo" width="180" align="center">
        <template #default="s"><el-link type="primary" :underline="false" @click="handleDetail(s.row)">{{ s.row.workNo }}</el-link></template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center" column-key="orderStatus" :filters="statusFilters">
        <template #default="s">
          <el-tag :type="woStatusMap[s.row.orderStatus]?.type || 'info'">{{ woStatusMap[s.row.orderStatus]?.label || s.row.orderStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发料合计" width="110" align="center"><template #default="s">{{ fmt(s.row.issuedQty) }}</template></el-table-column>
      <el-table-column label="收回合计" width="110" align="center"><template #default="s">{{ fmt(s.row.receivedQty) }}</template></el-table-column>
      <el-table-column label="维修服务费" width="140" align="center">
        <template #default="s">{{ s.row.serviceFee != null && Number(s.row.serviceFee) > 0 ? s.row.serviceFee + '（' + (s.row.currency || '-') + '）' : '—' }}</template>
      </el-table-column>
      <el-table-column label="计划完工日期" width="120" align="center">
        <template #default="s">{{ (s.row.finishDate || '').slice(0, 10) || '-' }}</template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip align="center">
        <template #default="s">{{ s.row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="s">
          <el-button v-if="s.row.orderStatus !== '05'" link type="primary" icon="Edit" v-hasPermi="['bonded:wo:edit']" @click="handleEdit(s.row)">编辑</el-button>
          <el-button v-if="s.row.orderStatus === '04'" link type="warning" icon="Stamp" v-hasPermi="['bonded:wo:edit']" @click="handleClose(s.row)">结案</el-button>
          <el-button v-if="s.row.orderStatus === '01'" link type="danger" icon="Delete" v-hasPermi="['bonded:wo:remove']" @click="handleDelete(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="工单号" prop="workNo">
          <el-input v-model="form.workNo" :disabled="!!form.id" placeholder="已自动生成，可修改" />
          <div class="field-tip" v-if="!form.id">按 参数设置[br.workorder.no.prefix] 自动生成（前缀+日期+3位流水）；保存后不可修改</div>
        </el-form-item>
        <el-form-item label="计划完工日期" prop="finishDate"><el-date-picker v-model="form.finishDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WorkOrder">
import { listWorkOrder, getWorkOrder, addWorkOrder, updWorkOrder, delWorkOrder, nextWorkOrderNo, closeWorkOrder } from '@/api/bonded/workorder'
import { enumToFilters, applyFilterChange, packListQuery } from '@/utils/bonded'
const { proxy } = getCurrentInstance()
const router = useRouter()

const woStatusMap = {
  '01': { label: '01 立项', type: 'info' },
  '02': { label: '02 维修中', type: 'primary' },
  '03': { label: '03 已收齐', type: 'success' },
  '04': { label: '04 已出清', type: 'warning' },
  '05': { label: '05 已结案', type: 'danger' }
}
const statusFilters = enumToFilters(woStatusMap)

const rows = ref([]); const loading = ref(false); const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, workNo: null, orderStatus: [] })
function fmt(v) { const n = Number(v); return Number.isFinite(n) && n !== 0 ? parseFloat(n.toFixed(6)) : (n === 0 ? '0' : '-') }

function getList() { loading.value = true; listWorkOrder(packListQuery(queryParams)).then(r => { rows.value = r.rows; total.value = r.total; loading.value = false }) }
function handleQuery() { queryParams.pageNum = 1; getList() }
function onFilterChange(filters) { applyFilterChange(queryParams, filters); handleQuery() }
function resetQuery() {
  Object.assign(queryParams, { pageNum: 1, pageSize: 10, workNo: null, orderStatus: [] })
  handleQuery()
}

// ===== 新建/编辑 =====
const dialogVisible = ref(false)
const dialogTitle = ref('新建维修工单')
const formRef = ref()
const form = ref({ id: null, workNo: null, finishDate: null, remark: null })
const rules = { workNo: [{ required: true, message: '工单号必填', trigger: 'blur' }] }

function handleAdd() {
  form.value = { id: null, workNo: null, finishDate: null, remark: null }
  dialogTitle.value = '新建维修工单'
  nextWorkOrderNo().then(r => { form.value.workNo = r.data }).catch(() => {})
  dialogVisible.value = true
}
function handleEdit(row) {
  form.value = { id: row.id, workNo: row.workNo, finishDate: (row.finishDate || '').slice(0, 10) || null, remark: row.remark }
  dialogTitle.value = '编辑维修工单'
  dialogVisible.value = true
}
function submitForm() {
  proxy.$refs.formRef.validate(v => {
    if (!v) return
    const req = form.value.id ? updWorkOrder(form.value) : addWorkOrder(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess('保存成功')
      dialogVisible.value = false
      getList()
    })
  })
}
function handleClose(row) {
  proxy.$modal.confirm(`确认结案工单【${row.workNo}】？结案后不可再关联发料/收料。`).then(() => closeWorkOrder(row.id)).then(() => { getList(); proxy.$modal.msgSuccess('已结案') }).catch(() => {})
}
function handleDelete(row) {
  proxy.$modal.confirm(`确认删除工单【${row.workNo}】？已关联发料/收料单的工单不可删除。`).then(() => delWorkOrder(row.id)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

function handleDetail(row) { router.push('/bonded/workorder-detail/index/' + row.id) }

getList()
</script>

<style scoped>
.field-tip { width: 100%; font-size: 12px; line-height: 1.4; color: #909399; }
</style>
