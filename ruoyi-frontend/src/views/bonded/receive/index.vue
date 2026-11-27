<template>
  <div class="p2">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd">新增收料单</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Stamp" v-hasPermi="['bonded:rcv:declare']" @click="handleDeclare('1')">收料申报(MSO2)</el-button></el-col>
    </el-row>
    <el-table v-loading="loading" :data="rows" @selection-change="s => ids = s.map(i => i.id)">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="收料单号" prop="receiveNo" width="160" />
      <el-table-column label="工单号" prop="workNo" width="150" />
      <el-table-column label="收料日期" prop="receiveTime" width="160" />
      <el-table-column label="单据状态" width="100" align="center">
        <template #default="s">
          <el-tag :type="{ '01': 'info', '03': 'primary', '04': 'warning', '05': 'success', '06': 'danger' }[s.row.docStatus]">
            {{ { '01': '草稿', '03': '审核通过', '04': '已申报', '05': '申报通过', '06': '退单' }[s.row.docStatus] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="海关状态" width="100" align="center">
        <template #default="s"><el-tag :type="cstType(s.row.customsStatus)">{{ cstLabel(s.row.customsStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center">
        <template #default="s">
          <el-button link type="primary" icon="View" @click="handleDetail(s.row)">详情</el-button>
          <el-button link type="primary" icon="Edit" :disabled="s.row.docStatus !== '01'" @click="handleEdit(s.row)">修改</el-button>
          <el-button link type="warning" icon="Stamp" v-hasPermi="['bonded:rcv:audit']" :disabled="s.row.docStatus !== '01'" @click="handleAudit(s.row)">审核</el-button>
          <el-button link type="danger" icon="Delete" v-hasPermi="['bonded:rcv:remove']" :disabled="s.row.docStatus !== '01'" @click="handleDelete(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="Receive">
import { listReceive, delReceive, auditReceive, declareReceive } from '@/api/bonded/biz'
import { cstLabel, cstType } from '@/utils/bonded'
const { proxy } = getCurrentInstance()
const router = useRouter()
const rows = ref([]); const loading = ref(false); const total = ref(0); const ids = ref([])
const queryParams = reactive({ pageNum: 1, pageSize: 10 })
function getList() { loading.value = true; listReceive(queryParams).then(r => { rows.value = r.rows; total.value = r.total; loading.value = false }) }
function handleAdd() { router.push('/bonded/receive-form/index') }
function handleEdit(row) { router.push('/bonded/receive-form/index/' + row.id) }
function handleDetail(row) { router.push('/bonded/receive-detail/index/' + row.id) }
function handleAudit(row) { proxy.$modal.confirm('审核将登记台账并校验收齐情况，确认？').then(() => auditReceive(row.id)).then(() => { getList(); proxy.$modal.msgSuccess('审核通过') }).catch(() => {}) }
function handleDeclare(t) {
  if (!ids.value.length) { proxy.$modal.msgWarning('请勾选收料单'); return }
  proxy.$modal.confirm('申报数据将写入海关镜像表 mso2_data，确认？').then(() => declareReceive(t, ids.value.join(','))).then(() => { getList(); proxy.$modal.msgSuccess('申报已提交') }).catch(() => {})
}
function handleDelete(row) { proxy.$modal.confirm('确认删除？').then(() => delReceive(row.id)).then(() => { getList() }).catch(() => {}) }
getList()
</script>
