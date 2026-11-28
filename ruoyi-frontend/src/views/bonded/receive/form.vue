<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row"><span>{{ title }}</span><el-button link type="primary" icon="Back" @click="cancel">返回列表</el-button></div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="收料单号" prop="receiveNo">
              <el-input v-model="form.receiveNo" placeholder="已自动生成，可修改" />
              <div class="field-tip">新增时默认按 参数设置[br.receive.no.prefix] 自动生成，可手工修改</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="维修工单号" prop="workNo">
              <el-input v-model="form.workNo" placeholder="如 WO-2026-001，不存在将自动建档" />
              <div class="field-tip">维修工单是收发料的归集主体：同工单的发料/收料共同参与统计与收齐判断</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="收料日期" prop="receiveTime"><el-date-picker v-model="form.receiveTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="申报批次号"><el-input v-model="form.batchNo" placeholder="如 B202611-001，用于台账与对账" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="维修服务费">
              <el-checkbox v-model="feeEnabled">本工单收取维修服务费</el-checkbox>
              <div class="field-tip">勾选后收料申报自动追加数量 0.1 的维修服务费行（returnType=8）；已维修货物复运回境内区外出库时强制同单带出</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="feeEnabled">
          <el-col :span="12"><el-form-item label="服务费总额" prop="serviceFee"><el-input-number v-model="form.serviceFee" :min="0" :precision="2" style="width: 100%" placeholder="服务费行金额来源" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="服务费币制"><el-select v-model="form.currency" filterable clearable placeholder="请选择" style="width: 100%"><el-option v-for="c in currencies" :key="c.value" :label="c.label" :value="c.value" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="收料明细" required>
          <el-table :data="form.items" size="small" border empty-text="暂无明细，点击下方按钮添加" style="width: 100%">
            <el-table-column label="序号" type="index" width="55" align="center" header-align="center" />
            <el-table-column label="收料类型" width="190" header-align="center" show-overflow-tooltip><template #default="s">{{ returnTypeLabel(s.row) }}</template></el-table-column>
            <el-table-column label="物料编号" width="120" header-align="center" show-overflow-tooltip><template #default="s">{{ itemField(s.row, 'materialCd') }}</template></el-table-column>
            <el-table-column label="物料名称" min-width="140" header-align="center" show-overflow-tooltip><template #default="s">{{ materialNameWithMark(s.row.materialName, s.row.markType, s.row.isBonded) }}</template></el-table-column>
            <el-table-column label="版本号" width="80" align="center" header-align="center"><template #default="s">{{ itemField(s.row, 'version') }}</template></el-table-column>
            <el-table-column label="规格型号" width="130" header-align="center" show-overflow-tooltip><template #default="s">{{ itemSpec(s.row) }}</template></el-table-column>
            <el-table-column label="是否保税" width="90" align="center" header-align="center"><template #default="s">{{ itemBonded(s.row) }}</template></el-table-column>
            <el-table-column label="计量单位" width="100" align="center" header-align="center"><template #default="s">{{ itemUnit(s.row) }}</template></el-table-column>
            <el-table-column label="数量" width="110" align="right" header-align="center"><template #default="s">{{ s.row.materialNum ?? '-' }}</template></el-table-column>
            <el-table-column label="说明" width="170" header-align="center" show-overflow-tooltip><template #default="s"><span v-if="isServiceFee(s.row)" class="text-muted">系统自动生成，数量 0.1</span></template></el-table-column>
            <el-table-column label="操作" width="110" align="center" header-align="center" fixed="right">
              <template #default="s">
                <el-button link type="primary" :disabled="isServiceFee(s.row)" @click="editItem(s.$index)">编辑</el-button>
                <el-button link type="danger" :disabled="isServiceFee(s.row)" @click="form.items.splice(s.$index,1)">删除</el-button>
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
    <!-- 收料明细编辑弹窗 -->
    <el-dialog v-model="itemDialogVisible" :title="editingIndex >= 0 ? '编辑收料明细' : '添加收料明细'" width="760px" append-to-body destroy-on-close>
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="收料类型" prop="returnType">
              <el-select v-model="itemForm.returnType" style="width: 100%">
                <el-option v-for="t in editableReturnTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="收料类型说明"><el-input :value="returnTypeLabel(itemForm)" disabled placeholder="选择收料类型后自动带出" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="物料编号" prop="materialId">
              <el-select v-model="itemForm.materialId" filterable style="width: 100%" @change="onMaterialChange(itemForm)">
                <el-option v-for="m in materials" :key="m.id" :label="m.materialCd + '　' + materialNameWithMark(m.materialName, m.markType, m.isBonded)" :value="m.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="物料名称"><el-input :value="materialNameWithMark(pickedItemMaterial?.materialName, pickedItemMaterial?.markType, pickedItemMaterial?.isBonded)" disabled placeholder="选择物料后自动带出" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="数量" prop="materialNum"><el-input-number v-model="itemForm.materialNum" :min="0.000001" :precision="6" controls-position="right" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="计量单位"><el-input :value="itemUnit(itemForm)" disabled placeholder="自动带出" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveItem">确 定</el-button>
      </template>
    </el-dialog>
    <DeclarePreviewDialog v-model="previewVisible" biz-type="MSO2" :data="previewData" />
  </div>
</template>

<script setup name="ReceiveForm">
import { getReceive, addReceive, updReceive, nextReceiveNo, queryWorkOrder } from '@/api/bonded/biz'
import { listMaterial } from '@/api/bonded/material'
import DeclarePreviewDialog from '../components/DeclarePreviewDialog.vue'
import { returnTypes, materialNameWithMark } from '@/utils/bonded'
import { currencies } from '@/utils/customsDict'
import { customsUnitLabel } from '@/utils/customsUnits'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const materials = ref([])
const form = ref({ receiveNo: null, workNo: null, batchNo: null, serviceFee: null, currency: null, receiveTime: null, items: [] })
const previewVisible = ref(false)
const previewData = computed(() => form.value)

// 明细弹窗（与入库单同一套交互）
const itemDialogVisible = ref(false)
const editingIndex = ref(-1)
const itemFormRef = ref()
const itemForm = ref({})
const itemRules = {
  returnType: [{ required: true, message: '请选择收料类型', trigger: 'change' }],
  materialId: [{ required: true, message: '请选择物料', trigger: 'change' }],
  materialNum: [{ required: true, message: '收料数量必填', trigger: 'blur' }]
}
const pickedItemMaterial = computed(() => findMaterial(itemForm.value))
function returnTypeLabel(row) { return returnTypes.find(t => t.value === row?.returnType)?.label || row?.returnType || '-' }
function addItem() {
  editingIndex.value = -1
  itemForm.value = {}
  itemDialogVisible.value = true
}
function editItem(index) {
  editingIndex.value = index
  itemForm.value = { ...form.value.items[index] }
  itemDialogVisible.value = true
}
function saveItem() {
  itemFormRef.value.validate(v => {
    if (!v) return
    if (editingIndex.value >= 0) {
      form.value.items.splice(editingIndex.value, 1, { ...itemForm.value })
    } else {
      form.value.items.push({ ...itemForm.value })
    }
    itemDialogVisible.value = false
  })
}
const rules = {
  receiveNo: [{ required: true, message: '收料单号必填', trigger: 'blur' }],
  workNo: [{ required: true, message: '维修工单号必填', trigger: 'blur' }],
  receiveTime: [{ required: true, message: '收料日期必填', trigger: 'change' }]
}
// 服务费行 returnType=8 由后端自动生成，不允许手工选择
const editableReturnTypes = computed(() => returnTypes.filter(t => t.value !== '8'))

const id = route.params.id
const title = ref(id ? '修改收料单' : '新增收料单')

// 维修服务费开关：勾选才显示总额/币制；工单已有服务费时自动勾选带出
const feeEnabled = ref(false)
function applyWorkOrderFee(wo) {
  if (wo && wo.serviceFee != null && Number(wo.serviceFee) > 0) {
    feeEnabled.value = true
    form.value.serviceFee = wo.serviceFee
    form.value.currency = wo.currency
  }
}
watch(() => form.value.workNo, no => {
  if (id || !no) return
  queryWorkOrder(no).then(r => { applyWorkOrderFee(r.data) }).catch(() => {})
})

function isServiceFee(row) { return row?.returnType === '8' }
function findMaterial(row) {
  return materials.value.find(m => m.id === row?.materialId) || null
}
function itemField(row, key) {
  const v = row?.[key] ?? findMaterial(row)?.[key]
  return v == null || v === '' ? '-' : String(v)
}
function itemUnit(row) {
  const u = row?.calcUnit ?? findMaterial(row)?.calcUnit
  return u ? customsUnitLabel(u) : '-'
}
function itemBonded(row) {
  const v = row?.isBonded ?? findMaterial(row)?.isBonded
  return v == null || v === '' ? '-' : (v === '1' ? '保税' : '非保税')
}
function itemSpec(row) {
  return row?.gModel || row?.specificationsModels || findMaterial(row)?.gModel || '-'
}
function onMaterialChange(row) {
  const m = findMaterial(row)
  if (!m) return
  row.materialName = m.materialName
  row.materialCd = m.materialCd
  row.version = m.version
  row.calcUnit = m.calcUnit
  row.isBonded = m.isBonded
  row.gModel = m.gModel
  row.markType = m.markType
}

if (id) {
  getReceive(id).then(r => {
    form.value = r.data
    // 草稿编辑：按已有服务费回显开关（服务费存于工单，查询已带出）
    if (r.data && r.data.serviceFee != null && Number(r.data.serviceFee) > 0) { feeEnabled.value = true }
  })
} else {
  // 新增时自动生成收料单号（可修改），失败则留空由用户填写
  nextReceiveNo().then(r => { form.value.receiveNo = r.data }).catch(() => {})
}
listMaterial({ pageNum: 1, pageSize: 200, declareStatus: '2' }).then(r => { materials.value = r.rows })

function submitForm() {
  proxy.$refs.formRef.validate(v => {
    if (!v) return
    if (!form.value.items.length || form.value.items.some(i => !i.materialId || !i.returnType || !i.materialNum)) { proxy.$modal.msgError('请完善收料明细（类型/物料/数量必填）'); return }
    if (feeEnabled.value && (form.value.serviceFee == null || Number(form.value.serviceFee) <= 0)) { proxy.$modal.msgError('勾选维修服务费后请填写服务费总额'); return }
    // 不勾选时显式清零，保证工单服务费与表单口径一致（后端为 0 时不生成服务费行）
    if (!feeEnabled.value) { form.value.serviceFee = 0 }
    ;(form.value.id ? updReceive(form.value) : addReceive(form.value)).then(() => {
      proxy.$modal.msgSuccess('保存成功' + (feeEnabled.value ? '，服务费行已自动生成' : ''))
      proxy.$tab.closeOpenPage({ path: '/bonded/receive' })
    })
  })
}

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/receive' })
}
</script>

<style scoped>
.text-muted { color: #909399; }
.field-tip {
  width: 100%;
  font-size: 12px;
  line-height: 1.4;
  color: #909399;
}
.add-item-btn {
  margin-top: 12px;
}
.form-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
