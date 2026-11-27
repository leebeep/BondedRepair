<template>
  <div class="p2">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd">新增发料单</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" v-hasPermi="['bonded:issue:declare']" :disabled="!ids.length" @click="handleVoid('3')">作废申报</el-button></el-col>
    </el-row>
    <el-table v-loading="loading" :data="rows" @selection-change="s => ids = s.map(i => i.id)">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="发料单号" prop="issueNo" width="140" />
      <el-table-column label="工单号" prop="workNo" width="150" />
      <el-table-column label="发料日期" prop="issueTime" width="160" />
      <el-table-column label="单据状态" width="100" align="center">
        <template #default="s">
          <el-tag :type="{ '01': 'info', '02': 'warning', '03': 'primary', '05': 'success', '06': 'danger', '07': 'info' }[s.row.docStatus]">
            {{ { '01': '草稿', '02': '待审核', '03': '申报中', '05': '申报通过', '06': '退单', '07': '已作废' }[s.row.docStatus] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="海关状态" width="100" align="center">
        <template #default="s"><el-tag :type="cstType(s.row.customsStatus)">{{ cstLabel(s.row.customsStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center">
        <template #default="s">
          <el-button link type="primary" icon="View" @click="handleDetail(s.row)">详情</el-button>
          <el-button link type="primary" icon="Edit" :disabled="s.row.docStatus !== '01' && s.row.docStatus !== '06'" @click="handleEdit(s.row)">修改</el-button>
          <el-button link type="warning" icon="Stamp" v-hasPermi="['bonded:issue:audit']" :disabled="s.row.docStatus !== '01'" @click="handleAudit(s.row)">审核</el-button>
          <el-button link type="danger" icon="Delete" v-hasPermi="['bonded:issue:remove']" :disabled="s.row.docStatus !== '01'" @click="handleDelete(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="Issue">
import { listIssue, delIssue, auditIssue, declareIssue } from '@/api/bonded/biz'
import { cstLabel, cstType } from '@/utils/bonded'
const { proxy } = getCurrentInstance()
const router = useRouter()
const rows = ref([]); const loading = ref(false); const total = ref(0); const ids = ref([])
const queryParams = reactive({ pageNum: 1, pageSize: 10 })
function getList() { loading.value = true; listIssue(queryParams).then(r => { rows.value = r.rows; total.value = r.total; loading.value = false }) }
function handleAdd() { router.push('/bonded/issue-form/index') }
function handleEdit(row) { router.push('/bonded/issue-form/index/' + row.id) }
function handleDetail(row) { router.push('/bonded/issue-detail/index/' + row.id) }
function handleAudit(row) { proxy.$modal.confirm('审核将校验来源余额、登记台账并自动写入海关镜像库（自动推送开启时），确认？').then(() => auditIssue(row.id)).then(() => { getList(); proxy.$modal.msgSuccess('审核通过') }).catch(() => {}) }
function handleVoid(t) {
  if (!ids.value.length) { proxy.$modal.msgWarning('请勾选发料单'); return }
  proxy.$modal.confirm('将向海关发送作废申报（executeType=3），需为申报中/申报通过/退单状态，确认？').then(() => declareIssue(t, ids.value.join(','))).then(() => { getList(); proxy.$modal.msgSuccess('作废申报已提交') }).catch(() => {})
}
function handleDelete(row) { proxy.$modal.confirm('确认删除？').then(() => delIssue(row.id)).then(() => { getList() }).catch(() => {}) }
getList()
</script>
