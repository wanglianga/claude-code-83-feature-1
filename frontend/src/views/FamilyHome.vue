<template>
  <div>
    <el-card shadow="never">
      <template #header>家人理发服务单</template>
      <el-table :data="orders" size="small" v-loading="loading" empty-text="暂无服务单">
        <el-table-column prop="orderNo" label="单号" width="150" />
        <el-table-column prop="elderName" label="老人" width="100" />
        <el-table-column prop="scheduledDate" label="日期" width="110" />
        <el-table-column prop="timeSlot" label="时段" width="110" />
        <el-table-column prop="barberName" label="理发师" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="ORDER_STATUS_TYPES[row.status]">{{ ORDER_STATUS_LABELS[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="费用(补/自)" width="110">
          <template #default="{ row }">¥{{ row.subsidyAmount }} / ¥{{ row.selfPayAmount }}</template>
        </el-table-column>
        <el-table-column label="满意度" width="90">
          <template #default="{ row }">
            <span v-if="row.satisfactionRating">{{ row.satisfactionRating }} 星</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/orders/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-alert style="margin-top: 12px" type="info" :closable="false"
        title="服务完成后可在详情页进行满意度评价；如需改约或上报异常，也可在详情页操作。" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listOrders } from '../api'
import { ORDER_STATUS_LABELS, ORDER_STATUS_TYPES } from '../utils/labels'

const orders = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    orders.value = await listOrders({})
  } finally {
    loading.value = false
  }
})
</script>
