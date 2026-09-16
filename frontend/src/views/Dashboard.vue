<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px" v-if="isStaffLike">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>楼栋服务覆盖</template>
          <div ref="buildingChart" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>老人风险分布与已服务</template>
          <div ref="riskChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 角色快捷入口 -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>待办与快捷入口</template>
      <el-space wrap>
        <el-button v-if="role === 'BARBER'" type="primary" @click="$router.push('/barber')">今日上门任务</el-button>
        <el-button v-if="role === 'VOLUNTEER'" type="primary" @click="$router.push('/volunteer')">今日陪同任务</el-button>
        <el-button v-if="role === 'FAMILY'" type="primary" @click="$router.push('/family')">家人服务单</el-button>
        <el-button v-if="['ADMIN', 'STAFF'].includes(role)" type="primary" @click="$router.push('/orders/create')">新建预约</el-button>
        <el-button v-if="['ADMIN', 'STAFF'].includes(role)" @click="$router.push('/elders')">老人档案</el-button>
        <el-button v-if="['ADMIN', 'STAFF', 'FINANCE', 'BARBER', 'VOLUNTEER'].includes(role)" @click="$router.push('/exceptions')">
          异常处理<el-badge v-if="stats.openExceptions" :value="stats.openExceptions" type="danger" style="margin-left: 4px" />
        </el-button>
        <el-button v-if="['FINANCE', 'ADMIN', 'STAFF'].includes(role)" @click="$router.push('/finance')">
          补贴审核<el-badge v-if="stats.pendingSubsidy" :value="stats.pendingSubsidy" type="warning" style="margin-left: 4px" />
        </el-button>
        <el-button v-if="['ADMIN', 'STAFF', 'GRID', 'VOLUNTEER'].includes(role)" @click="$router.push('/follow-ups')">
          回访管理<el-badge v-if="stats.pendingFollowUps" :value="stats.pendingFollowUps" type="warning" style="margin-left: 4px" />
        </el-button>
        <el-button v-if="['ADMIN', 'STAFF', 'GRID'].includes(role)" @click="$router.push('/care-tasks')">
          关怀任务<el-badge v-if="stats.pendingCareTasks" :value="stats.pendingCareTasks" type="danger" style="margin-left: 4px" />
        </el-button>
        <el-button v-if="['ADMIN', 'STAFF', 'FINANCE', 'GRID'].includes(role)" @click="$router.push('/stats')">复盘统计</el-button>
      </el-space>
    </el-card>

    <!-- 今日服务单 -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>今日服务单</template>
      <el-table :data="todayOrders" size="small" empty-text="今日暂无服务单">
        <el-table-column prop="orderNo" label="单号" width="150" />
        <el-table-column prop="elderName" label="老人" width="100" />
        <el-table-column prop="timeSlot" label="时段" width="110" />
        <el-table-column prop="barberName" label="理发师" width="100" />
        <el-table-column prop="volunteerName" label="志愿者" width="100">
          <template #default="{ row }">{{ row.volunteerName || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="ORDER_STATUS_TYPES[row.status]">{{ ORDER_STATUS_LABELS[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="异常" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.hasException" type="danger" size="small">有</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/orders/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { statsDashboard, statsCoverage, listOrders } from '../api'
import { useUserStore } from '../store/user'
import { ORDER_STATUS_LABELS, ORDER_STATUS_TYPES, RISK_LABELS } from '../utils/labels'

const store = useUserStore()
const role = computed(() => store.role)
const isStaffLike = computed(() => ['ADMIN', 'STAFF', 'GRID'].includes(store.role))

const stats = ref({})
const todayOrders = ref([])
const buildingChart = ref(null)
const riskChart = ref(null)

const cards = computed(() => [
  { label: '在册老人', value: stats.value.elderCount ?? '-', color: '#409eff' },
  { label: '累计服务单', value: stats.value.orderTotal ?? '-', color: '#67c23a' },
  { label: '已完成服务', value: stats.value.orderCompleted ?? '-', color: '#67c23a' },
  { label: '公益补贴总额(元)', value: stats.value.subsidyTotal ?? '-', color: '#e6a23c' }
])

onMounted(async () => {
  stats.value = await statsDashboard()
  todayOrders.value = await listOrders({ date: dayjs().format('YYYY-MM-DD') })
  if (isStaffLike.value) {
    const cov = await statsCoverage()
    await nextTick()
    renderBuilding(cov.byBuilding)
    renderRisk(cov.byRisk)
  }
})

const renderBuilding = (data) => {
  const chart = echarts.init(buildingChart.value)
  chart.setOption({
    tooltip: {},
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: data.map((d) => d.building) },
    yAxis: { type: 'value' },
    series: [
      { name: '在册老人', type: 'bar', data: data.map((d) => d.elderCount), itemStyle: { color: '#409eff' } },
      { name: '已服务老人', type: 'bar', data: data.map((d) => d.servedCount), itemStyle: { color: '#67c23a' } }
    ]
  })
}

const renderRisk = (data) => {
  const chart = echarts.init(riskChart.value)
  chart.setOption({
    tooltip: {},
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: data.map((d) => RISK_LABELS[d.riskLevel]) },
    yAxis: { type: 'value' },
    series: [
      { name: '老人数', type: 'bar', data: data.map((d) => d.elderCount), itemStyle: { color: '#e6a23c' } },
      { name: '已服务', type: 'bar', data: data.map((d) => d.servedCount), itemStyle: { color: '#67c23a' } },
      { name: '服务次数', type: 'bar', data: data.map((d) => d.orderCount), itemStyle: { color: '#409eff' } }
    ]
  })
}
</script>

<style scoped>
.stat-card {
  text-align: center;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
}
.stat-label {
  color: #909399;
  margin-top: 4px;
}
</style>
