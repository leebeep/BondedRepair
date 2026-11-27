<template>
  <div class="p2">
    <el-tabs v-model="tab">
      <el-tab-pane label="台账流水" name="ledger">
        <el-form :inline="true" label-width="90px">
          <el-form-item label="物料名称"><el-input v-model="queryParams.materialName" clearable style="width: 180px" @keyup.enter="getList" /></el-form-item>
          <el-form-item label="业务环节">
            <el-select v-model="queryParams.bizType" clearable placeholder="全部" style="width: 140px">
              <el-option label="入库" value="IN" /><el-option label="发料" value="ISSUE" />
              <el-option label="收料" value="RCV" /><el-option label="出库" value="OUT" />
            </el-select>
          </el-form-item>
          <el-form-item label="货物类型">
            <el-select v-model="queryParams.markType" clearable placeholder="全部" style="width: 150px">
              <el-option v-for="d in markTypes" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" icon="Search" @click="getList">搜索</el-button></el-form-item>
        </el-form>
        <el-table v-loading="loading" :data="rows">
          <el-table-column label="时间" prop="createTime" width="160" />
          <el-table-column label="物料" prop="materialName" show-overflow-tooltip />
          <el-table-column label="货物类型" width="150">
            <template #default="s">{{ markTypes.find(d => d.value === s.row.markType)?.label || s.row.markType }}</template>
          </el-table-column>
          <el-table-column label="环节" prop="bizType" width="90" align="center">
            <template #default="s"><el-tag :type="{ IN: 'success', ISSUE: 'warning', RCV: 'primary', OUT: 'danger' }[s.row.bizType]">{{ { IN: '入库', ISSUE: '发料', RCV: '收料', OUT: '出库' }[s.row.bizType] }}</el-tag></template>
          </el-table-column>
          <el-table-column label="来源单号" prop="goodsSourceNo" width="150" />
          <el-table-column label="工单" prop="workNo" width="140" />
          <el-table-column label="变动数量" prop="qty" width="110" align="right" />
          <el-table-column label="结存" prop="balance" width="110" align="right" />
        </el-table>
        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>

      <el-tab-pane label="库存对账" name="reconcile">
        <el-form :inline="true">
          <el-form-item label="申报批次"><el-input v-model="recQuery.batchNo" clearable placeholder="如 B202611-001" style="width: 180px" /></el-form-item>
          <el-form-item label="工单号"><el-input v-model="recQuery.workNo" clearable style="width: 160px" /></el-form-item>
          <el-form-item><el-button type="primary" icon="Search" @click="getReconcile">对账</el-button></el-form-item>
        </el-form>
        <!-- 六类账存卡片 -->
        <el-row :gutter="10" class="mb8">
          <el-col :span="3" v-for="c in rec.cards" :key="c.markType">
            <el-card shadow="hover" body-style="padding: 12px">
              <div class="card-label">{{ markTypes.find(d => d.value === c.markType)?.label }}</div>
              <div class="card-qty" :class="{ 'card-alert': c.markType !== '4' && c.markType !== '8' && Number(c.balance) !== 0 }">{{ c.balance }}</div>
            </el-card>
          </el-col>
        </el-row>
        <!-- 四项公式对账表 -->
        <el-table :data="rec.rows" border>
          <el-table-column label="对账项目" prop="item" />
          <el-table-column label="期望值" width="140" align="right">
            <template #default="s">{{ s.row.expected }}</template>
          </el-table-column>
          <el-table-column label="实际值" width="140" align="right" prop="actual" />
          <el-table-column label="差异" width="140" align="right">
            <template #default="s">
              <span :class="{ 'diff-red': Number(s.row.diff) !== 0 }">{{ s.row.diff }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结论" width="100" align="center">
            <template #default="s">
              <el-tag v-if="s.row.expected === '结转'" type="info">结转下期</el-tag>
              <el-tag v-else :type="Number(s.row.diff) === 0 ? 'success' : 'danger'">{{ Number(s.row.diff) === 0 ? '平' : '差异' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-alert v-if="rec.allZero" type="success" :closable="false" show-icon title="本期对账全部为 0，流量账闭环" class="mt4" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="Stock">
import { listStock, reconcileStock } from '@/api/bonded/biz'
const tab = ref('ledger')
const rows = ref([]); const loading = ref(false); const total = ref(0)
const markTypes = [
  { value: '1', label: '待维修货物' }, { value: '2', label: '已维修货物' }, { value: '3', label: '无法维修货物' },
  { value: '4', label: '维修用料件' }, { value: '5', label: '旧件或坏件' }, { value: '6', label: '边角料' },
  { value: '8', label: '维修服务费' }
]
const queryParams = reactive({ pageNum: 1, pageSize: 10, materialName: null, bizType: null, markType: null })
const recQuery = reactive({ batchNo: null, workNo: null })
const rec = ref({ cards: [], rows: [], allZero: null })

function getList() { loading.value = true; listStock(queryParams).then(r => { rows.value = r.rows; total.value = r.total; loading.value = false }) }
function getReconcile() { reconcileStock(recQuery).then(r => { rec.value = r.data }) }

getList()
</script>

<style scoped>
.card-label { font-size: 12px; color: #909399; }
.card-qty { font-size: 18px; font-weight: bold; margin-top: 4px; }
.card-alert { color: #f56c6c; }
.diff-red { color: #f56c6c; font-weight: bold; }
</style>
