<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row"><span>{{ title }}</span><el-button link type="primary" icon="Back" @click="cancel">返回列表</el-button></div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="出库单号" prop="outboundNo">
              <el-input v-model="form.outboundNo" placeholder="已自动生成，可修改" />
              <div class="field-tip">新增时默认按 参数设置[br.outbound.no.prefix] 自动生成，可手工修改</div>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="出库时间" prop="inOutTime"><el-date-picker v-model="form.inOutTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="出库业务类型" prop="busiType">
              <el-select v-model="form.busiType" style="width: 100%">
                <el-option v-for="t in busiTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="关联申报单证类型" prop="relaCustomsFormType">
              <el-select v-model="form.relaCustomsFormType" style="width: 100%">
                <el-option label="03-报关单" value="03" /><el-option label="04-公服出库单" value="04" /><el-option label="99-其他" value="99" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="关联申报单证编号"><el-input v-model="form.relaCustomsFormNum" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="核扣记录编号"><el-input v-model="form.calculatingRecordNo" /></el-form-item></el-col>
        </el-row>
        <el-form-item v-if="isBusi05" label="维修服务费行" prop="serviceFeeLine" required>
          <span :class="hasServiceFeeLine ? 'text-success' : 'text-danger'">{{ serviceFeeLineTip }}</span>
        </el-form-item>
        <el-form-item label="出库明细" required>
          <el-table :data="form.items" size="small" border empty-text="暂无明细，点击下方按钮添加" style="width: 100%">
            <el-table-column label="序号" type="index" width="55" align="center" />
            <el-table-column label="物料编号" width="120" show-overflow-tooltip><template #default="s">{{ itemField(s.row, 'materialCd') }}</template></el-table-column align="center">
            <el-table-column label="物料名称" min-width="140" show-overflow-tooltip><template #default="s">{{ s.row.materialName }}</template></el-table-column align="center">
            <el-table-column label="版本号" width="80" align="center"><template #default="s">{{ itemField(s.row, 'version') }}</template></el-table-column>
            <el-table-column label="规格型号" width="130" show-overflow-tooltip><template #default="s">{{ itemSpec(s.row) }}</template></el-table-column align="center">
            <el-table-column label="是否保税" width="90" align="center"><template #default="s">{{ itemBonded(s.row) }}</template></el-table-column>
            <el-table-column label="来源类型" width="110" align="center"><template #default="s">{{ sourceTypeLabel(s.row.goodsSourceType) }}</template></el-table-column>
            <el-table-column label="来源单号" width="150" show-overflow-tooltip><template #default="s">{{ s.row.goodsSourceNo || '-' }}</template></el-table-column align="center">
            <el-table-column label="余额提示" width="150"><template #default="s"><span :class="balanceInsufficient(s.row) ? 'text-danger' : ''">{{ balanceTip(s.row) }}</span></template></el-table-column align="center">
            <el-table-column label="计量单位" width="100" align="center"><template #default="s">{{ itemUnit(s.row) }}</template></el-table-column>
            <el-table-column label="出库数量" width="110" align="center"><template #default="s">{{ s.row.deliveryCnt ?? '-' }}</template></el-table-column>
            <el-table-column label="单价" width="100" align="center"><template #default="s">{{ s.row.price ?? '-' }}</template></el-table-column>
            <el-table-column label="币制" width="110" align="center"><template #default="s">{{ currencyLabel(s.row.currency) }}</template></el-table-column>
            <el-table-column v-if="isBusi99" label="出库说明" width="130" show-overflow-tooltip><template #default="s">{{ s.row.busiDesc || '-' }}</template></el-table-column align="center">
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
    <!-- 出库明细编辑弹窗 -->
    <el-dialog v-model="itemDialogVisible" :title="editingIndex >= 0 ? '编辑出库明细' : '添加出库明细'" width="760px" append-to-body destroy-on-close>
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
          <el-col :span="12"><el-form-item label="余额提示"><el-input :value="balanceTip(itemForm)" disabled placeholder="选择来源单号后自动带出" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="出库数量" prop="deliveryCnt">
              <el-input-number v-model="itemForm.deliveryCnt" :min="0.000001" :precision="6" :disabled="isServiceFeeLine(itemForm)" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="单价" prop="price"><el-input-number v-model="itemForm.price" :min="0" :precision="6" controls-position="right" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="币制"><el-select v-model="itemForm.currency" filterable style="width: 100%"><el-option v-for="c in currencies" :key="c.value" :label="c.label" :value="c.value" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="计量单位"><el-input :value="itemUnit(itemForm)" disabled placeholder="自动带出" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规格型号"><el-input :value="itemSpec(itemForm)" disabled placeholder="选择物料后自动带出" /></el-form-item></el-col>
        </el-row>
        <el-form-item v-if="isBusi99" label="出库说明" prop="busiDesc"><el-input v-model="itemForm.busiDesc" type="textarea" :rows="2" placeholder="业务类型=99 其他时必填" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveItem">确 定</el-button>
      </template>
    </el-dialog>
    <DeclarePreviewDialog v-model="previewVisible" biz-type="SDO2" :data="previewData" />
  </div>
</template>

<script setup name="OutboundForm">
import { getOutbound, addOutbound, updOutbound, listStock, listInbound, listReceive, nextOutboundNo } from '@/api/bonded/biz'
import { listMaterial } from '@/api/bonded/material'
import { useMaterialCache } from '@/composables/useMaterialCache'
import DeclarePreviewDialog from '../components/DeclarePreviewDialog.vue'
import { outboundBusiTypes as busiTypes } from '@/utils/bonded'
import { currencies, currencyLabel } from '@/utils/customsDict'
import { customsUnitLabel } from '@/utils/customsUnits'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const sourceDocsByType = ref({ g: [], i: [] })
const sourceBalances = ref({})
const sourceQtys = ref({})
const form = ref({ outboundNo: null, busiType: null, relaCustomsFormType: null, relaCustomsFormNum: null, calculatingRecordNo: null, inOutTime: null, items: [] })
const previewVisible = ref(false)
const previewData = computed(() => form.value)

// 明细弹窗（与入库单同一套交互）
const itemDialogVisible = ref(false)
const editingIndex = ref(-1)
const itemFormRef = ref()
const itemForm = ref({})
const itemRules = computed(() => ({
  materialId: [{ required: true, message: '请选择物料', trigger: 'change' }],
  goodsSourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  goodsSourceNo: [{ required: true, message: '请选择来源单号', trigger: 'change' }],
  deliveryCnt: [
    { required: true, message: '出库数量必填', trigger: 'blur' },
    { validator: (rule, value, callback) => {
      if (isServiceFeeLine(itemForm.value)) return callback()
      // 余额台账未加载到该物料时不拦截（最终以保存生效校验为准）
      const b = balanceOf(itemForm.value)
      if (b == null) return callback()
      if (Number(value) > b) callback(new Error('出库数量超过可用余额（可用 ' + parseFloat(Number(b).toFixed(6)) + '）'))
      else callback()
    }, trigger: ['blur', 'change'] }
  ],
  price: [{ required: true, message: '单价必填', trigger: 'blur' }],
  ...(isBusi99.value ? { busiDesc: [{ required: true, message: '业务类型=99 其他时出库说明必填', trigger: 'blur' }] } : {})
}))
const pickedItemMaterial = computed(() => findMaterial(itemForm.value))
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
  itemForm.value = { goodsSourceType: 'i' }
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
    // 非 99-其他 不保留出库说明（与表头业务类型联动）
    const saved = { ...itemForm.value }
    if (!isBusi99.value) { delete saved.busiDesc }
    if (editingIndex.value >= 0) {
      form.value.items.splice(editingIndex.value, 1, saved)
    } else {
      form.value.items.push(saved)
    }
    itemDialogVisible.value = false
  })
}

const isBusi05 = computed(() => form.value.busiType === '05')
const isBusi99 = computed(() => form.value.busiType === '99')
// 业务类型联动出库说明：离开 99-其他 时清空明细中已录入的出库说明，避免隐藏字段残留旧值
watch(() => form.value.busiType, t => {
  if (t !== '99') {
    for (const it of form.value.items) { delete it.busiDesc }
  }
})
function findMaterial(row) {
  return materials.value.find(m => m.id === row?.materialId) || null
}
function isServiceFeeLine(row) {
  return row?.markType === '8' || findMaterial(row)?.markType === '8'
}
const hasServiceFeeLine = computed(() =>
  form.value.items.some(i => isServiceFeeLine(i) && Number(i.deliveryCnt) === 0.1)
)
const serviceFeeLineTip = computed(() => hasServiceFeeLine.value
  ? '已添加维修服务费行（markType=8，数量 0.1）'
  : '需同单添加维修服务费行（markType=8，数量 0.1）')

const rules = {
  outboundNo: [{ required: true, message: '出库单号必填', trigger: 'blur' }],
  busiType: [{ required: true, message: '请选择出库业务类型', trigger: 'change' }],
  relaCustomsFormType: [{ required: true, message: '关联申报单证类型必填', trigger: 'blur' }],
  inOutTime: [{ required: true, message: '出库时间必填', trigger: 'change' }],
  serviceFeeLine: [{ validator: (rule, value, callback) => {
    if (isBusi05.value && !hasServiceFeeLine.value) {
      callback(new Error('业务类型=05 时必须同单添加维修服务费行（markType=8，数量 0.1）'))
    } else {
      callback()
    }
  }, trigger: 'change' }]
}

const id = route.params.id
const title = ref(id ? '修改出库单' : '新增出库单')

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
  return row?.specificationsModels || row?.gModel || findMaterial(row)?.gModel || '-'
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
  row.specificationsModels = m.gModel || null
  if (m.markType === '8') row.deliveryCnt = 0.1
  // 物料变更后原来源单号可能无该物料余额，清空重选
  if (row.goodsSourceNo) {
    const doc = (sourceDocsByType.value[row.goodsSourceType] || []).find(d => d.no === row.goodsSourceNo)
    if (!doc || !((sourceMatBalance(row.goodsSourceType, doc, row.materialId) || 0) > 0)) {
      row.goodsSourceNo = null
    }
  }
}

// 来源单号：已生效(03)及申报通过(05)的入库单/收料单；已选物料时仅显示该物料仍有余额的单号
function buildSourceDocs(rows, type, noKey, qtyKey) {
  return (rows || []).filter(x => ['03', '05'].includes(x.docStatus)).map(x => {
    const doc = { no: x[noKey], docStatus: x.docStatus, items: x.items || [] }
    for (const it of doc.items) {
      sourceQtys.value[balanceKey(type, doc.no, it.materialId)] = Number(it[qtyKey]) || 0
    }
    return doc
  })
}
function sourceDocs(type) {
  const list = sourceDocsByType.value[type] || []
  const mid = itemForm.value.materialId
  if (!mid) return list
  return list.filter(d => (sourceMatBalance(type, d, mid) || 0) > 0)
}
function sourceMatBalance(type, doc, materialId) {
  if (!materialId) return null
  const key = balanceKey(type, doc.no, materialId)
  if (Object.prototype.hasOwnProperty.call(sourceBalances.value, key)) return sourceBalances.value[key]
  if (Object.prototype.hasOwnProperty.call(sourceQtys.value, key)) return sourceQtys.value[key]
  return 0
}
function fmtQty(v) {
  const n = Number(v)
  return Number.isFinite(n) ? String(parseFloat(n.toFixed(6))) : '-'
}
function sourceOptionLabel(type, doc) {
  // 已选物料按该物料余额展示，未选物料展示整单合计
  const mid = itemForm.value.materialId
  if (mid) return doc.no + '（可用 ' + fmtQty(sourceMatBalance(type, doc, mid) || 0) + '）'
  let total = 0
  for (const it of doc.items || []) {
    const key = balanceKey(type, doc.no, it.materialId)
    const v = Object.prototype.hasOwnProperty.call(sourceBalances.value, key)
      ? sourceBalances.value[key] : sourceQtys.value[key]
    total += Number(v) || 0
  }
  return doc.no + '（可用 ' + fmtQty(total) + '）'
}

// 来源余额提示：取库存台账净流水，供参考，不阻断提交（由后端保存生效校验）
const balanceKey = (type, no, materialId) => `${type}|${no}|${materialId}`
function balanceOf(row) {
  if (!row.materialId || !row.goodsSourceNo) return null
  const key = balanceKey(row.goodsSourceType, row.goodsSourceNo, row.materialId)
  if (Object.prototype.hasOwnProperty.call(sourceBalances.value, key)) return Number(sourceBalances.value[key])
  if (Object.prototype.hasOwnProperty.call(sourceQtys.value, key)) return Number(sourceQtys.value[key])
  return null
}
function balanceInsufficient(row) {
  const b = balanceOf(row)
  return b != null && (Number(row.deliveryCnt) || 0) > b
}
function balanceTip(row) {
  const b = balanceOf(row)
  if (b == null) return ''
  const n = Number.isFinite(b) ? String(parseFloat(b.toFixed(6))) : '-'
  return balanceInsufficient(row) ? '余额不足（可用 ' + n + '）' : '可用 ' + n
}

if (id) {
  getOutbound(id).then(r => { form.value = r.data })
} else {
  // 新增时自动生成出库单号（可修改），失败则留空由用户填写
  nextOutboundNo().then(r => { form.value.outboundNo = r.data }).catch(() => {})
}
// 备案通过的物料（全局缓存）+ markType=8 服务费虚物料（虚物料未备案但出库允许作为台账载体）
const { load: loadMaterialCache, materials, byId } = useMaterialCache()
loadMaterialCache()
listMaterial({ pageNum: 1, pageSize: 200, markType: '8' }).then(r => {
  const extra = (r.rows || []).filter(m => !byId(m.id))
  materials.value = [...materials.value, ...extra]
})
Promise.all([
  listInbound({ pageNum: 1, pageSize: 200 }),
  listReceive({ pageNum: 1, pageSize: 200 }),
  listStock({ pageNum: 1, pageSize: 1000 }).catch(() => ({ rows: [] }))
]).then(([rin, rrcv, rstk]) => {
  sourceQtys.value = {}
  sourceDocsByType.value = {
    g: buildSourceDocs(rin.rows, 'g', 'inboundNo', 'storageCnt'),
    i: buildSourceDocs(rrcv.rows, 'i', 'receiveNo', 'materialNum')
  }
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
    if (!form.value.items.length || form.value.items.some(i => !i.materialId || !i.goodsSourceType || !i.goodsSourceNo || !i.deliveryCnt || i.price == null)) { proxy.$modal.msgError('请完善出库明细（来源/单号/物料/数量/单价必填）'); return }
    if (isBusi99.value && form.value.items.some(i => !i.busiDesc)) { proxy.$modal.msgError('业务类型=99 其他时出库说明必填'); return }
    if (form.value.items.some(balanceInsufficient)) { proxy.$modal.msgWarning('部分明细来源余额不足，请核对；最终以保存生效校验为准') }
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

<style scoped>
.text-danger { color: #f56c6c; }
.text-success { color: #67c23a; }
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
