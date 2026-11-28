<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row"><span>{{ title }}</span><el-button link type="primary" icon="Back" @click="cancel">返回列表</el-button></div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
        <el-row>
          <el-col :span="12"><el-form-item label="入库单号" prop="inboundNo"><el-input v-model="form.inboundNo" placeholder="已自动生成，可修改" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="入库业务类型" prop="busiType">
              <el-select v-model="form.busiType" style="width: 100%">
                <el-option label="04-待维修货物" value="04" /><el-option label="05-维修用料" value="05" /><el-option label="99-其他" value="99" />
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
          <el-table :data="form.items" size="small" border empty-text="暂无明细，点击下方按钮添加" style="width: 100%">
            <el-table-column label="序号" type="index" width="55" align="center" header-align="center" />
            <el-table-column label="物料编号" width="130" header-align="center" show-overflow-tooltip>
              <template #default="s">{{ itemMaterialCode(s.row) }}</template>
            </el-table-column>
            <el-table-column label="物料名称" min-width="150" header-align="center" show-overflow-tooltip>
              <template #default="s">{{ itemMaterialName(s.row) }}</template>
            </el-table-column>
            <el-table-column label="规格型号" width="130" header-align="center" show-overflow-tooltip><template #default="s">{{ itemSpecification(s.row) }}</template></el-table-column>
            <el-table-column label="数量" width="100" align="right" header-align="center"><template #default="s">{{ s.row.storageCnt }}</template></el-table-column>
            <el-table-column label="计量单位" width="100" align="center" header-align="center"><template #default="s">{{ itemUnit(s.row, 'calcUnit') }}</template></el-table-column>
            <el-table-column label="单价" width="100" align="right" header-align="center"><template #default="s">{{ s.row.price }}</template></el-table-column>
            <el-table-column label="币制" width="90" align="center" header-align="center"><template #default="s">{{ currencyLabel(s.row.currency) }}</template></el-table-column>
            <el-table-column label="总价" width="110" align="right" header-align="center"><template #default="s">{{ itemTotal(s.row) }}</template></el-table-column>
            <el-table-column label="法定计量单位1" width="120" align="center" header-align="center"><template #default="s">{{ itemUnit(s.row, 'unit1') }}</template></el-table-column>
            <el-table-column label="第一法定数量" prop="unit1Num" width="110" align="right" header-align="center" />
            <el-table-column label="法定计量单位2" width="120" align="center" header-align="center"><template #default="s">{{ itemUnit(s.row, 'unit2') }}</template></el-table-column>
            <el-table-column label="第二法定数量" prop="unit2Num" width="110" align="right" header-align="center" />
            <el-table-column v-if="isDecl01" label="品牌类型" prop="brandType" width="100" header-align="center" show-overflow-tooltip />
            <el-table-column v-if="isDecl01" label="监管方式" prop="tradeModeCd" width="100" header-align="center" show-overflow-tooltip />
            <el-table-column v-if="isDecl01" label="启抵国" prop="arrivingCountryCode" width="90" header-align="center" show-overflow-tooltip />
            <el-table-column v-if="isDecl01" label="原产国" prop="endCountryCode" width="90" header-align="center" show-overflow-tooltip />
            <el-table-column v-if="isBusi99" label="入库说明" prop="busiDesc" width="140" header-align="center" show-overflow-tooltip />
            <el-table-column label="操作" width="110" align="center" header-align="center" fixed="right">
              <template #default="s">
                <el-button link type="primary" @click="editItem(s.$index)">编辑</el-button>
                <el-button link type="danger" @click="form.items.splice(s.$index,1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button class="add-item-btn" size="small" type="primary" plain icon="Plus" @click="addItem">添加明细</el-button>
        </el-form-item>
      </el-form>
      <el-row justify="end" class="form-footer">
        <el-button plain icon="View" @click="previewVisible = true">预览报文</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </el-row>
    </el-card>
    <!-- 物料明细编辑弹窗 -->
    <el-dialog v-model="itemDialogVisible" :title="editingIndex >= 0 ? '编辑物料明细' : '添加物料明细'" width="760px" append-to-body destroy-on-close>
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="物料编号" prop="materialId">
              <el-select v-model="itemForm.materialId" filterable :filter-method="filterMaterial" @change="onMaterialChange" @visible-change="v => !v && (materialQuery = '')" style="width: 100%">
                <el-option v-for="m in filteredMaterials" :key="m.id" :label="m.materialCd + '　' + materialNameWithMark(m.materialName, m.markType, m.isBonded)" :value="m.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="物料名称"><el-input :value="materialNameWithMark(pickedMaterial?.materialName, pickedMaterial?.markType, pickedMaterial?.isBonded)" disabled placeholder="选择物料后自动带出" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="数量" prop="storageCnt"><el-input-number v-model="itemForm.storageCnt" :min="0.000001" :precision="6" controls-position="right" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="单价" prop="price"><el-input-number v-model="itemForm.price" :min="0" :precision="6" controls-position="right" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="币制">
              <el-select v-model="itemForm.currency" filterable placeholder="请选择" style="width: 100%">
                <el-option v-for="c in currencies" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="总价"><el-input :value="totalPrice == null ? null : String(totalPrice)" disabled placeholder="数量 × 单价 自动计算" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="计量单位"><el-input :value="customsUnitLabel(pickedMaterial?.calcUnit)" disabled placeholder="自动带出" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规格型号"><el-input :value="pickedMaterial?.gModel || itemForm.specificationsModels" disabled placeholder="自动带出" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="法定计量单位1"><el-input :value="customsUnitLabel(pickedMaterial?.unit1)" disabled placeholder="自动带出" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="法定计量单位2"><el-input :value="customsUnitLabel(pickedMaterial?.unit2)" disabled placeholder="自动带出" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="第一法定数量" prop="unit1Num"><el-input-number v-model="itemForm.unit1Num" :precision="5" controls-position="right" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="第二法定数量"><el-input-number v-model="itemForm.unit2Num" :precision="5" controls-position="right" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <template v-if="isDecl01">
          <el-row>
            <el-col :span="12"><el-form-item label="品牌类型" prop="brandType"><el-input v-model="itemForm.brandType" placeholder="径予放行申明时必填" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="监管方式" prop="tradeModeCd"><el-input v-model="itemForm.tradeModeCd" placeholder="径予放行申明时必填" /></el-form-item></el-col>
          </el-row>
          <el-row>
            <el-col :span="12"><el-form-item label="启抵国" prop="arrivingCountryCode"><el-input v-model="itemForm.arrivingCountryCode" placeholder="径予放行申明时必填" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="原产国" prop="endCountryCode"><el-input v-model="itemForm.endCountryCode" placeholder="径予放行申明时必填" /></el-form-item></el-col>
          </el-row>
        </template>
        <el-form-item v-if="isBusi99" label="入库说明" prop="busiDesc"><el-input v-model="itemForm.busiDesc" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveItem">确 定</el-button>
      </template>
    </el-dialog>
    <DeclarePreviewDialog v-model="previewVisible" biz-type="PTO2" :data="previewData" />
  </div>
</template>

<script setup name="InboundForm">
import { getInbound, addInbound, updInbound, nextInboundNo } from '@/api/bonded/biz'
import { listMaterial } from '@/api/bonded/material'
import { currencies } from '@/utils/customsDict'
import { customsUnitLabel } from '@/utils/customsUnits'
import { materialNameWithMark } from '@/utils/bonded'
import DeclarePreviewDialog from '../components/DeclarePreviewDialog.vue'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const materials = ref([])
const form = ref({ inboundNo: null, busiType: '04', relaEntNm: null, relaCustomsFormType: '03', relaCustomsFormNum: null, inOutTime: null, items: [] })
const rules = {
  inboundNo: [{ required: true, message: '入库单号必填', trigger: 'blur' }],
  busiType: [{ required: true, message: '请选择入库业务类型', trigger: 'change' }],
  relaCustomsFormType: [{ required: true, message: '请选择关联申报单证类型', trigger: 'change' }],
  inOutTime: [{ required: true, message: '入库时间必填', trigger: 'change' }]
}
const isDecl01 = computed(() => form.value.relaCustomsFormType === '01')
const isBusi99 = computed(() => form.value.busiType === '99')

// 明细弹窗
const itemDialogVisible = ref(false)
const editingIndex = ref(-1)
const itemFormRef = ref()
const itemForm = ref({})
const previewVisible = ref(false)
const previewData = computed(() => ({
  ...form.value,
  items: form.value.items.map(i => ({ ...materials.value.find(m => m.id === i.materialId), ...i }))
}))
const itemRules = computed(() => ({
  materialId: [{ required: true, message: '请选择物料', trigger: 'change' }],
  storageCnt: [{ required: true, message: '数量必填', trigger: 'blur' }],
  price: [{ required: true, message: '单价必填', trigger: 'blur' }],
  ...(pickedMaterial.value?.isBonded === '1' ? { unit1Num: [{ required: true, message: '保税物料第一法定数量必填', trigger: 'blur' }] } : {}),
  ...(isBusi99.value ? { busiDesc: [{ required: true, message: '入库业务类型=99 其他 时入库说明必填（规范 §3.2.2）', trigger: 'blur' }] } : {}),
  ...(isDecl01.value ? {
    brandType: [{ required: true, message: '品牌类型必填', trigger: 'blur' }],
    tradeModeCd: [{ required: true, message: '监管方式必填', trigger: 'blur' }],
    arrivingCountryCode: [{ required: true, message: '启抵国必填', trigger: 'blur' }],
    endCountryCode: [{ required: true, message: '原产国必填', trigger: 'blur' }]
  } : {})
}))

// 物料支持按编号或名称输入过滤
const materialQuery = ref('')
function filterMaterial(q) { materialQuery.value = q }
const filteredMaterials = computed(() => {
  const q = materialQuery.value.trim().toLowerCase()
  if (!q) return materials.value
  return materials.value.filter(m =>
    (m.materialCd || '').toLowerCase().includes(q) || (m.materialName || '').toLowerCase().includes(q)
  )
})

// 选中的物料（自动带出：名称/计量单位/法定计量单位1/2/规格型号）
const pickedMaterial = computed(() => materials.value.find(m => m.id === itemForm.value.materialId) || null)
const totalPrice = computed(() => {
  const c = Number(itemForm.value.storageCnt) || 0
  const p = Number(itemForm.value.price) || 0
  return c && p ? parseFloat((c * p).toFixed(6)) : null
})

// 选择物料后自动带出：规格型号默认取物料申报型号，单位信息随选中联动
function onMaterialChange(id) {
  const m = materials.value.find(m => m.id === id)
  if (m && m.gModel) { itemForm.value.specificationsModels = m.gModel }
}

function addItem() {
  editingIndex.value = -1
  itemForm.value = {}
  materialQuery.value = ''
  itemDialogVisible.value = true
}

function editItem(index) {
  editingIndex.value = index
  itemForm.value = { ...form.value.items[index] }
  materialQuery.value = ''
  itemDialogVisible.value = true
}

function saveItem() {
  itemFormRef.value.validate(v => {
    if (!v) return
    if (pickedMaterial.value?.gModel) { itemForm.value.specificationsModels = pickedMaterial.value.gModel }
    if (editingIndex.value >= 0) {
      form.value.items.splice(editingIndex.value, 1, { ...itemForm.value })
    } else {
      form.value.items.push({ ...itemForm.value })
    }
    itemDialogVisible.value = false
  })
}

function findMaterial(row) {
  return materials.value.find(m => m.id === row.materialId) || null
}

function itemMaterialCode(row) {
  return findMaterial(row)?.materialCd || row.materialCd || '-'
}

function itemMaterialName(row) {
  const m = findMaterial(row)
  return materialNameWithMark(m?.materialName || row.materialName, m?.markType ?? row.markType, m?.isBonded ?? row.isBonded) || '-'
}

function itemUnit(row, key) {
  const unit = findMaterial(row)?.[key] ?? row[key]
  return unit ? customsUnitLabel(unit) : '-'
}

function itemSpecification(row) {
  return findMaterial(row)?.gModel || row.specificationsModels || '-'
}

function itemTotal(row) {
  if (row.storageCnt == null || row.price == null) return '-'
  const total = (Number(row.storageCnt) || 0) * (Number(row.price) || 0)
  return String(parseFloat(total.toFixed(6)))
}

function currencyLabel(v) {
  const c = currencies.find(c => c.value === v)
  return c ? c.label : v
}

const id = route.params.id
const title = ref(id ? '修改入库单' : '新增入库单')

if (id) {
  getInbound(id).then(r => { form.value = r.data })
} else {
  // 新增时自动生成入库单号（可修改），失败则留空由用户填写
  nextInboundNo().then(r => { form.value.inboundNo = r.data }).catch(() => {})
}
listMaterial({ pageNum: 1, pageSize: 200, declareStatus: '2' }).then(r => { materials.value = r.rows })

function submitForm() {
  proxy.$refs.formRef.validate(v => {
    if (!v) return
    if (!form.value.items.length || form.value.items.some(i => !i.materialId || !i.storageCnt || i.price == null)) { proxy.$modal.msgError('请完善物料明细（物料/数量/单价必填）'); return }
    if (isDecl01.value && form.value.items.some(i => !i.brandType || !i.tradeModeCd || !i.arrivingCountryCode || !i.endCountryCode)) { proxy.$modal.msgError('请完善物料明细（单证类型=01 时品牌类型/监管方式/启抵国/原产国必填）'); return }
    if (isBusi99.value && form.value.items.some(i => !i.busiDesc)) { proxy.$modal.msgError('请完善物料明细（入库业务类型=99 时入库说明必填）'); return }
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
.gap4 { gap: 8px; }
.add-item-btn {
  margin-top: 12px;
}
.form-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
