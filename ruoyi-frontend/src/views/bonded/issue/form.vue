<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row"><span>{{ title }}</span><el-button link type="primary" icon="Back" @click="cancel">返回列表</el-button></div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
        <el-row>
          <el-col :span="12"><el-form-item label="发料单号" prop="issueNo"><el-input v-model="form.issueNo" placeholder="已自动生成，可修改" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="维修工单" prop="workNo">
              <el-select v-model="form.workNo" filterable placeholder="请选择维修工单" style="width: 100%">
                <el-option v-for="o in workOrders" :key="o.workNo" :label="o.workNo + '（' + woStatusLabel(o.orderStatus) + '）'" :value="o.workNo" />
              </el-select>
              <div class="field-tip">同一批维修业务的归组标识：这批发料/收料/出库/服务费都归到它下面，并作为海关报文(RRD2)的工单号上报；新批次请先到 维修工单 页面创建</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="发料日期" prop="issueTime"><el-date-picker v-model="form.issueTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="发料明细" required>
          <el-table :data="form.items" size="small" border empty-text="暂无明细，点击下方按钮添加" style="width: 100%">
            <el-table-column label="序号" type="index" width="55" align="center" />
            <el-table-column label="物料编号" width="120" show-overflow-tooltip><template #default="s">{{ itemField(s.row, 'materialCd') }}</template></el-table-column align="center">
            <el-table-column label="物料名称" min-width="140" show-overflow-tooltip><template #default="s">{{ s.row.materialName }}</template></el-table-column align="center">
            <el-table-column label="版本号" width="80" align="center"><template #default="s">{{ itemField(s.row, 'version') }}</template></el-table-column>
            <el-table-column label="规格型号" width="130" show-overflow-tooltip><template #default="s">{{ itemSpec(s.row) }}</template></el-table-column align="center">
            <el-table-column label="是否保税" width="90" align="center"><template #default="s">{{ itemBonded(s.row) }}</template></el-table-column>
            <el-table-column label="来源类型" width="120" align="center"><template #default="s">{{ sourceTypeLabel(s.row.goodsSourceType) }}</template></el-table-column>
            <el-table-column label="来源单号" width="180" show-overflow-tooltip><template #default="s">{{ s.row.goodsSourceNo || '-' }}</template></el-table-column align="center">
            <el-table-column label="可用余额" width="100" align="center"><template #default="s">{{ sourceAvailable(s.row) }}</template></el-table-column>
            <el-table-column label="计量单位" width="100" align="center"><template #default="s">{{ itemUnit(s.row) }}</template></el-table-column>
            <el-table-column label="发料数量" width="110" align="center"><template #default="s">{{ s.row.issueQty ?? '-' }}</template></el-table-column>
            <el-table-column label="操作" width="110" align="center" fixed="right">
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
    <!-- 发料明细编辑弹窗 -->
    <el-dialog v-model="itemDialogVisible" :title="editingIndex >= 0 ? '编辑发料明细' : '添加发料明细'" width="760px" append-to-body destroy-on-close>
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="物料编号" prop="materialId">
              <el-select v-model="itemForm.materialId" filterable :filter-method="filterMaterial" @change="onMaterialChange(itemForm)" @visible-change="v => !v && (materialQuery = '')" style="width: 100%">
                <el-option v-for="m in filteredMaterials" :key="m.id" :label="m.materialCd + '　' + m.materialName" :value="m.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="物料名称"><el-input :value="pickedItemMaterial?.materialName" disabled placeholder="选择物料后自动带出" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="来源类型" prop="goodsSourceType">
              <el-select v-model="itemForm.goodsSourceType" style="width: 100%" @change="itemForm.goodsSourceNo = null">
                <el-option label="g-入库清单" value="g" /><el-option label="i-工单收料" value="i" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源单号" prop="goodsSourceNo">
              <el-select v-model="itemForm.goodsSourceNo" filterable :disabled="!itemForm.goodsSourceType" style="width: 100%">
                <el-option v-for="doc in sourceDocs(itemForm.goodsSourceType)" :key="doc.no" :label="sourceOptionLabel(itemForm.goodsSourceType, doc)" :value="doc.no" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="可用余额"><el-input :value="itemAvailable" disabled placeholder="选择来源单号后自动带出" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="发料数量" prop="issueQty"><el-input-number v-model="itemForm.issueQty" :min="0.000001" :precision="6" controls-position="right" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="计量单位"><el-input :value="itemUnit(itemForm)" disabled placeholder="自动带出" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveItem">确 定</el-button>
      </template>
    </el-dialog>
    <DeclarePreviewDialog v-model="previewVisible" biz-type="RRD2" :data="previewData" />
  </div>
</template>

<script setup name="IssueForm">
import { getIssue, addIssue, updIssue, listInbound, listReceive, listStock, nextIssueNo } from '@/api/bonded/biz'
import { listWorkOrderOptions } from '@/api/bonded/workorder'
import { useMaterialCache } from '@/composables/useMaterialCache'
import DeclarePreviewDialog from '../components/DeclarePreviewDialog.vue'
import { customsUnitLabel } from '@/utils/customsUnits'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

// 物料档案统一走全局缓存
const { load: loadMaterialCache, materials } = useMaterialCache()
loadMaterialCache()
const sourceDocsByType = ref({ g: [], i: [] })
const sourceBalances = ref({})
const sourceQtys = ref({})
const form = ref({ issueNo: null, workNo: null, issueTime: null, items: [] })

// 维修工单下拉（工单先行：仅列未结案 01/02/03 工单）
const workOrders = ref([])
const woStatusMap = { '01': '立项', '02': '维修中', '03': '已收齐' }
const woStatusLabel = s => woStatusMap[s] || s
listWorkOrderOptions().then(r => { workOrders.value = r.data || [] }).catch(() => {})
const previewVisible = ref(false)
const previewData = computed(() => form.value)

// 明细弹窗（与入库单同一套交互）
const itemDialogVisible = ref(false)
const editingIndex = ref(-1)
const itemFormRef = ref()
const itemForm = ref({})
const itemRules = {
  materialId: [{ required: true, message: '请选择物料', trigger: 'change' }],
  goodsSourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  goodsSourceNo: [{ required: true, message: '请选择来源单号', trigger: 'change' }],
  issueQty: [
    { required: true, message: '发料数量必填', trigger: 'blur' },
    { validator: (rule, value, callback) => {
      // 余额台账未加载到该物料时不拦截（最终以保存生效校验为准）
      if (!itemForm.value.materialId || !itemForm.value.goodsSourceNo) return callback()
      const key = balanceKey(itemForm.value.goodsSourceType, itemForm.value.goodsSourceNo, itemForm.value.materialId)
      const hasBalance = Object.prototype.hasOwnProperty.call(sourceBalances.value, key) || Object.prototype.hasOwnProperty.call(sourceQtys.value, key)
      if (!hasBalance) return callback()
      const b = sourceMatBalance(itemForm.value.goodsSourceType, { no: itemForm.value.goodsSourceNo }, itemForm.value.materialId)
      if (b != null && Number(value) > b) callback(new Error('发料数量超过可用余额（可用 ' + fmtQty(b) + '）'))
      else callback()
    }, trigger: ['blur', 'change'] }
  ]
}
const pickedItemMaterial = computed(() => findMaterial(itemForm.value))
const itemAvailable = computed(() => sourceAvailable(itemForm.value))
// 物料选择：编号/名称双字段模糊搜索
const materialQuery = ref('')
function filterMaterial(q) { materialQuery.value = q }
const filteredMaterials = computed(() => {
  const q = materialQuery.value.trim().toLowerCase()
  if (!q) return materials.value
  return materials.value.filter(m =>
    (m.materialCd || '').toLowerCase().includes(q) || (m.materialName || '').toLowerCase().includes(q)
  )
})
function sourceTypeLabel(type) { return { g: 'g-入库清单', i: 'i-工单收料' }[type] || type || '-' }
function addItem() {
  editingIndex.value = -1
  itemForm.value = { goodsSourceType: 'g' }
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
  issueNo: [{ required: true, message: '发料单号必填', trigger: 'blur' }],
  workNo: [{ required: true, message: '请选择维修工单', trigger: 'change' }],
  issueTime: [{ required: true, message: '发料日期必填', trigger: 'change' }]
}

const id = route.params.id
const title = ref(id ? '修改发料单' : '新增发料单')

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
  // 物料变更后原来源单号可能无该物料余额，清空重选
  if (row.goodsSourceNo) {
    const doc = (sourceDocsByType.value[row.goodsSourceType] || []).find(d => d.no === row.goodsSourceNo)
    if (!doc || !((sourceMatBalance(row.goodsSourceType, doc, row.materialId) || 0) > 0)) {
      row.goodsSourceNo = null
    }
  }
}

// 来源单号：已生效(03)及申报通过(05)的入库单/收料单，余额仅作参考（最终以保存生效校验为准）
const balanceKey = (type, no, materialId) => `${type}|${no}|${materialId}`
function fmtQty(v) {
  const n = Number(v)
  return Number.isFinite(n) ? String(parseFloat(n.toFixed(6))) : '-'
}
function sourceDocs(type) {
  const list = sourceDocsByType.value[type] || []
  // 已选物料时仅显示该物料在来源单据中仍有余额的单号
  const mid = itemForm.value.materialId
  if (!mid) return list
  return list.filter(d => (sourceMatBalance(type, d, mid) || 0) > 0)
}
// 单个物料在某来源单据中的余额（未取到台账时回退单据原始数量）
function sourceMatBalance(type, doc, materialId) {
  if (!materialId) return null
  const key = balanceKey(type, doc.no, materialId)
  if (Object.prototype.hasOwnProperty.call(sourceBalances.value, key)) return sourceBalances.value[key]
  if (Object.prototype.hasOwnProperty.call(sourceQtys.value, key)) return sourceQtys.value[key]
  return 0
}
function buildSourceDocs(rows, type, noKey, qtyKey) {
  return (rows || []).filter(x => ['03', '05'].includes(x.docStatus)).map(x => {
    const doc = { no: x[noKey], docStatus: x.docStatus, items: x.items || [] }
    for (const it of doc.items) {
      const key = balanceKey(type, doc.no, it.materialId)
      sourceQtys.value[key] = Number(it[qtyKey]) || 0
    }
    return doc
  })
}
function sourceDocBalance(type, doc) {
  // 已选物料按该物料余额展示，未选物料展示整单合计
  const mid = itemForm.value.materialId
  if (mid) return fmtQty(sourceMatBalance(type, doc, mid) || 0)
  let total = 0
  for (const it of doc.items || []) {
    const key = balanceKey(type, doc.no, it.materialId)
    const v = Object.prototype.hasOwnProperty.call(sourceBalances.value, key)
      ? sourceBalances.value[key] : sourceQtys.value[key]
    total += Number(v) || 0
  }
  return fmtQty(total)
}
function sourceOptionLabel(type, doc) {
  return doc.no + '（可用 ' + sourceDocBalance(type, doc) + '）'
}
function sourceAvailable(row) {
  if (!row.materialId || !row.goodsSourceNo) return '-'
  const key = balanceKey(row.goodsSourceType, row.goodsSourceNo, row.materialId)
  if (Object.prototype.hasOwnProperty.call(sourceBalances.value, key)) return fmtQty(sourceBalances.value[key])
  if (Object.prototype.hasOwnProperty.call(sourceQtys.value, key)) return fmtQty(sourceQtys.value[key])
  return '-'
}

if (id) {
  getIssue(id).then(r => { form.value = r.data })
} else {
  // 新增时自动生成发料单号（可修改），失败则留空由用户填写
  nextIssueNo().then(r => { form.value.issueNo = r.data }).catch(() => {})
}
Promise.all([
  listInbound({ pageNum: 1, pageSize: 200 }),
  listReceive({ pageNum: 1, pageSize: 200 }),
  listStock({ pageNum: 1, pageSize: 1000 }).catch(() => ({ rows: [] }))
]).then(([rin, rrcv, rstk]) => {
  sourceQtys.value = {}
  const g = buildSourceDocs(rin.rows, 'g', 'inboundNo', 'storageCnt')
  const i = buildSourceDocs(rrcv.rows, 'i', 'receiveNo', 'materialNum')
  sourceDocsByType.value = { g, i }
  const sums = {}
  for (const l of (rstk.rows || [])) {
    if (!l.goodsSourceType || !l.goodsSourceNo || !l.materialId) continue
    const key = balanceKey(l.goodsSourceType, l.goodsSourceNo, l.materialId)
    sums[key] = (Number(sums[key]) || 0) + (Number(l.qty) || 0)
  }
  sourceBalances.value = sums
})

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

<style scoped>
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
