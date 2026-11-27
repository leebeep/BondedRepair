<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row"><span>{{ title }}</span><el-button link type="primary" icon="Back" @click="cancel">返回列表</el-button></div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="12"><el-form-item label="收料单号" prop="receiveNo"><el-input v-model="form.receiveNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="工单号" prop="workNo"><el-input v-model="form.workNo" placeholder="如 WO-2026-001，不存在将自动建档" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="收料日期" prop="receiveTime"><el-date-picker v-model="form.receiveTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="申报批次号"><el-input v-model="form.batchNo" placeholder="如 B202611-001，用于台账与对账" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="维修服务费总额"><el-input-number v-model="form.serviceFee" :min="0" :precision="2" style="width: 100%" placeholder="服务费行金额来源" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="服务费币制"><el-input v-model="form.currency" placeholder="如 142" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="收料明细" required>
          <el-table :data="form.items" size="small" border style="width: 100%">
            <el-table-column label="收料类型" width="190">
              <template #default="s">
                <el-select v-model="s.row.returnType" size="small">
                  <el-option v-for="t in returnTypes" :key="t.value" :label="t.label" :value="t.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="物料">
              <template #default="s">
                <el-select v-model="s.row.materialId" filterable size="small">
                  <el-option v-for="m in materials" :key="m.id" :label="m.materialName + '（' + m.materialCd + '）'" :value="m.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="150"><template #default="s"><el-input-number v-model="s.row.materialNum" :min="0.000001" :precision="6" size="small" style="width:100%" /></template></el-table-column>
            <el-table-column label="操作" width="70" align="center"><template #default="s"><el-button link type="danger" @click="form.items.splice(s.$index,1)">删</el-button></template></el-table-column>
          </el-table>
          <el-button class="mt4" size="small" icon="Plus" @click="form.items.push({})">添加明细</el-button>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon title="填写维修服务费总额后自动追加 returnType=8 服务费行（数量固定 0.1，按被维修商品编码与名称+（已修复）+维修服务费口径申报）；工单号由本单录入并自动建档（内部支撑数据）" />
      </el-form>
    </el-card>
    <el-row justify="end" class="mt16">
      <el-button type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="cancel">取 消</el-button>
    </el-row>
  </div>
</template>

<script setup name="ReceiveForm">
import { getReceive, addReceive, updReceive } from '@/api/bonded/biz'
import { listMaterial } from '@/api/bonded/material'
import { returnTypes } from '@/utils/bonded'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const materials = ref([])
const form = ref({ receiveNo: null, workNo: null, batchNo: null, serviceFee: null, currency: null, receiveTime: null, items: [{}] })
const rules = {
  receiveNo: [{ required: true, message: '收料单号必填', trigger: 'blur' }],
  workNo: [{ required: true, message: '工单号必填', trigger: 'blur' }],
  receiveTime: [{ required: true, message: '收料日期必填', trigger: 'change' }]
}

const id = route.params.id
const title = ref(id ? '修改收料单' : '新增收料单')

if (id) {
  getReceive(id).then(r => { form.value = r.data })
}
listMaterial({ pageNum: 1, pageSize: 200, declareStatus: '2' }).then(r => { materials.value = r.rows })

function submitForm() {
  proxy.$refs.formRef.validate(v => {
    if (!v) return
    if (!form.value.items.length || form.value.items.some(i => !i.materialId || !i.returnType || !i.materialNum)) { proxy.$modal.msgError('请完善收料明细（类型/物料/数量必填）'); return }
    ;(form.value.id ? updReceive(form.value) : addReceive(form.value)).then(() => {
      proxy.$modal.msgSuccess('保存成功，服务费行已自动生成')
      proxy.$tab.closeOpenPage({ path: '/bonded/receive' })
    })
  })
}

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/receive' })
}
</script>
