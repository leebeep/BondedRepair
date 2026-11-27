<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <section class="hero">
      <div class="hero-text">
        <h2>{{ greeting }}，{{ userStore.name }}</h2>
        <p>{{ todayText }} · 保税维修综合管理平台</p>
      </div>
      <div class="hero-brief" v-loading="loading">
        <div class="hero-item">
          <div class="hero-num">{{ stats.workOrder.active }}</div>
          <div class="hero-label">进行中工单</div>
        </div>
        <div class="hero-item">
          <div class="hero-num">{{ declarePending }}</div>
          <div class="hero-label">待回执申报</div>
        </div>
        <div class="hero-item">
          <div class="hero-num">{{ todoDocs }}</div>
          <div class="hero-label">待办单据</div>
        </div>
      </div>
    </section>

    <!-- 指标卡片 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="8" :lg="4" v-for="k in kpis" :key="k.key">
        <div class="kpi-card" :class="'kpi-' + k.tone" @click="go(k.path)">
          <div class="kpi-icon"><el-icon :size="20"><component :is="k.icon" /></el-icon></div>
          <div class="kpi-main">
            <div class="kpi-num" v-loading="loading">{{ k.value }}</div>
            <div class="kpi-title">{{ k.title }}</div>
            <div class="kpi-sub">{{ k.sub }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷导航 -->
    <el-card shadow="never" class="panel">
      <template #header><span class="panel-title">快捷导航</span></template>
      <div class="nav-grid">
        <div class="nav-item" v-for="n in navs" :key="n.path" @click="go(n.path)">
          <el-icon :size="22"><component :is="n.icon" /></el-icon>
          <span>{{ n.label }}</span>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <!-- 近 7 日出入库趋势 -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel">
          <template #header><span class="panel-title">近 7 日出入库趋势</span></template>
          <div ref="trendRef" class="trend-chart" />
        </el-card>
      </el-col>
      <!-- 六类账存 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel">
          <template #header>
            <span class="panel-title">六类货物账存</span>
            <el-tag v-if="stockAlert" type="danger" size="small" effect="plain" class="panel-tag">存在差异账存</el-tag>
          </template>
          <div class="stock-list" v-loading="loading">
            <div class="stock-row" v-for="c in stockCards" :key="c.markType">
              <span class="stock-name">{{ markLabel(c.markType) }}</span>
              <span class="stock-qty" :class="{ alert: isAlert(c.markType, c.balance) }">{{ c.balance ?? 0 }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 最近申报动态 -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel">
          <template #header>
            <span class="panel-title">最近申报动态</span>
            <el-link type="primary" :underline="false" @click="go('/bonded/declare')">查看全部</el-link>
          </template>
          <div class="log-list" v-loading="loading">
            <div class="log-row" v-for="(l, i) in recentLogs" :key="i">
              <el-tag :type="declResultType(l.resultStatus)" size="small" effect="light" class="log-tag">{{ declResultLabel(l.resultStatus) }}</el-tag>
              <span class="log-biz">{{ bizLabel(l.bizType) }}</span>
              <span class="log-msg" :title="l.msg">{{ l.msg || '-' }}</span>
              <span class="log-time">{{ l.createTime }}</span>
            </div>
            <el-empty v-if="!loading && recentLogs.length === 0" description="暂无申报记录" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <!-- 申报回执概览 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel">
          <template #header><span class="panel-title">申报回执概览</span></template>
          <div class="decl-list" v-loading="loading">
            <div class="decl-row" v-for="d in declSummary" :key="d.label">
              <span class="decl-dot" :class="d.tone"></span>
              <span>{{ d.label }}</span>
              <span class="decl-num">{{ d.value }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index">
import * as echarts from 'echarts'
import { useResizeObserver } from '@vueuse/core'
import { dashboardStats } from '@/api/bonded/biz'
import { declareBizTypes, declResultLabel, declResultType } from '@/utils/bonded'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'

const router = useRouter()
const userStore = useUserStore()
const settingsStore = useSettingsStore()

const loading = ref(true)
const stats = ref({ workOrder: {}, flow: {}, declare: {}, stockCards: [], trend: [], recentLogs: [] })

const markMap = {
  '1': '待维修货物', '2': '已维修货物', '3': '无法维修货物',
  '4': '维修用料件', '5': '旧件或坏件', '6': '边角料', '8': '维修服务费'
}
const markLabel = t => markMap[t] || t
// 1/2/3/5/6 类账存应为 0，非 0 即差异
const isAlert = (t, v) => t !== '4' && t !== '8' && Number(v) !== 0
const stockAlert = computed(() => stats.value.stockCards.some(c => isAlert(c.markType, c.balance)))
const bizLabel = v => declareBizTypes.find(d => d.value === v)?.label || v
const declarePending = computed(() => stats.value.declare.pending || 0)
const todoDocs = computed(() => {
  const f = stats.value.flow
  return (f.IN?.pendingAudit || 0) + (f.IN?.pendingDeclare || 0) + (f.OUT?.pendingAudit || 0) + (f.OUT?.pendingDeclare || 0)
})

const declSummary = computed(() => {
  const d = stats.value.declare
  return [
    { label: '待回执', value: d.pending || 0, tone: 'blue' },
    { label: '通过/入库成功', value: d.success || 0, tone: 'green' },
    { label: '转人工审核', value: d.manual || 0, tone: 'orange' },
    { label: '退单/入库失败', value: d.fail || 0, tone: 'red' }
  ]
})

const kpis = computed(() => {
  const m = stats.value.material || {}
  const byStatus = m.byStatus || {}
  const w = stats.value.workOrder || {}
  const f = stats.value.flow
  const stockTotal = (stats.value.stockCards || []).reduce((s, c) => s + Number(c.balance || 0), 0)
  return [
    { key: 'material', icon: 'Document', tone: 'blue', title: '物料备案', value: m.total ?? '-', sub: `待申报 ${byStatus['0'] || 0} · 通过 ${byStatus['2'] || 0}`, path: '/bonded/material' },
    { key: 'workorder', icon: 'Tickets', tone: 'cyan', title: '维修工单', value: w.active ?? '-', sub: `累计 ${w.total ?? 0} 单`, path: '/bonded/issue' },
    { key: 'in', icon: 'Download', tone: 'green', title: '入库单', value: f.IN?.total ?? '-', sub: `今日 ${f.IN?.today || 0} · 待审 ${f.IN?.pendingAudit || 0}`, path: '/bonded/inbound' },
    { key: 'out', icon: 'Upload', tone: 'orange', title: '出库单', value: f.OUT?.total ?? '-', sub: `今日 ${f.OUT?.today || 0} · 退单 ${f.OUT?.back || 0}`, path: '/bonded/outbound' },
    { key: 'declare', icon: 'Message', tone: 'purple', title: '申报待回执', value: stats.value.declare.pending ?? '-', sub: `异常 ${stats.value.declare.fail || 0}`, path: '/bonded/declare' },
    { key: 'stock', icon: 'DataLine', tone: 'red', title: '在库总量', value: stockTotal, sub: '六类货物合计', path: '/bonded/stock' }
  ]
})

const navs = [
  { label: '物料备案', icon: 'Document', path: '/bonded/material' },
  { label: '入库管理', icon: 'Download', path: '/bonded/inbound' },
  { label: '工单发料', icon: 'SetUp', path: '/bonded/issue' },
  { label: '工单收料', icon: 'Box', path: '/bonded/receive' },
  { label: '出库管理', icon: 'Upload', path: '/bonded/outbound' },
  { label: '库存台账', icon: 'DataLine', path: '/bonded/stock' },
  { label: '申报管理', icon: 'Message', path: '/bonded/declare' },
  { label: '用户管理', icon: 'User', path: '/system/user' }
]

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const todayText = computed(() => {
  const d = new Date()
  const weeks = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日 星期${weeks[d.getDay()]}`
})

function go(path) { router.push(path) }

// ---------- 趋势图 ----------
const trendRef = ref(null)
let chart = null
const chartTheme = computed(() => (settingsStore.isDark
  ? { textColor: '#a3a6ad', splitLine: '#2b2f36' }
  : { textColor: '#64748b', splitLine: '#e2e8f0' }))

function renderChart() {
  if (!trendRef.value) return
  const dates = stats.value.trend.map(t => t.date)
  const inQty = stats.value.trend.map(t => Number(t.inQty) || 0)
  const outQty = stats.value.trend.map(t => Number(t.outQty) || 0)
  const th = chartTheme.value
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库', '出库'], right: 0, textStyle: { color: th.textColor } },
    grid: { left: 8, right: 8, top: 36, bottom: 0, containLabel: true },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: th.splitLine } }, axisLabel: { color: th.textColor } },
    yAxis: { type: 'value', axisLabel: { color: th.textColor }, splitLine: { lineStyle: { color: th.splitLine } } },
    series: [
      { name: '入库', type: 'bar', barMaxWidth: 18, itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] }, data: inQty },
      { name: '出库', type: 'line', smooth: true, symbolSize: 6, itemStyle: { color: '#f59e0b' }, lineStyle: { width: 2 }, data: outQty }
    ]
  }
  if (!chart) chart = echarts.init(trendRef.value)
  chart.setOption(option, true)
}

function getStats() {
  loading.value = true
  dashboardStats().then(r => {
    stats.value = r.data || {}
    nextTick(renderChart)
  }).finally(() => { loading.value = false })
}

watch(() => settingsStore.isDark, () => nextTick(renderChart))
useResizeObserver(trendRef, () => chart && chart.resize())

onMounted(getStats)
onBeforeUnmount(() => { chart && chart.dispose(); chart = null })
</script>

<style scoped>
.dashboard {
  padding: 16px;
  background: var(--br-bg);
  min-height: calc(100vh - 84px);
}

/* 欢迎横幅 */
.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 24px 28px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: linear-gradient(120deg, #1e3a5f 0%, #2563eb 100%);
  color: #fff;
}
.hero h2 { margin: 0 0 6px; font-size: 22px; font-weight: 700; }
.hero p { margin: 0; opacity: .85; font-size: 13px; }
.hero-brief { display: flex; gap: 32px; }
.hero-item { text-align: center; cursor: default; }
.hero-num { font-size: 26px; font-weight: 700; line-height: 1.2; }
.hero-label { font-size: 12px; opacity: .8; margin-top: 2px; }

/* 指标卡片 */
.kpi-row { margin-bottom: 16px; }
.kpi-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  margin-bottom: 8px;
  background: var(--br-card);
  border: 1px solid var(--br-border);
  border-radius: 12px;
  cursor: pointer;
  transition: box-shadow .2s, transform .2s;
}
.kpi-card:hover { box-shadow: 0 6px 16px rgba(15, 23, 42, .1); transform: translateY(-2px); }
.kpi-icon {
  width: 42px; height: 42px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 10px;
}
.kpi-blue .kpi-icon { background: rgba(37, 99, 235, .12); color: #2563eb; }
.kpi-cyan .kpi-icon { background: rgba(6, 182, 212, .12); color: #0891b2; }
.kpi-green .kpi-icon { background: rgba(22, 163, 74, .12); color: #16a34a; }
.kpi-orange .kpi-icon { background: rgba(245, 158, 11, .14); color: #d97706; }
.kpi-purple .kpi-icon { background: rgba(124, 58, 237, .12); color: #7c3aed; }
.kpi-red .kpi-icon { background: rgba(239, 68, 68, .12); color: #dc2626; }
.kpi-num { font-size: 22px; font-weight: 700; color: var(--br-text); line-height: 1.2; }
.kpi-title { font-size: 13px; color: var(--br-text); margin-top: 2px; }
.kpi-sub { font-size: 12px; color: var(--br-muted); margin-top: 2px; }

/* 面板 */
.panel { margin-bottom: 16px; border-radius: 12px; border: 1px solid var(--br-border); background: var(--br-card); }
.panel :deep(.el-card__header) { padding: 14px 20px; border-bottom: 1px solid var(--br-border); display: flex; align-items: center; justify-content: space-between; }
.panel :deep(.el-card__body) { padding: 16px 20px; }
.panel-title { font-size: 15px; font-weight: 600; color: var(--br-text); }
.panel-tag { margin-left: 10px; }

/* 快捷导航 */
.nav-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(110px, 1fr)); gap: 12px; }
.nav-item {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 16px 8px; border-radius: 10px;
  color: var(--br-text); font-size: 13px;
  border: 1px solid transparent;
  transition: background .2s, border-color .2s, color .2s;
}
.nav-item .el-icon { color: #2563eb; }
.nav-item:hover { background: var(--br-hover); border-color: var(--br-border); }

/* 趋势图 */
.trend-chart { height: 280px; }

/* 六类账存 */
.stock-list { display: flex; flex-direction: column; }
.stock-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 11px 4px; border-bottom: 1px dashed var(--br-border); font-size: 13px;
  color: var(--br-text);
}
.stock-row:last-child { border-bottom: none; }
.stock-qty { font-weight: 700; font-variant-numeric: tabular-nums; }
.stock-qty.alert { color: #dc2626; }

/* 申报动态 */
.log-list { display: flex; flex-direction: column; }
.log-row {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 4px; border-bottom: 1px dashed var(--br-border); font-size: 13px;
}
.log-row:last-child { border-bottom: none; }
.log-tag { flex-shrink: 0; width: 76px; justify-content: center; }
.log-biz { color: var(--br-text); flex-shrink: 0; }
.log-msg { flex: 1; color: var(--br-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.log-time { color: var(--br-muted); font-size: 12px; flex-shrink: 0; }

/* 回执概览 */
.decl-list { display: flex; flex-direction: column; }
.decl-row {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 4px; border-bottom: 1px dashed var(--br-border);
  font-size: 13px; color: var(--br-text);
}
.decl-row:last-child { border-bottom: none; }
.decl-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.decl-dot.blue { background: #2563eb; }
.decl-dot.green { background: #16a34a; }
.decl-dot.orange { background: #f59e0b; }
.decl-dot.red { background: #dc2626; }
.decl-num { margin-left: auto; font-weight: 700; font-variant-numeric: tabular-nums; }
</style>
