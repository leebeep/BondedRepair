<template>
  <div class="p2">
    <!-- 五类报文状态列 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="4" v-for="b in bizTypes" :key="b.value">
        <el-card shadow="hover">
          <template #header><b>{{ b.value }}</b> {{ b.label }}</template>
          <div v-if="board[b.value]">
            <div v-for="(v, k) in board[b.value]" :key="k" class="board-line">
              <el-tag :type="declResultType(k)" size="small">{{ declResultLabel(k) }}</el-tag>
              <span class="board-cnt">{{ v }}</span>
            </div>
          </div>
          <div v-else class="board-empty">暂无申报</div>
        </el-card>
      </el-col>
    </el-row>

    <el-form :inline="true" label-width="90px">
      <el-form-item label="报文类型">
        <el-select v-model="logQuery.bizType" clearable placeholder="全部" style="width: 150px">
          <el-option v-for="b in bizTypes" :key="b.value" :label="b.value + '-' + b.label" :value="b.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="回执状态">
        <el-select v-model="logQuery.resultStatus" clearable placeholder="全部" style="width: 150px">
          <el-option label="待回执" value="PENDING" />
          <el-option label="通过" value="Y" /><el-option label="入库成功" value="1" />
          <el-option label="退单" value="N" /><el-option label="入库失败" value="2" /><el-option label="退单(3)" value="3" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="getLogs">查询</el-button></el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="logs">
      <el-table-column label="时间" prop="createTime" width="160" />
      <el-table-column label="报文类型" prop="bizType" width="100" align="center" />
      <el-table-column label="业务单据" width="220">
        <template #default="s">{{ s.row.bizTable }} #{{ s.row.bizId }}</template>
      </el-table-column>
      <el-table-column label="镜像表" prop="mirrorTable" width="130" />
      <el-table-column label="执行类型" width="90" align="center">
        <template #default="s">{{ { '1': '备案', '2': '变更', '3': '作废' }[s.row.executeType] || s.row.executeType }}</template>
      </el-table-column>
      <el-table-column label="预录入编号" prop="uuid" width="180" />
      <el-table-column label="回执状态" width="110" align="center">
        <template #default="s"><el-tag :type="declResultType(s.row.resultStatus || 'PENDING')">{{ declResultLabel(s.row.resultStatus || 'PENDING') }}</el-tag></template>
      </el-table-column>
      <el-table-column label="回执消息" prop="msg" show-overflow-tooltip />
      <el-table-column label="回执时间" prop="resultTime" width="160" />
    </el-table>
    <pagination v-show="logTotal > 0" :total="logTotal" v-model:page="logQuery.pageNum" v-model:limit="logQuery.pageSize" @pagination="getLogs" />
  </div>
</template>

<script setup name="DeclareBoard">
import { declareBoard, listDeclareLog } from '@/api/bonded/biz'
import { declareBizTypes as bizTypes, declResultLabel, declResultType } from '@/utils/bonded'
const board = ref({})
const logs = ref([]); const loading = ref(false); const logTotal = ref(0)
const logQuery = reactive({ pageNum: 1, pageSize: 10, bizType: null, resultStatus: null })

function getBoard() {
  declareBoard().then(r => {
    const m = {}
    for (const row of r.data) {
      const key = row.bizType; const st = row.resultStatus || 'PENDING'
      if (!m[key]) { m[key] = {} }
      m[key][st] = Number(row.cnt)
    }
    board.value = m
  })
}
function getLogs() { loading.value = true; listDeclareLog(logQuery).then(r => { logs.value = r.rows; logTotal.value = r.total; loading.value = false }) }

getBoard(); getLogs()
// 60s 自动刷新看板
setInterval(getBoard, 60000)
</script>

<style scoped>
.board-line { display: flex; justify-content: space-between; align-items: center; padding: 3px 0; }
.board-cnt { font-weight: bold; font-size: 15px; }
.board-empty { color: #999; font-size: 13px; }
</style>
