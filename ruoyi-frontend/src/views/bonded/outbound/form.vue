<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row"><span>{{ title }}</span><el-button link type="primary" icon="Back" @click="cancel">返回列表</el-button></div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
        <el-row>
          <el-col :span="12"><el-form-item label="出库单号" prop="outboundNo"><el-input v-model="form.outboundNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="关联企业名称"><el-input v-model="form.relaEntNm" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="出库业务类型" prop="busiType">
              <el-select v-model="form.busiType" style="width: 100%">
                <el-option v-for="t in busiTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="出库业务单证编号" prop="busiFormNum"><el-input v-model="form.busiFormNum" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="关联申报单证类型" prop="relaCustomsFormType">
              <el-select v-model="form.relaCustomsFormType" style="width: 100%">
                <el-option label="03-报关单" value="03" /><el-option label="04-公服出库单" value="04" />
                <el-option label="05-异常调整" value="05" /><el-option label="06-核扣清单" value="06" /><el-option label="99-其他" value="99" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="关联申报单证编号"><el-input v-model="form.relaCustomsFormNum" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="核扣记录编号"><el-input v-model="form.calculatingRecordNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="出库时间" prop="inOutTime"><el-date-picker v-model="form.inOutTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="出库明细" required>
          <el-table :data="form.items" size="small" border style="width: 100%">
            <el-table-column label="来源类型" width="110">
              <template #default="s">
                <el-select v-model="s.row.goodsSourceType" size="small">
                  <el-option label="g-入库清单" value="g" /><el-option label="i-工单收料" value="i" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="来源单号" width="150"><template #default="s"><el-input v-model="s.row.goodsSourceNo" size="small" /></template></el-table-column>
            <el-table-column label="物料">
              <template #default="s">
                <el-select v-model="s.row.materialId" filterable size="small">
                  <el-option v-for="m in materials" :key="m.id" :label="m.materialName + '（' + m.materialCd + '）'" :value="m.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="出库数量" width="150"><template #default="s"><el-input-number v-model="s.row.deliveryCnt" :min="0.000001" :precision="6" size="small" style="width:100%" /></template></el-table-column>
            <el-table-column label="单价" width="130"><template #default="s"><el-input-number v-model="s.row.price" :min="0" :precision="6" size="small" style="width:100%" /></template></el-table-column>
            <el-table-column label="币制" width="90"><template #default="s"><el-input v-model="s.row.currency" size="small" placeholder="502" /></template></el-table-column>
            <el-table-column label="规格型号" width="140"><template #default="s"><el-input v-model="s.row.specificationsModels" size="small" placeholder="默认物料型号" /></template></el-table-column>
            <el-table-column label="出库说明" width="140"><template #default="s"><el-input v-model="s.row.busiDesc" size="small" placeholder="99-其他时必填" /></template></el-table-column>
            <el-table-column label="操作" width="70" align="center" fixed="right"><template #default="s"><el-button link type="danger" @click="form.items.splice(s.$index,1)">删</el-button></template></el-table-column>
          </el-table>
          <el-button class="mt4" size="small" icon="Plus" @click="form.items.push({})">添加明细</el-button>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon title="出库数量不得超过来源单据余额；业务类型 05（复运回境内区外）必须同单出库维修服务费库存行（数量 0.1，服务费行按被维修商品口径申报）；业务类型 99 时出库说明必填（规范 §3.5.1）" />
      </el-form>
    </el-card>
    <el-row justify="end" class="mt16">
      <el-button type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="cancel">取 消</el-button>
    </el-row>
  </div>
</template>

<script setup name="OutboundForm">
import { getOutbound, addOutbound, updOutbound } from '@/api/bonded/biz'
import { listMaterial } from '@/api/bonded/material'
import { outboundBusiTypes as busiTypes } from '@/utils/bonded'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const materials = ref([])
const form = ref({ outboundNo: null, relaEntNm: null, busiType: null, busiFormNum: null, relaCustomsFormType: null, relaCustomsFormNum: null, calculatingRecordNo: null, inOutTime: null, items: [{}] })
const rules = {
  outboundNo: [{ required: true, message: '出库单号必填', trigger: 'blur' }],
  busiType: [{ required: true, message: '请选择出库业务类型', trigger: 'change' }],
  busiFormNum: [{ required: true, message: '出库业务单证编号必填', trigger: 'blur' }],
  relaCustomsFormType: [{ required: true, message: '关联申报单证类型必填', trigger: 'blur' }],
  inOutTime: [{ required: true, message: '出库时间必填', trigger: 'change' }]
}

const id = route.params.id
const title = ref(id ? '修改出库单' : '新增出库单')

if (id) {
  getOutbound(id).then(r => { form.value = r.data })
}
listMaterial({ pageNum: 1, pageSize: 200, declareStatus: '2' }).then(r => { materials.value = r.rows })

function submitForm() {
  proxy.$refs.formRef.validate(v => {
    if (!v) return
    if (!form.value.items.length || form.value.items.some(i => !i.materialId || !i.goodsSourceType || !i.goodsSourceNo || !i.deliveryCnt || !i.price)) { proxy.$modal.msgError('请完善出库明细（来源/单号/物料/数量/单价必填）'); return }
    ;(form.value.id ? updOutbound(form.value) : addOutbound(form.value)).then(() => {
      proxy.$modal.msgSuccess('保存成功')
      proxy.$tab.closeOpenPage({ path: '/bonded/outbound' })
    })
  })
}

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/outbound' })
}
</script>
