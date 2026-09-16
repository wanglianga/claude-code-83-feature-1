<template>
  <el-card shadow="never">
    <template #header>我的陪同任务（陪同进门、核对老人状态、现场照片、家属授权）</template>
    <el-table :data="orders" size="small" v-loading="loading" empty-text="暂无陪同任务">
      <el-table-column prop="orderNo" label="单号" width="140" />
      <el-table-column prop="elderName" label="老人" width="90" />
      <el-table-column prop="scheduledDate" label="日期" width="105" />
      <el-table-column prop="timeSlot" label="时段" width="105" />
      <el-table-column prop="address" label="地址" min-width="170" show-overflow-tooltip />
      <el-table-column label="需家属在场" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.needFamilyPresent" type="warning" size="small">需要</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="ORDER_STATUS_TYPES[row.status]">{{ ORDER_STATUS_LABELS[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/orders/${row.id}`)">
            {{ ['ASSIGNED', 'TOOL_CONFIRMED'].includes(row.status) ? '进门核对' : '查看' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
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
    orders.value = (await listOrders({})).filter((o) => !['COMPLETED', 'CANCELLED'].includes(o.status))
  } finally {
    loading.value = false
  }
})
</script>
