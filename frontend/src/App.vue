<template>
  <div class="page">
    <header>
      <h1>ShardingSphere-JDBC 单库分表示例</h1>
      <p class="sub">
        逻辑表 <code>t_order</code> → 物理表 <code>t_order_0</code> / <code>t_order_1</code>，
        分片键 <code>user_id</code>，规则：<code>BOUNDARY_RANGE</code>（边界
        <code>1_000_000_000</code>：小于 → <code>t_order_0</code>，大于等于 → <code>t_order_1</code>）。控制台可看到 SQL
        改写（<code>sql-show: true</code>）。
      </p>
    </header>

    <section class="card">
      <h2>新建订单</h2>
      <div class="row">
        <label>userId（分片键）</label>
        <input v-model.number="form.userId" type="number" min="0" step="1" />
        <span class="hint">{{ shardHint }}</span>
      </div>
      <div class="row">
        <label>标题</label>
        <input v-model="form.title" type="text" placeholder="例如：学习分表" />
      </div>
      <div class="row">
        <label>金额</label>
        <input v-model="form.amount" type="number" min="0" step="0.01" />
      </div>
      <button type="button" :disabled="loading" @click="submit">提交</button>
      <p v-if="message" class="msg">{{ message }}</p>
    </section>

    <section class="card">
      <h2>订单列表</h2>
      <div class="toolbar">
        <button type="button" :disabled="loading" @click="() => loadPage(1)">全表（跨分片分页）</button>
        <label>
          仅 userId
          <input v-model.number="filterUserId" type="number" min="0" step="1" style="width: 6rem" />
        </label>
        <button type="button" :disabled="loading" @click="() => loadPage(1, true)">按用户查询</button>
        <label>
          每页
          <input v-model.number="pageSize" type="number" min="1" max="100" style="width: 4rem" />
        </label>
      </div>
      <p class="pager-meta">
        第 {{ pageNum }} / {{ pages }} 页，共 {{ total }} 条
        <span class="pager-btns">
          <button type="button" :disabled="loading || pageNum <= 1" @click="prevPage">上一页</button>
          <button type="button" :disabled="loading || pageNum >= pages" @click="nextPage">下一页</button>
        </span>
      </p>
      <table>
        <thead>
        <tr>
          <th>id</th>
          <th>userId</th>
          <th>物理表（推导）</th>
          <th>title</th>
          <th>amount</th>
          <th>createTime</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="o in orders" :key="o.id + '-' + o.userId">
          <td>{{ o.id }}</td>
          <td>{{ o.userId }}</td>
          <td><code>t_order_{{ shardSuffix(o.userId) }}</code></td>
          <td>{{ o.title }}</td>
          <td>{{ o.amount }}</td>
          <td>{{ o.createTime }}</td>
        </tr>
        </tbody>
      </table>
      <p v-if="!orders.length" class="empty">暂无数据：请创建库并执行 backend/src/main/resources/sql/init.sql 建表后，再创建订单。</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

/** 与 backend OrderShardingConstants / sharding.yaml 一致 */
const USER_ID_BOUNDARY = 1000000000

function shardSuffix(userId) {
  return Number(userId) < USER_ID_BOUNDARY ? 0 : 1
}

const form = reactive({
  userId: 100,
  title: 'ShardingSphere 学习订单',
  amount: 9.99
})

const filterUserId = ref(100)
const orders = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const pages = ref(1)
const onlyUser = ref(false)
const loading = ref(false)
const message = ref('')

const shardHint = computed(() => {
  const u = Number(form.userId)
  if (Number.isNaN(u)) return ''
  return `→ 物理表 t_order_${shardSuffix(u)}`
})

async function refreshShardPreview() {
  try {
    const u = Number(form.userId)
    if (Number.isNaN(u)) return
    const { data } = await api.get('/orders/preview-shard', { params: { userId: u } })
    message.value = `预览：userId=${data.userId} → ${data.physicalTable}`
  } catch {
    /* 忽略预览失败 */
  }
}

watch(
    () => form.userId,
    () => {
      refreshShardPreview()
    }
)

async function submit() {
  loading.value = true
  message.value = ''
  try {
    const { data } = await api.post('/orders', {
      userId: Number(form.userId),
      title: form.title,
      amount: Number(form.amount)
    })
    message.value = `创建成功，id=${data.id}，请查看后端日志中的实际 SQL（表名应为 t_order_${shardSuffix(Number(form.userId))}）`
    await loadPage(1, onlyUser.value)
  } catch (e) {
    message.value = e.response?.data?.message || String(e)
  } finally {
    loading.value = false
  }
}

async function loadPage(num, byUser = false) {
  onlyUser.value = byUser
  pageNum.value = num
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: Math.min(100, Math.max(1, pageSize.value || 10))
    }
    if (byUser) {
      params.userId = filterUserId.value
    }
    const { data } = await api.get('/orders', { params })
    orders.value = data.list || []
    total.value = data.total ?? 0
    pages.value = Math.max(1, data.pages ?? 1)
    pageNum.value = data.pageNum ?? pageNum.value
  } finally {
    loading.value = false
  }
}

function prevPage() {
  if (pageNum.value > 1) loadPage(pageNum.value - 1, onlyUser.value)
}

function nextPage() {
  if (pageNum.value < pages.value) loadPage(pageNum.value + 1, onlyUser.value)
}

onMounted(() => {
  loadPage(1, false)
  refreshShardPreview()
})
</script>

<style>
:root {
  font-family: 'SF Pro Text', 'Segoe UI', system-ui, sans-serif;
  color: #1a1a1a;
  background: #f4f6f8;
}
.page {
  max-width: 960px;
  margin: 0 auto;
  padding: 2rem 1.25rem 3rem;
}
header h1 {
  font-size: 1.5rem;
  margin: 0 0 0.5rem;
}
.sub {
  margin: 0;
  color: #444;
  line-height: 1.55;
  font-size: 0.95rem;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 1.25rem 1.5rem;
  margin-top: 1.25rem;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}
.card h2 {
  margin: 0 0 1rem;
  font-size: 1.1rem;
}
.row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.65rem;
  flex-wrap: wrap;
}
.row label {
  width: 8rem;
  font-size: 0.9rem;
  color: #333;
}
.row input[type='text'],
.row input[type='number'] {
  flex: 1;
  min-width: 12rem;
  padding: 0.45rem 0.6rem;
  border: 1px solid #cfd6de;
  border-radius: 8px;
}
.hint {
  font-size: 0.85rem;
  color: #0b6b3a;
}
button {
  padding: 0.45rem 1rem;
  border: none;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
  font-size: 0.9rem;
}
button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  margin-bottom: 0.75rem;
}
.pager-meta {
  margin: 0 0 0.75rem;
  font-size: 0.9rem;
  color: #374151;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}
.pager-btns {
  display: inline-flex;
  gap: 0.5rem;
}
.pager-btns button {
  padding: 0.25rem 0.65rem;
  font-size: 0.85rem;
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}
th,
td {
  border: 1px solid #e5e7eb;
  padding: 0.45rem 0.5rem;
  text-align: left;
}
th {
  background: #f8fafc;
}
.msg {
  margin-top: 0.75rem;
  color: #0f766e;
  font-size: 0.9rem;
}
.empty {
  color: #6b7280;
  font-size: 0.9rem;
}
code {
  background: #f1f5f9;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  font-size: 0.86em;
}
</style>
