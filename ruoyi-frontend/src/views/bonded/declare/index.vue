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
          <el-tooltip :disabled="queryReady" content="主动查询不可用：公服查询接口未配置完整。公服平台注册后，在 参数设置 填写 br.query.api.appKey 与 br.query.api.privateKey（接口地址已预置），保存后约 1 分钟内自动启用" placement="top">
            <span style="margin-left: 12px; display: inline-block">
              <el-button icon="Search" :loading="querying" :disabled="!queryReady || querying" @click="onQueryReceipt">立即主动查询</el-button>
            </span>
          </el-tooltip>
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
          <template #header><b>{{ b.value }}</b> {{ b.label }}<span class="flow-tag sync">→ 我方申报</span></template>
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

    <!-- 回执结果看板（镜像回执），点击状态行可跳到日志清单过滤 -->
    <el-row :gutter="10" class="mb8">
      <el-col :xs="12" :md="4" :lg="4" v-for="b in bizTypes" :key="'r' + b.value">
        <el-card shadow="hover" class="board-card">
          <template #header><b>{{ b.value }}</b><span class="flow-tag rcv">← 海关回执</span></template>
          <div v-if="board[b.value]">
            <div v-for="(v, k) in board[b.value]" :key="k" class="board-line clickable" title="点击按该回执状态过滤日志清单" @click="filterLogsByStatus(k)">
              <el-tag :type="declResultType(k)" :icon="declResultIcon(k)" size="small">{{ declResultLabel(k) }}</el-tag>
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
        <el-select v-model="logQuery.bizType" clearable multiple collapse-tags placeholder="全部" style="width: 180px">
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
        <el-select v-model="logQuery.executeType" clearable multiple collapse-tags placeholder="全部" style="width: 140px">
          <el-option label="备案" value="1" /><el-option label="变更" value="2" /><el-option label="作废" value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="回执状态">
        <el-select v-model="logQuery.resultStatus" clearable multiple collapse-tags placeholder="全部" style="width: 170px">
          <el-option v-for="r in resultStatusOptions" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="申报时间">
        <el-date-picker v-model="logDateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="-" start-placeholder="开始" end-placeholder="结束" style="width: 240px" />
      </el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="getLogs">查询</el-button></el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="logs" @filter-change="onFilterChange">
      <el-table-column label="报文类型" prop="bizType" width="90" align="center" column-key="bizType" :filters="bizTypeFilters" />
      <el-table-column label="单证类型" width="110" align="center">
        <template #default="s">{{ bizLabel(s.row.bizType) }}</template>
      </el-table-column>
      <el-table-column label="单证编号" min-width="170" show-overflow-tooltip align="center">
        <template #default="s">
          <el-link v-if="s.row.bizId" type="primary" :underline="false" @click="goDoc(s.row)">{{ s.row.bizNo || (s.row.bizTable + ' #' + s.row.bizId) }}</el-link>
          <span v-else>{{ s.row.bizNo || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="执行类型" width="80" align="center" column-key="executeType" :filters="executeTypeFilters">
        <template #default="s">{{ { '1': '备案', '2': '变更', '3': '作废' }[s.row.executeType] || s.row.executeType }}</template>
      </el-table-column>
      <el-table-column label="预录入编号" prop="uuid" width="170" show-overflow-tooltip align="center">
        <template #default="s">{{ s.row.uuid || '-' }}</template>
      </el-table-column>
      <el-table-column label="申报时间" width="110" align="center">
        <template #default="s">
          <el-tooltip :content="fmtTimeFull(s.row.createTime)" placement="top"><span>{{ fmtTimeShort(s.row.createTime) }}</span></el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="回执状态" width="120" align="center" column-key="resultStatus" :filters="resultStatusFilters">
        <template #default="s"><el-tag :type="declResultType(s.row.resultStatus || 'PENDING')" :icon="declResultIcon(s.row.resultStatus)">{{ declResultLabel(s.row.resultStatus || 'PENDING') }}</el-tag></template>
      </el-table-column>
      <el-table-column label="回执时间" width="110" align="center">
        <template #default="s">
          <el-tooltip :content="fmtTimeFull(s.row.resultTime)" placement="top"><span>{{ s.row.resultTime ? fmtTimeShort(s.row.resultTime) : '-' }}</span></el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="回执消息" prop="msg" min-width="160" show-overflow-tooltip align="center">
        <template #default="s">{{ s.row.msg || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template #default="s">
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
import { declareBoard, listDeclareLog, getPushSwitch, setPushSwitch, pushPending, queryReceipt, queryStatus, syncBoard as fetchSyncBoard } from '@/api/bonded/biz'
import { declareBizTypes as bizTypes, declareResultMap, declResultLabel, declResultType, declResultIcon, fmtTimeShort, fmtTimeFull, enumToFilters, applyFilterChange, packListQuery } from '@/utils/bonded'
import useUserStore from '@/store/modules/user'
import { ElMessage } from 'element-plus'

const board = ref({})
const activeTab = ref('logs')
const logs = ref([]); const loading = ref(false); const logTotal = ref(0)
const logQuery = reactive({ pageNum: 1, pageSize: 10, bizType: [], resultStatus: [], bizNo: null, uuid: null, executeType: [] })
const logDateRange = ref([])

// 表头列筛选项（与顶部下拉共用同一参数）
const bizTypeFilters = enumToFilters(bizTypes)
const executeTypeFilters = enumToFilters([{ value: '1', label: '备案' }, { value: '2', label: '变更' }, { value: '3', label: '作废' }])
const resultStatusFilters = enumToFilters(declareResultMap)
const resultStatusOptions = resultStatusFilters.map(f => ({ value: f.value, label: f.text }))

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
// 主动查询可用性：公服 url/appKey 已配置才启用按钮
const queryReady = ref(false)
function getQueryStatus() {
  queryStatus().then(r => { queryReady.value = !!r.data }).catch(() => { queryReady.value = false })
}

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
  listDeclareLog({ ...packListQuery(logQuery), beginTime: beginTime || null, endTime: endTime || null }).then(r => { logs.value = r.rows; logTotal.value = r.total; loading.value = false })
}

// 表头列筛选（远端）：勾选值写入 logQuery 对应 xxxList，按全量数据过滤
function onFilterChange(filters) { applyFilterChange(logQuery, filters); logQuery.pageNum = 1; getLogs() }

// 看板联动：点击回执结果状态 → 切到日志清单并按该状态过滤
function filterLogsByStatus(k) {
  activeTab.value = 'logs'
  logQuery.resultStatus = [k]
  logQuery.pageNum = 1
  getLogs()
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
getBoard(); getSyncBoard(); getLogs(); getQueryStatus()
// 60s 自动刷新看板
setInterval(() => { getBoard(); getSyncBoard(); getQueryStatus() }, 60000)
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.switch-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.switch-title { font-weight: 600; margin-right: 8px; }
.switch-desc { color: var(--el-text-color-secondary); font-size: 12px; margin-left: 10px; }
.board-line { display: flex; justify-content: space-between; align-items: center; padding: 3px 0; }
.board-line.clickable { cursor: pointer; border-radius: 4px; padding: 3px 6px; transition: background-color 0.15s; }
.board-line.clickable:hover { background: var(--el-fill-color-light); }
.board-cnt { font-weight: bold; font-size: 15px; }
.board-empty { color: var(--el-text-color-secondary); font-size: 13px; }
.flow-tag { float: right; font-size: 11px; color: var(--el-text-color-secondary); font-weight: normal; }
.flow-tag.sync { color: var(--el-color-primary); }
.flow-tag.rcv { color: var(--el-color-success); margin-left: 8px; }
.msg-title { font-weight: 600; margin: 12px 0 6px; }
.msg-box { background: var(--el-fill-color-light); border: 1px solid var(--el-border-color-lighter); border-radius: 4px; padding: 10px 12px; font-size: 13px; line-height: 1.6; word-break: break-all; white-space: pre-wrap; min-height: 48px; }
</style>