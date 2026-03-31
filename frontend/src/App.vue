<template>
  <div class="page">
    <header class="hero">
      <h1>分库分表与数据同步学习台</h1>
      <p>
        覆盖 ShardingSphere-JDBC、Debezium、Kafka、Kafka Connect、Elasticsearch 的完整链路：
        MySQL 分片写入、CDC 同步、ES 检索与前后端联调。
      </p>
      <div class="hero-metrics">
        <div class="metric"><span>当前模式</span><strong>{{ modeLabel }}</strong></div>
        <div class="metric"><span>总记录</span><strong>{{ total }}</strong></div>
        <div class="metric"><span>当前页</span><strong>{{ pageNum }}/{{ pages }}</strong></div>
      </div>
    </header>

    <div class="content-layout">
      <div class="main-column">
        <section class="card">
          <h2>创建订单</h2>
          <div class="form-grid">
            <label>
              <span>userId</span>
              <input v-model.number="form.userId" type="number" min="0" step="1" />
            </label>
            <label>
              <span>标题</span>
              <input v-model="form.title" type="text" placeholder="例如：学习 ES" />
            </label>
            <label>
              <span>金额</span>
              <input v-model="form.amount" type="number" min="0" step="0.01" />
            </label>
          </div>
          <div class="actions">
            <button type="button" :disabled="loading" @click="submit">提交</button>
            <span class="hint">{{ shardHint }}</span>
          </div>
          <p v-if="message" class="msg">{{ message }}</p>
        </section>

        <section class="card">
          <h2>查询</h2>
          <div class="mode-switch">
            <button type="button" :class="{ active: mode === 'all' }" :disabled="loading" @click="loadAll(1)">全量分页（ES）</button>
            <button type="button" :class="{ active: mode === 'user' }" :disabled="loading" @click="loadByUser(1)">按 userId（MySQL）</button>
            <button type="button" :class="{ active: mode === 'es' }" :disabled="loading" @click="loadEs(1)">条件检索（ES）</button>
          </div>

      <div v-if="mode === 'user'" class="query-panel query-panel-user">
        <label>
          <span>userId(MySQL)</span>
          <input v-model.number="filterUserId" type="number" min="0" step="1" />
        </label>
        <label>
          <span>每页</span>
          <input v-model.number="pageSizeUser" type="number" min="1" max="100" />
        </label>
      </div>

      <div v-else-if="mode === 'es'" class="query-panel query-panel-es">
        <label>
          <span>关键词(title)</span>
          <input v-model="esFilter.keyword" type="text" placeholder="match 查询" />
        </label>
        <label>
          <span>userId(ES可选)</span>
          <input v-model.number="esFilter.userId" type="number" min="0" step="1" />
        </label>
        <label>
          <span>开始时间</span>
          <input v-model="esFilter.startTime" type="datetime-local" />
        </label>
        <label>
          <span>结束时间</span>
          <input v-model="esFilter.endTime" type="datetime-local" />
        </label>
        <label>
          <span>每页</span>
          <input v-model.number="pageSizeEs" type="number" min="1" max="100" />
        </label>
      </div>

          <div class="pager-meta">
            <span>共 {{ total }} 条</span>
            <span>每页 {{ currentPageSize }} 条</span>
            <span>第 {{ pageNum }} / {{ pages }} 页</span>
            <div class="pager-btns">
              <button type="button" :disabled="loading || pageNum <= 1" @click="prevPage">上一页</button>
              <button type="button" :disabled="loading || pageNum >= pages" @click="nextPage">下一页</button>
            </div>
          </div>

          <table>
            <thead>
              <tr>
                <th>id</th>
                <th>userId</th>
                <th>物理节点</th>
                <th>title</th>
                <th>amount</th>
                <th>createTime</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in orders" :key="o.id + '-' + o.userId">
                <td>{{ o.id }}</td>
                <td>{{ o.userId }}</td>
                <td><code>{{ physicalNode(o.userId) }}</code></td>
                <td>{{ o.title }}</td>
                <td>{{ o.amount }}</td>
                <td>{{ o.createTime }}</td>
              </tr>
            </tbody>
          </table>
          <p v-if="!orders.length" class="empty">当前条件下无数据</p>
        </section>
      </div>

      <aside class="trace-panel">
        <h2>学习透视</h2>
        <p class="trace-title">{{ traceTitle }}</p>
        <ol class="trace-list">
          <li v-for="(step, idx) in traceSteps" :key="idx">
            <span class="trace-index">{{ idx + 1 }}</span>
            <span class="trace-text">{{ step }}</span>
          </li>
        </ol>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

const USER_ID_BOUNDARY = 1000000000
const DATABASE_COUNT = 2

function databaseSuffix(userId) {
  const u = Math.trunc(Number(userId))
  return ((u % DATABASE_COUNT) + DATABASE_COUNT) % DATABASE_COUNT
}

function tableSuffix(userId) {
  return Number(userId) < USER_ID_BOUNDARY ? 0 : 1
}

function physicalNode(userId) {
  const d = databaseSuffix(userId)
  const t = tableSuffix(userId)
  return `ds_${d}.t_order_${t}`
}

function normalizePageSize(size) {
  return Math.min(100, Math.max(1, size || 10))
}

function toIso(value) {
  if (!value) return undefined
  return new Date(value).toISOString()
}

const form = reactive({
  userId: 100,
  title: 'ShardingSphere 学习订单',
  amount: 9.99
})

const esFilter = reactive({
  keyword: '',
  userId: null,
  startTime: '',
  endTime: ''
})

const filterUserId = ref(100)
const orders = ref([])
const pageNum = ref(1)
const pageSizeAll = ref(10)
const pageSizeUser = ref(10)
const pageSizeEs = ref(10)
const total = ref(0)
const pages = ref(1)
const mode = ref('all')
const loading = ref(false)
const message = ref('')
const traceTitle = ref('初始状态')
const traceSteps = ref([
  '点击任一按钮后，这里会展示本次操作经过的组件与链路。'
])

const modeLabel = computed(() => {
  if (mode.value === 'user') return '按 userId（MySQL）'
  if (mode.value === 'es') return '条件检索（ES）'
  return '全量分页（ES）'
})

const currentPageSize = computed(() => {
  if (mode.value === 'user') return normalizePageSize(pageSizeUser.value)
  if (mode.value === 'es') return normalizePageSize(pageSizeEs.value)
  return normalizePageSize(pageSizeAll.value)
})

const shardHint = computed(() => {
  const u = Number(form.userId)
  if (Number.isNaN(u)) return ''
  return `路由预估：${physicalNode(u)}`
})

watch(
  () => form.userId,
  () => {
    if (!Number.isNaN(Number(form.userId))) {
      message.value = `当前 userId 路由到 ${physicalNode(Number(form.userId))}`
    }
  },
  { immediate: true }
)

async function submit() {
  loading.value = true
  message.value = ''
  traceTitle.value = '新增订单执行链路'
  traceSteps.value = [
    '前端调用 POST /api/orders 提交 userId、title、amount。',
    `后端 OrderService.create() 生成 createTime，OrderMapper 向逻辑表 t_order 插入。`,
    `ShardingSphere 按 userId 路由到 ${physicalNode(Number(form.userId))}。`,
    'MySQL 落库后，Debezium 监听 binlog 捕获变更并发送到 Kafka(order 主题)。',
    'Kafka Connect 读取 order 主题，转换字段并按 id upsert 写入 Elasticsearch。'
  ]
  try {
    const { data } = await api.post('/orders', {
      userId: Number(form.userId),
      title: form.title,
      amount: Number(form.amount)
    })
    message.value = `创建成功，id=${data.id}`
    traceSteps.value = [
      ...traceSteps.value,
      `本次订单 id=${data.id} 已写入 MySQL；随后会通过 CDC 同步到 ES。`
    ]
    await reloadCurrentMode()
  } catch (e) {
    message.value = e.response?.data?.message || String(e)
  } finally {
    loading.value = false
  }
}

async function requestAndApply(url, params) {
  const { data } = await api.get(url, { params })
  orders.value = data.list || []
  total.value = data.total ?? 0
  pages.value = Math.max(1, data.pages ?? 1)
  pageNum.value = data.pageNum ?? pageNum.value
}

async function loadAll(num, showTrace = true) {
  mode.value = 'all'
  pageNum.value = num
  if (showTrace) {
    traceTitle.value = '全量分页（ES）执行链路'
    traceSteps.value = [
      '前端调用 GET /api/orders?pageNum&pageSize。',
      '后端 OrderService.pageAll() 转发到 OrderEsQueryService.pageAll()。',
      '通过 EsOrderRepository.findAll() 从 Elasticsearch 拉取分页数据。',
      '返回到前端展示，避免跨分片全库扫描带来的性能成本。'
    ]
  }
  loading.value = true
  try {
    await requestAndApply('/orders', {
      pageNum: pageNum.value,
      pageSize: normalizePageSize(pageSizeAll.value)
    })
  } finally {
    loading.value = false
  }
}

async function loadByUser(num, showTrace = true) {
  mode.value = 'user'
  pageNum.value = num
  if (showTrace) {
    traceTitle.value = '按 userId 查询（MySQL）执行链路'
    traceSteps.value = [
      `前端调用 GET /api/orders?userId=${filterUserId.value}&pageNum&pageSize。`,
      '后端 OrderService.pageByUser() 使用 PageHelper + OrderMapper.selectByUserId()。',
      `ShardingSphere 按 userId 精确路由到单节点 ${physicalNode(Number(filterUserId.value))}。`,
      '仅查询一个物理表后返回分页结果，不做多分片归并。'
    ]
  }
  loading.value = true
  try {
    await requestAndApply('/orders', {
      userId: filterUserId.value,
      pageNum: pageNum.value,
      pageSize: normalizePageSize(pageSizeUser.value)
    })
  } finally {
    loading.value = false
  }
}

async function loadEs(num, showTrace = true) {
  mode.value = 'es'
  pageNum.value = num
  if (showTrace) {
    traceTitle.value = '条件检索（ES）执行链路'
    traceSteps.value = [
      '前端调用 GET /api/orders/es，携带 keyword/userId/startTime/endTime 条件。',
      '后端 OrderEsQueryService.search() 组装 bool 查询与时间范围条件。',
      'Elasticsearch 执行查询并按 createTime、id 倒序分页返回。',
      '前端渲染结果，用于学习 ES 条件检索能力。'
    ]
  }
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: normalizePageSize(pageSizeEs.value)
    }
    if (esFilter.keyword?.trim()) params.keyword = esFilter.keyword.trim()
    if (esFilter.userId !== null && esFilter.userId !== '' && !Number.isNaN(Number(esFilter.userId))) {
      params.userId = Number(esFilter.userId)
    }
    const startIso = toIso(esFilter.startTime)
    const endIso = toIso(esFilter.endTime)
    if (startIso) params.startTime = startIso
    if (endIso) params.endTime = endIso
    await requestAndApply('/orders/es', params)
  } finally {
    loading.value = false
  }
}

async function reloadCurrentMode() {
  if (mode.value === 'user') return loadByUser(1, false)
  if (mode.value === 'es') return loadEs(1, false)
  return loadAll(1, false)
}

function prevPage() {
  if (pageNum.value <= 1) return
  if (mode.value === 'user') {
    loadByUser(pageNum.value - 1)
    return
  }
  if (mode.value === 'es') {
    loadEs(pageNum.value - 1)
    return
  }
  loadAll(pageNum.value - 1)
}

function nextPage() {
  if (pageNum.value >= pages.value) return
  if (mode.value === 'user') {
    loadByUser(pageNum.value + 1)
    return
  }
  if (mode.value === 'es') {
    loadEs(pageNum.value + 1)
    return
  }
  loadAll(pageNum.value + 1)
}

onMounted(() => {
  loadAll(1)
})
</script>

<style>
:root {
  font-family: 'PingFang SC', 'Segoe UI', system-ui, sans-serif;
  color: #111827;
  background: linear-gradient(180deg, #f8fbff 0%, #f1f5f9 100%);
}

body {
  margin: 0;
}

.page {
  max-width: 1320px;
  margin: 0 auto;
  padding: 24px 16px 40px;
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 16px;
  align-items: start;
  margin-top: 16px;
}

.main-column {
  min-width: 0;
}

.hero {
  background: linear-gradient(135deg, #1d4ed8 0%, #7c3aed 100%);
  color: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 14px 40px rgba(29, 78, 216, 0.28);
}

.hero h1 {
  margin: 0;
  font-size: 28px;
}

.hero p {
  margin: 10px 0 0;
  line-height: 1.6;
  opacity: 0.95;
}

.hero-metrics {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.metric {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 12px;
  padding: 10px 12px;
}

.metric span {
  display: block;
  font-size: 12px;
  opacity: 0.8;
}

.metric strong {
  display: block;
  margin-top: 6px;
  font-size: 18px;
}

.card {
  background: #fff;
  border-radius: 16px;
  padding: 18px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.main-column .card + .card {
  margin-top: 16px;
}

.card h2 {
  margin: 0 0 14px;
  font-size: 18px;
}

.form-grid,
.query-panel {
  display: grid;
  gap: 10px;
}

.query-panel-user {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.query-panel-es {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

label span {
  font-size: 12px;
  color: #475569;
}

input {
  height: 36px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  padding: 0 10px;
  font-size: 14px;
}

.actions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

button {
  height: 36px;
  border: none;
  border-radius: 10px;
  background: #2563eb;
  color: #fff;
  padding: 0 14px;
  cursor: pointer;
  font-size: 14px;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.mode-switch {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.mode-switch button {
  background: #e2e8f0;
  color: #1e293b;
}

.mode-switch button.active {
  background: #2563eb;
  color: #fff;
}

.pager-meta {
  margin: 12px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  color: #334155;
  font-size: 14px;
}

.pager-btns {
  display: flex;
  gap: 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  overflow: hidden;
  border-radius: 10px;
}

th,
td {
  border-bottom: 1px solid #e5e7eb;
  padding: 10px 8px;
  text-align: left;
}

th {
  background: #f8fafc;
  color: #334155;
}

.msg {
  margin-top: 10px;
  color: #0f766e;
}

.hint {
  color: #475569;
  font-size: 13px;
}

.empty {
  color: #6b7280;
  margin: 14px 0 0;
}

.trace-title {
  margin: 0 0 8px;
  color: #dbeafe;
}

.trace-list {
  margin: 0;
  padding: 0 0 0 24px;
  list-style: none;
  line-height: 1.7;
  position: relative;
}

.trace-list::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 6px;
  bottom: 6px;
  width: 2px;
  background: linear-gradient(180deg, rgba(56, 189, 248, 0), rgba(56, 189, 248, 0.95), rgba(56, 189, 248, 0));
  background-size: 100% 220%;
  animation: flow-line 2.8s linear infinite;
}

.trace-list li {
  position: relative;
  color: #f8fafc;
  margin-bottom: 10px;
  background: rgba(15, 23, 42, 0.32);
  border: 1px solid rgba(148, 163, 184, 0.32);
  border-radius: 12px;
  padding: 10px 12px;
  backdrop-filter: blur(6px);
  display: flex;
  align-items: flex-start;
  gap: 10px;
  transition: transform 220ms ease, border-color 220ms ease, background-color 220ms ease;
}

.trace-list li::before {
  content: '';
  position: absolute;
  left: -20px;
  top: 12px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #38bdf8;
  box-shadow: 0 0 0 0 rgba(56, 189, 248, 0.8);
  animation: pulse-node 2.4s ease-out infinite;
}

.trace-list li:hover {
  transform: translateX(2px);
  border-color: rgba(56, 189, 248, 0.6);
  background: rgba(15, 23, 42, 0.5);
}

.trace-index {
  flex: 0 0 auto;
  min-width: 20px;
  height: 20px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #082f49;
  background: linear-gradient(135deg, #7dd3fc 0%, #38bdf8 100%);
}

.trace-text {
  display: block;
  font-size: 13px;
  line-height: 1.7;
}

.trace-panel {
  position: sticky;
  top: 16px;
  border-radius: 16px;
  padding: 18px;
  background: linear-gradient(160deg, rgba(15, 23, 42, 0.5), rgba(30, 41, 59, 0.5));
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.24);
  overflow: hidden;
}

.trace-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(120deg, rgba(56, 189, 248, 0) 10%, rgba(56, 189, 248, 0.12) 48%, rgba(56, 189, 248, 0) 86%);
  transform: translateX(-120%);
  animation: panel-sheen 4.2s ease-in-out infinite;
}

.trace-panel h2 {
  margin: 0 0 10px;
  color: #e2e8f0;
}

@keyframes flow-line {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 0 220%;
  }
}

@keyframes pulse-node {
  0% {
    box-shadow: 0 0 0 0 rgba(56, 189, 248, 0.8);
  }
  80% {
    box-shadow: 0 0 0 12px rgba(56, 189, 248, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(56, 189, 248, 0);
  }
}

@keyframes panel-sheen {
  0% {
    transform: translateX(-120%);
  }
  55% {
    transform: translateX(120%);
  }
  100% {
    transform: translateX(120%);
  }
}

code {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 6px;
}

@media (max-width: 960px) {
  .content-layout {
    grid-template-columns: 1fr;
  }

  .trace-panel {
    position: static;
  }

  .hero-metrics,
  .form-grid,
  .query-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hero-metrics,
  .form-grid,
  .query-panel {
    grid-template-columns: 1fr;
  }

  .mode-switch,
  .pager-meta {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
