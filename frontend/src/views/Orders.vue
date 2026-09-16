<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-space wrap>
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 140px" @change="load">
          <el-option v-for="(v, k) in ORDER_STATUS_LABELS" :key="k" :label="v" :value="k" />
        </el-select>
        <el-date-picker v-model="filters.date" type="date" value-format="YYYY-MM-DD" placeholder="预约日期"
          style="width: 160px" @change="load" />
        <el-button @click="reset">重置</el-button>
      </el-space>
    </div>
    <el-table :data="list" v-loading="loading" size="small">
      <el-table-column prop="orderNo" label="单号" width="150" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ ORDER_TYPE_LABELS[row.type] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="elderName" label="老人" width="90" />
      <el-table-column prop="scheduledDate" label="日期" width="105" />
      <el-table-column prop="timeSlot" label="时段" width="105" />
      <el-table-column prop="barberName" label="理发师" width="90" />
      <el-table-column label="志愿者" width="90">
        <template #default="{ row }">{{ row.volunteerName || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="105">
        <template #default="{ row }">
          <el-tag size="small" :type="ORDER_STATUS_TYPES[row.status]">{{ ORDER_STATUS_LABELS[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="费用(补/自)" width="110">
        <template #default="{ row }">¥{{ row.subsidyAmount }} / ¥{{ row.selfPayAmount }}</template>
      </el-table-column>
      <el-table-column label="支付" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="PAYMENT_TYPES[row.paymentStatus]">{{ PAYMENT_LABELS[row.paymentStatus] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="异常" width="60">
        <template #default="{ row }">
          <el-tag v-if="row.hasException" type="danger" size="small">有</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="满意度" width="80">
        <template #default="{ row }">
          <span v-if="row.satisfactionRating">{{ row.satisfactionRating }} 星</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/orders/${row.id}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listOrders } from '../api'
import {
  ORDER_STATUS_LABELS, ORDER_STATUS_TYPES, ORDER_TYPE_LABELS, PAYMENT_LABELS, PAYMENT_TYPES
} from '../utils/labels'

const list = ref([])
const loading = ref(false)
const filters = ref({ status: '', date: '' })

const load = async () => {
  loading.value = true
  try {
    const params = {}
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.date) params.date = filters.value.date
    list.value = await listOrders(params)
  } finally {
    loading.value = false
  }
}

const reset = () => {
  filters.value = { status: '', date: '' }
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>
