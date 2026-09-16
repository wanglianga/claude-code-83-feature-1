<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat">
          <div class="num" style="color: #e6a23c">¥{{ funds.pendingTotal ?? 0 }}</div>
          <div class="label">待审核补贴（{{ funds.pendingCount ?? 0 }} 笔）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat">
          <div class="num" style="color: #67c23a">¥{{ funds.approvedTotal ?? 0 }}</div>
          <div class="label">已发放补贴（{{ funds.approvedCount ?? 0 }} 笔）</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>公益资金月度发放</template>
          <div ref="chart" style="height: 120px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="loadList">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="PENDING">待审核</el-radio-button>
          <el-radio-button value="APPROVED">已发放</el-radio-button>
          <el-radio-button value="REJECTED">已驳回</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="list" size="small" v-loading="loading">
        <el-table-column prop="id" label="编号" width="70" />
        <el-table-column prop="elderName" label="老人" width="100" />
        <el-table-column label="服务单" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/orders/${row.orderId}`)">#{{ row.orderId }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="补贴金额" width="100">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="SUBSIDY_STATUS_TYPES[row.status]">{{ SUBSIDY_STATUS_LABELS[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewedBy" label="审核人" width="90">
          <template #default="{ row }">{{ row.reviewedBy || '—' }}</template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="140" fixed="right" v-if="isFinance">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" @click="review(row, true)">发放</el-button>
              <el-button link type="danger" @click="review(row, false)">驳回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSubsidies, reviewSubsidy, statsFunds } from '../api'
import { useUserStore } from '../store/user'
import { SUBSIDY_STATUS_LABELS, SUBSIDY_STATUS_TYPES } from '../utils/labels'

const store = useUserStore()
const isFinance = computed(() => ['FINANCE', 'ADMIN'].includes(store.role))
const list = ref([])
const funds = ref({})
const loading = ref(false)
const statusFilter = ref('')
const chart = ref(null)

const loadList = async () => {
  loading.value = true
  try {
    const params = {}
    if (statusFilter.value) params.status = statusFilter.value
    list.value = await listSubsidies(params)
  } finally {
    loading.value = false
  }
}

const review = async (row, approve) => {
  const { value } = await ElMessageBox.prompt(
    approve ? '确认发放该笔补贴？' : '请输入驳回原因',
    approve ? '发放补贴' : '驳回补贴',
    { inputPlaceholder: '备注', inputValue: approve ? '资格核验通过' : '' }
  )
  await reviewSubsidy(row.id, { approve, note: value || '' })
  ElMessage.success('已处理')
  loadAll()
}

const loadAll = async () => {
  await loadList()
  funds.value = await statsFunds()
  await nextTick()
  const months = Object.keys(funds.value.byMonth || {})
  const c = echarts.init(chart.value)
  c.setOption({
    grid: { left: 50, right: 20, top: 20, bottom: 25 },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: months.map((m) => funds.value.byMonth[m]), itemStyle: { color: '#e6a23c' } }]
  })
}

onMounted(loadAll)
</script>

<style scoped>
.stat {
  text-align: center;
}
.num {
  font-size: 26px;
  font-weight: 700;
}
.label {
  color: #909399;
  margin-top: 4px;
}
.toolbar {
  margin-bottom: 12px;
}
</style>
