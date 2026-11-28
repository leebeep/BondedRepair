<template>
  <div class="p2">
    <!-- 自动申报开关 + 补偿操作 -->
    <el-card shadow="never" class="mb16">
      <div class="switch-bar">
        <div class="switch-left">
          <span class="switch-title">自动申报</span>
          <el-switch v-model="autoPush" :disabled="!canEdit" @change="onSwitchChange" />
          <span class="switch-desc">{{ autoPush ? '开：单据审核通过 / 物料保存后自动申报（写入镜像库），由海关客户端采集' : '关：单据滞留待申报，可通过【立即补报待申报】人工申报' }}</span>
        </div>
        <div class="switch-actions" v-if="canEdit">
          <el-button icon="Refresh" :loading="pushing" :disabled="!autoPush" :title="autoPush ? '将开关关闭期间滞留的待申报单据（审核通过但未写入镜像库）批量补报' : '自动申报已关闭，开关打开后才可补报'" @click="onPushPending">立即补报待申报</el-button>
          <el-button icon="Search" :loading="querying" title="调海关公共服务接口查询未回执单据的最新状态，用于核对/刷新回执" @click="onQueryReceipt">立即主动查询</el-button>
        </div>
      </div>
    </el-card>

    <!-- 采集状态看板（按报文类型） -->
    <el-tabs v-model="activeTab" class="mb8">
      <el-tab-pane label="日志清单" name="logs" />
      <el-tab-pane label="统计概览" name="stats" />
    </el-tabs>

    <div v-show="activeTab === 'stats'">
    <el-row :gutter="10" class="mb8">
      <el-col :xs="12" :md="4" :lg="4" v-for="b in bizTypes" :key="b.value">
        <el-card shadow="hover" class="board-card">
          <template #header><b>{{ b.value }}</b> {{ b.label }}</template>
          <div v-if="syncBoard[b.value]">
            <div v-for="s in customsOrder" :key="s" class="board-line">
              <el-tag :type="customsType(s)" size="small">{{ customsLabel(s) }}</el-tag>
              <span class="board-cnt" v-loading="!syncBoard[b.value]">{{ syncBoard[b.value][s] || 0 }}</span>
            </div>
          </div>
          <div v-else class="board-empty">暂无数据</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 回执结果看板（镜像回执） -->
    <el-row :gutter="10" class="mb8">
      <el-col :xs="12" :md="4" :lg="4" v-for="b in bizTypes" :key="'r' + b.value">
        <el-card shadow="hover" class="board-card">
          <template #header>回执结果 <b>{{ b.value }}</b></template>
          <div v-if="board[b.value]">
            <div v-for="(v, k) in board[b.value]" :key="k" class="board-line">
              <el-tag :type="declResultType(k)" size="small">{{ declResultLabel(k) }}</el-tag>
              <span class="board-cnt">{{ v }}</span>
            </div>
          </div>
          <div v-else class="board-empty">暂无回执</div>
        </el-card>
      </el-col>
    </el-row>

    </div>

    <div v-show="activeTab === 'logs'">
        <el-form :inline="true" label-width="90px">
      <el-form-item label="报文类型">
        <el-select v-model="logQuery.bizType" clearable placeholder="全部" style="width: 150px">
          <el-option v-for="b in bizTypes" :key="b.value" :label="b.value + '-' + b.label" :value="b.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="单证编号">
        <el-input v-model="logQuery.bizNo" clearable placeholder="入库/发料/收料/出库单号" style="width: 190px" @keyup.enter="getLogs" />
      </el-form-item>
      <el-form-item label="预录入编号">
        <el-input v-model="logQuery.uuid" clearable placeholder="回执预录入编号" style="width: 180px" @keyup.enter="getLogs" />
      </el-form-item>
      <el-form-item label="执行类型">
        <el-select v-model="logQuery.executeType" clearable placeholder="全部" style="width: 120px">
          <el-option label="备案" value="1" /><el-option label="变更" value="2" /><el-option label="作废" value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="回执状态">
        <el-select v-model="logQuery.resultStatus" clearable placeholder="全部" style="width: 150px">
          <el-option label="待回执" value="PENDING" />
          <el-option label="通过" value="Y" /><el-option label="入库成功" value="1" />
          <el-option label="退单" value="N" /><el-option label="入库失败" value="2" /><el-option label="退单(3)" value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="申报时间">
        <el-date-picker v-model="logDateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="-" start-placeholder="开始" end-placeholder="结束" style="width: 240px" />
      </el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="getLogs">查询</el-button></el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="logs">
      <el-table-column label="报文类型" prop="bizType" width="90" align="center" />
      <el-table-column label="单证类型" width="110" align="center">
        <template #default="s">{{ bizLabel(s.row.bizType) }}</template>
      </el-table-column>
      <el-table-column label="单证编号" min-width="170" header-align="center" show-overflow-tooltip>
        <template #default="s">
          <el-link v-if="s.row.bizId" type="primary" :underline="false" @click="goDoc(s.row)">{{ s.row.bizNo || (s.row.bizTable + ' #' + s.row.bizId) }}</el-link>
          <span v-else>{{ s.row.bizNo || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="执行类型" width="80" align="center">
        <template #default="s">{{ { '1': '备案', '2': '变更', '3': '作废' }[s.row.executeType] || s.row.executeType }}</template>
      </el-table-column>
      <el-table-column label="预录入编号" prop="uuid" width="170" show-overflow-tooltip>
        <template #default="s">{{ s.row.uuid || '-' }}</template>
      </el-table-column>
      <el-table-column label="申报时间" prop="createTime" width="160">
        <template #default="s">{{ fmtTime(s.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="回执状态" width="100" align="center">
        <template #default="s"><el-tag :type="declResultType(s.row.resultStatus || 'PENDING')">{{ declResultLabel(s.row.resultStatus || 'PENDING') }}</el-tag></template>
      </el-table-column>
      <el-table-column label="回执时间" prop="resultTime" width="160">
        <template #default="s">{{ s.row.resultTime ? fmtTime(s.row.resultTime) : '-' }}</template>
      </el-table-column>
      <el-table-column label="回执消息" prop="msg" min-width="160" show-overflow-tooltip>
        <template #default="s">{{ s.row.msg || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="s">
          <el-button link type="primary" @click="goDoc(s.row)">单证详情</el-button>
          <el-button link type="primary" @click="openReceipt(s.row)">回执详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="logTotal > 0" :total="logTotal" v-model:page="logQuery.pageNum" v-model:limit="logQuery.pageSize" @pagination="getLogs" />

    <!-- 回执详情弹窗 -->
    <el-dialog v-model="receiptOpen" title="回执详情" width="640px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="报文类型">{{ receiptRow?.bizType }}</el-descriptions-item>
        <el-descriptions-item label="执行类型">{{ { '1': '备案', '2': '变更', '3': '作废' }[receiptRow?.executeType] || receiptRow?.executeType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单证类型">{{ bizLabel(receiptRow?.bizType) }}</el-descriptions-item>
        <el-descriptions-item label="单证编号">{{ receiptRow?.bizNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预录入编号">{{ receiptRow?.uuid || '-' }}</el-descriptions-item>
        <el-descriptions-item label="回执状态"><el-tag :type="declResultType(receiptRow?.resultStatus || 'PENDING')">{{ declResultLabel(receiptRow?.resultStatus || 'PENDING') }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="申报时间">{{ fmtTime(receiptRow?.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="回执时间">{{ receiptRow?.resultTime ? fmtTime(receiptRow.resultTime) : '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="msg-title">回执消息</div>
      <div class="msg-box">{{ receiptRow?.msg || '（无回执消息）' }}</div>
    </el-dialog>
    </div>
  </div>
</template>

<script setup name="DeclareBoard">
import { declareBoard, listDeclareLog, getPushSwitch, setPushSwitch, pushPending, queryReceipt, syncBoard as fetchSyncBoard } from '@/api/bonded/biz'
import { declareBizTypes as bizTypes, declResultLabel, declResultType } from '@/utils/bonded'
import useUserStore from '@/store/modules/user'
import { ElMessage } from 'element-plus'

const board = ref({})
const activeTab = ref('logs')
const logs = ref([]); const loading = ref(false); const logTotal = ref(0)
const logQuery = reactive({ pageNum: 1, pageSize: 10, bizType: null, resultStatus: null, bizNo: null, uuid: null, executeType: null })
const logDateRange = ref([])

// 单证类型标签 / 单证详情跳转路由（EPD101 → 物料详情）
const bizLabel = v => (bizTypes.find(b => b.value === v) || {}).label || v || '-'
const docRouteMap = {
  EPD101: '/bonded/material-detail/index/',
  PTO2: '/bonded/inbound-detail/index/',
  RRD2: '/bonded/issue-detail/index/',
  MSO2: '/bonded/receive-detail/index/',
  SDO2: '/bonded/outbound-detail/index/'
}
const router = useRouter()
function goDoc(row) { const base = docRouteMap[row.bizType]; if (base && row.bizId) router.push(base + row.bizId) }

// 回执详情弹窗
const receiptOpen = ref(false)
const receiptRow = ref(null)
function openReceipt(row) { receiptRow.value = row; receiptOpen.value = true }

// 时间展示（去掉毫秒）
function fmtTime(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }

// 自动申报开关
const autoPush = ref(false)
const canEdit = ref(false)
const pushing = ref(false)
const querying = ref(false)

// 海关采集状态标签
const customsMap = {
  '00': { label: '未同步', type: 'info' },
  '10': { label: '待采集', type: 'primary' },
  '20': { label: '已采集', type: 'primary' },
  '21': { label: '申报中', type: 'warning' },
  '90': { label: '采集超时', type: 'danger' },
  '2Y': { label: '通过', type: 'success' },
  '22': { label: '通过', type: 'success' },
  '2N': { label: '退单', type: 'danger' },
  '23': { label: '退单', type: 'danger' },
  '24': { label: '退单', type: 'danger' },
  '29': { label: '已作废', type: 'info' }
}
const customsOrder = ['00', '10', '20', '21', '90', '2Y', '22', '2N', '24', '29']
const customsLabel = k => customsMap[k]?.label || k
const customsType = k => customsMap[k]?.type || 'info'

const syncBoard = ref({})

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

function getSyncBoard() {
  fetchSyncBoard().then(r => {
    const m = {}
    for (const row of r.data) {
      const key = row.bizType
      if (!m[key]) { m[key] = {} }
      m[key][row.k] = Number(row.cnt)
    }
    syncBoard.value = m
  })
}

function getLogs() {
  loading.value = true
  const [beginTime, endTime] = logDateRange.value || []
  listDeclareLog({ ...logQuery, beginTime: beginTime || null, endTime: endTime || null }).then(r => { logs.value = r.rows; logTotal.value = r.total; loading.value = false })
}

function initSwitch() {
  getPushSwitch().then(r => { autoPush.value = r.data === true })
  // 有编辑权限才显示开关操作（无权限时接口 403，按钮隐藏即可）
  canEdit.value = useUserStore().permissions.some(p => p === '*:*:*' || p === 'bonded:declare:edit')
}

function onSwitchChange(v) {
  setPushSwitch(v).then(() => {
    ElMessage.success(v ? '自动申报已开启：审核通过/物料保存将自动申报（写入镜像库）' : '自动申报已关闭：单据滞留待申报，可随时补报')
  }).catch(() => { autoPush.value = !v })
}

function onPushPending() {
  pushing.value = true
  pushPending().then(r => {
    ElMessage.success(r.msg)
    getSyncBoard()
  }).finally(() => { pushing.value = false })
}

function onQueryReceipt() {
  querying.value = true
  queryReceipt().then(r => {
    ElMessage.success(r.msg)
  }).finally(() => { querying.value = false })
}

initSwitch()
getBoard(); getSyncBoard(); getLogs()
// 60s 自动刷新看板
setInterval(() => { getBoard(); getSyncBoard() }, 60000)
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.switch-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.switch-title { font-weight: 600; margin-right: 8px; }
.switch-desc { color: var(--br-muted, #64748b); font-size: 12px; margin-left: 10px; }
.board-line { display: flex; justify-content: space-between; align-items: center; padding: 3px 0; }
.board-cnt { font-weight: bold; font-size: 15px; }
.board-empty { color: #999; font-size: 13px; }
.msg-title { font-weight: 600; margin: 12px 0 6px; }
.msg-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 4px; padding: 10px 12px; font-size: 13px; line-height: 1.6; word-break: break-all; white-space: pre-wrap; min-height: 48px; }
</style>