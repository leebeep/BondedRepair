<template>
  <el-dialog :model-value="modelValue" title="回执详情" width="820px" append-to-body destroy-on-close @update:model-value="v => emit('update:modelValue', v)">
    <el-descriptions :column="2" border size="small" class="mb8">
      <el-descriptions-item label="业务单据">{{ bizTable }} #{{ bizId }}</el-descriptions-item>
      <el-descriptions-item label="单号">{{ bizNo || '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-table v-loading="loading" :data="logs" size="small" border empty-text="暂无申报记录">
      <el-table-column label="报文类型" prop="bizType" width="100" align="center" />
      <el-table-column label="执行类型" width="90" align="center">
        <template #default="s">{{ { '1': '备案', '2': '变更', '3': '作废' }[s.row.executeType] || s.row.executeType }}</template>
      </el-table-column>
      <el-table-column label="预录入编号" prop="uuid" width="180" show-overflow-tooltip />
      <el-table-column label="镜像表" prop="mirrorTable" width="120" show-overflow-tooltip />
      <el-table-column label="回执状态" width="100" align="center">
        <template #default="s"><el-tag :type="declResultType(s.row.resultStatus || 'PENDING')">{{ declResultLabel(s.row.resultStatus || 'PENDING') }}</el-tag></template>
      </el-table-column>
      <el-table-column label="回执消息" prop="msg" min-width="160" show-overflow-tooltip />
      <el-table-column label="回执时间" prop="resultTime" width="160" />
      <el-table-column label="申报时间" prop="createTime" width="160" />
    </el-table>
    <template #footer><el-button @click="emit('update:modelValue', false)">关 闭</el-button></template>
  </el-dialog>
</template>

<script setup name="DeclareReceiptDialog">
import { listDeclareLog } from '@/api/bonded/biz'
import { declResultLabel, declResultType } from '@/utils/bonded'

const props = defineProps({
  modelValue: Boolean,
  bizTable: String,
  bizId: [Number, String],
  bizNo: String
})
const emit = defineEmits(['update:modelValue'])
const logs = ref([])
const loading = ref(false)

function load() {
  if (!props.bizId) { logs.value = []; return }
  loading.value = true
  listDeclareLog({ bizTable: props.bizTable, bizId: props.bizId, pageNum: 1, pageSize: 50 }).then(r => {
    logs.value = r.rows || []
    loading.value = false
  }).catch(() => { loading.value = false })
}

watch(() => props.modelValue, v => { if (v) load() })
</script>
