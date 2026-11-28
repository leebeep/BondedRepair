<template>
  <el-dialog :model-value="modelValue" title="申报报文预览" width="900px" append-to-body destroy-on-close @update:model-value="v => emit('update:modelValue', v)">
    <el-descriptions :column="3" border size="small" class="mb8">
      <el-descriptions-item label="报文类型">{{ bizType }}</el-descriptions-item>
      <el-descriptions-item label="执行类型">{{ { '1': '备案', '2': '变更', '3': '作废' }[payload.executeType] || payload.executeType }}</el-descriptions-item>
      <el-descriptions-item label="明细行数">{{ rows.length }}</el-descriptions-item>
    </el-descriptions>
    <el-tabs type="border-card">
      <el-tab-pane label="表单视图">
        <el-table :data="rows" size="small" border empty-text="暂无明细">
          <el-table-column v-for="col in columns" :key="col" :label="col" :prop="col" min-width="130" show-overflow-tooltip align="center"/>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="JSON 报文">
        <pre class="json-box">{{ jsonText }}</pre>
      </el-tab-pane>
    </el-tabs>
    <template #footer><el-button @click="emit('update:modelValue', false)">关 闭</el-button></template>
  </el-dialog>
</template>

<script setup name="DeclarePreviewDialog">

const props = defineProps({
  modelValue: Boolean,
  bizType: { type: String, required: true },
  data: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue'])

function now() {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function desc(item) {
  const name = item.materialName || item.materialDesc || ''
  if (item.markType === '8') return name.includes('维修服务费') ? name : name + '维修服务费'
  return name
}

const rows = computed(() => {
  const h = props.data || {}
  const items = h.items || []
  if (props.bizType === 'PTO2') {
    return items.map(i => ({
      take_order_cd: h.inboundNo,
      rela_ent_nm: h.relaEntNm,
      material_cd: i.materialCd,
      material_desc: desc(i),
      material_version: i.version || '1',
      specifications_models: i.specificationsModels || i.gModel,
      storage_cnt: i.storageCnt,
      price: i.price,
      currency: i.currency,
      calc_unit: i.calcUnit,
      is_bonded: i.isBonded,
      in_out_time: h.inOutTime,
      busi_type: h.busiType,
      busi_form_num: h.busiFormNum || h.inboundNo,
      rela_customs_form_type: h.relaCustomsFormType,
      rela_customs_form_num: h.relaCustomsFormNum,
      goods_source_type: i.goodsSourceType,
      goods_source_no: i.goodsSourceNo,
      brand_type: i.brandType,
      trade_mode_cd: i.tradeModeCd,
      unit1_num: i.unit1Num,
      unit2_num: i.unit2Num,
      arriving_country_code: i.arrivingCountryCode,
      end_country_code: i.endCountryCode,
      busid_esc: i.busiDesc
    }))
  }
  if (props.bizType === 'RRD2') {
    return items.map(i => ({
      work_no: h.workNo,
      finish_material_num: h.issueNo,
      finish_material_tm: h.issueTime,
      goods_source_type: i.goodsSourceType,
      goods_source_no: i.goodsSourceNo,
      material_cd: i.materialCd,
      material_desc: desc(i),
      material_version: i.version || '1',
      calc_unit: i.calcUnit,
      issue_material_qty: i.issueQty,
      is_bonded: i.isBonded,
      price: i.price,
      gmt_create: now()
    }))
  }
  if (props.bizType === 'MSO2') {
    return items.map(i => ({
      return_material_no: h.receiveNo,
      return_time: h.receiveTime,
      work_no: h.workNo,
      material_cd: i.materialCd,
      material_desc: desc(i),
      material_version: i.version || '1',
      material_num: i.materialNum,
      calc_unit: i.calcUnit,
      is_bonded: i.isBonded,
      return_type: i.returnType
    }))
  }
  return items.map(i => ({
    rela_ent_nm: h.relaEntNm,
    releaseno: h.outboundNo,
    material_cd: i.materialCd,
    material_desc: desc(i),
    version: i.version || '1',
    specifications_models: i.specificationsModels || i.gModel,
    delivery_cnt: i.deliveryCnt,
    price: i.price,
    currency: i.currency,
    calc_unit: i.calcUnit,
    is_bonded: i.isBonded,
    in_out_time: h.inOutTime,
    busi_type: h.busiType,
    busi_desc: i.busiDesc,
    busi_form_num: h.busiFormNum,
    rela_customs_form_type: h.relaCustomsFormType,
    rela_customs_form_num: h.relaCustomsFormNum,
    calculating_record_no: h.calculatingRecordNo,
    goods_source_type: i.goodsSourceType,
    goods_source_no: i.goodsSourceNo
  }))
})

const columns = computed(() => rows.value.length ? Object.keys(rows.value[0]) : [])

const payload = computed(() => ({
  enterpCd: props.data.enterpCd || '4600AA0001',
  syncBusiType: props.bizType,
  syncTime: now(),
  executeType: props.data.executeType || '1',
  data: rows.value
}))

const jsonText = computed(() => JSON.stringify(payload.value, null, 2))
</script>

<style scoped>
.json-box {
  margin: 0;
  padding: 12px;
  max-height: 50vh;
  overflow: auto;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  font-size: 12px;
  line-height: 1.5;
}
</style>
