<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span>{{ title }}</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px" style="max-width: 1100px">
        <el-row>
          <el-col :span="12"><el-form-item label="入库单号" prop="inboundNo"><el-input v-model="form.inboundNo" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="业务类型" prop="busiType">
              <el-select v-model="form.busiType" style="width: 100%">
                <el-option label="04-待维修货物" value="04" /><el-option label="05-维修用料" value="05" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="关联企业名称" prop="relaEntNm"><el-input v-model="form.relaEntNm" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="入库时间" prop="inOutTime"><el-date-picker v-model="form.inOutTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="关联申报单证类型" prop="relaCustomsFormType">
              <el-select v-model="form.relaCustomsFormType" style="width: 100%">
                <el-option label="01-径予放行申明" value="01" /><el-option label="02-公服入库单" value="02" />
                <el-option label="03-报关单" value="03" /><el-option label="05-异常调整" value="05" /><el-option label="99-其他" value="99" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="关联申报单证编号"><el-input v-model="form.relaCustomsFormNum" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="物料明细" required>
          <el-table :data="form.items" size="small" border style="width: 100%">
            <el-table-column label="物料">
              <template #default="scope">
                <el-select v-model="scope.row.materialId" filterable size="small">
                  <el-option v-for="m in materials" :key="m.id" :label="m.materialName + '（' + m.materialCd + '）'" :value="m.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="130"><template #default="s"><el-input-number v-model="s.row.storageCnt" :min="0.000001" :precision="6" size="small" style="width:100%" /></template></el-table-column>
            <el-table-column label="单价" width="130"><template #default="s"><el-input-number v-model="s.row.price" :min="0" :precision="6" size="small" style="width:100%" /></template></el-table-column>
            <el-table-column label="币制" width="90"><template #default="s"><el-input v-model="s.row.currency" size="small" placeholder="502" /></template></el-table-column>
            <el-table-column label="规格型号" width="140"><template #default="s"><el-input v-model="s.row.specificationsModels" size="small" placeholder="默认物料型号" /></template></el-table-column>
            <el-table-column label="来源" width="150">
              <template #default="s">
                <div class="flex gap1">
                  <el-select v-model="s.row.goodsSourceType" size="small" style="width:70px" placeholder="-">
                    <el-option label="g" value="g" /><el-option label="i" value="i" /><el-option label="h" value="h" />
                  </el-select>
                  <el-input v-model="s.row.goodsSourceNo" size="small" placeholder="来源单号" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="法定数量1/2" width="160">
              <template #default="s">
                <div class="flex gap1">
                  <el-input-number v-model="s.row.unit1Num" :precision="5" size="small" style="width:50%" placeholder="法1" controls-position="right" />
                  <el-input-number v-model="s.row.unit2Num" :precision="5" size="small" style="width:50%" placeholder="法2" controls-position="right" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="品牌/监管" width="130">
              <template #default="s">
                <div class="flex gap1">
                  <el-input v-model="s.row.brandType" size="small" style="width:45%" placeholder="品牌" />
                  <el-input v-model="s.row.tradeModeCd" size="small" style="width:55%" placeholder="监管" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="启抵/原产国" width="120">
              <template #default="s">
                <div class="flex gap1">
                  <el-input v-model="s.row.arrivingCountryCode" size="small" style="width:50%" placeholder="启抵" />
                  <el-input v-model="s.row.endCountryCode" size="small" style="width:50%" placeholder="原产" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="业务单证编号" width="130"><template #default="s"><el-input v-model="s.row.busiFormNum" size="small" placeholder="默认入库单号" /></template></el-table-column>
            <el-table-column label="入库说明" width="140"><template #default="s"><el-input v-model="s.row.busiDesc" size="small" /></template></el-table-column>
            <el-table-column label="操作" width="70" align="center" fixed="right"><template #default="s"><el-button link type="danger" @click="form.items.splice(s.$index,1)">删</el-button></template></el-table-column>
          </el-table>
          <el-button class="mt4" size="small" icon="Plus" @click="form.items.push({})">添加明细</el-button>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon title="规范 §3.2.1：关联申报单证类型=01 径予放行申明时，品牌类型/监管方式/启抵国/原产国必填；保税商品第一法定数量必填" />
      </el-form>
    </el-card>
    <el-row justify="center" class="mt16">
      <el-button type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="cancel">取 消</el-button>
    </el-row>
  </div>
</template>

<script setup name="InboundForm">
import { getInbound, addInbound, updInbound } from '@/api/bonded/biz'
import { listMaterial } from '@/api/bonded/material'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const materials = ref([])
const form = ref({ inboundNo: null, busiType: '04', relaEntNm: null, relaCustomsFormType: '03', relaCustomsFormNum: null, inOutTime: null, items: [{}] })
const rules = {
  inboundNo: [{ required: true, message: '入库单号必填', trigger: 'blur' }],
  busiType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  relaCustomsFormType: [{ required: true, message: '请选择关联申报单证类型', trigger: 'change' }],
  inOutTime: [{ required: true, message: '入库时间必填', trigger: 'change' }]
}

const id = route.params.id
const title = ref(id ? '修改入库单' : '新增入库单')

if (id) {
  getInbound(id).then(r => { form.value = r.data })
}
listMaterial({ pageNum: 1, pageSize: 200, declareStatus: '2' }).then(r => { materials.value = r.rows })

function submitForm() {
  proxy.$refs.formRef.validate(v => {
    if (!v) return
    if (!form.value.items.length || form.value.items.some(i => !i.materialId || !i.storageCnt || i.price == null)) { proxy.$modal.msgError('请完善物料明细（物料/数量/单价必填）'); return }
    ;(form.value.id ? updInbound(form.value) : addInbound(form.value)).then(() => {
      proxy.$modal.msgSuccess('保存成功')
      proxy.$tab.closeOpenPage({ path: '/bonded/inbound' })
    })
  })
}

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/inbound' })
}
</script>

<style scoped>
.flex { display: flex; }
.gap1 { gap: 4px; }
</style>
