<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row"><span>{{ title }}</span><el-button link type="primary" icon="Back" @click="cancel">返回列表</el-button></div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="12"><el-form-item label="发料单号" prop="issueNo"><el-input v-model="form.issueNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="工单号" prop="workNo"><el-input v-model="form.workNo" placeholder="如 WO-2026-001，不存在将自动建档" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="发料日期" prop="issueTime"><el-date-picker v-model="form.issueTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="申报批次号"><el-input v-model="form.batchNo" placeholder="如 B202611-001，用于台账与对账" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="发料明细" required>
          <el-table :data="form.items" size="small" border style="width: 100%">
            <el-table-column label="物料">
              <template #default="s">
                <el-select v-model="s.row.materialId" filterable size="small">
                  <el-option v-for="m in materials" :key="m.id" :label="m.materialName + '（' + m.materialCd + '）'" :value="m.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="来源类型" width="130">
              <template #default="s">
                <el-select v-model="s.row.goodsSourceType" size="small" @change="s.row.goodsSourceNo = null">
                  <el-option label="g-入库清单" value="g" /><el-option label="i-工单收料" value="i" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="来源单号" width="180">
              <template #default="s">
                <el-select v-model="s.row.goodsSourceNo" filterable size="small" :disabled="!s.row.goodsSourceType">
                  <el-option v-for="no in sourceNos(s.row.goodsSourceType)" :key="no" :label="no" :value="no" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="发料数量" width="150"><template #default="s"><el-input-number v-model="s.row.issueQty" :min="0.000001" :precision="6" size="small" style="width:100%" /></template></el-table-column>
            <el-table-column label="单价" width="130"><template #default="s"><el-input-number v-model="s.row.price" :min="0" :precision="6" size="small" style="width:100%" /></template></el-table-column>
            <el-table-column label="操作" width="70" align="center"><template #default="s"><el-button link type="danger" @click="form.items.splice(s.$index,1)">删</el-button></template></el-table-column>
          </el-table>
          <el-button class="mt4" size="small" icon="Plus" @click="form.items.push({ goodsSourceType: 'g' })">添加明细</el-button>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon title="审核时校验来源单据可发余额；工单号由本单录入并自动建档（内部支撑数据，随 RRD2 报文 work_no 报送）" />
      </el-form>
    </el-card>
    <el-row justify="end" class="mt16">
      <el-button type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="cancel">取 消</el-button>
    </el-row>
  </div>
</template>

<script setup name="IssueForm">
import { getIssue, addIssue, updIssue, listInbound, listReceive } from '@/api/bonded/biz'
import { listMaterial } from '@/api/bonded/material'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const materials = ref([]); const inboundNos = ref([]); const receiveNos = ref([])
const form = ref({ issueNo: null, workNo: null, batchNo: null, issueTime: null, items: [{ goodsSourceType: 'g' }] })
const rules = {
  issueNo: [{ required: true, message: '发料单号必填', trigger: 'blur' }],
  workNo: [{ required: true, message: '工单号必填', trigger: 'blur' }],
  issueTime: [{ required: true, message: '发料日期必填', trigger: 'change' }]
}

const id = route.params.id
const title = ref(id ? '修改发料单' : '新增发料单')

function sourceNos(type) { return type === 'g' ? inboundNos.value : type === 'i' ? receiveNos.value : [] }

if (id) {
  getIssue(id).then(r => { form.value = r.data })
}
listInbound({ pageNum: 1, pageSize: 100, docStatus: '03' }).then(r => { inboundNos.value = r.rows.map(x => x.inboundNo) })
listReceive({ pageNum: 1, pageSize: 100, docStatus: '03' }).then(r => { receiveNos.value = r.rows.map(x => x.receiveNo) })
listMaterial({ pageNum: 1, pageSize: 200, declareStatus: '2' }).then(r => { materials.value = r.rows })

function submitForm() {
  proxy.$refs.formRef.validate(v => {
    if (!v) return
    if (!form.value.items.length || form.value.items.some(i => !i.materialId || !i.goodsSourceNo || !i.issueQty)) { proxy.$modal.msgError('请完善发料明细'); return }
    ;(form.value.id ? updIssue(form.value) : addIssue(form.value)).then(() => {
      proxy.$modal.msgSuccess('保存成功')
      proxy.$tab.closeOpenPage({ path: '/bonded/issue' })
    })
  })
}

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/issue' })
}
</script>
