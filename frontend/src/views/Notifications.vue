<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button @click="doReadAll">全部已读</el-button>
    </div>
    <el-table :data="list" size="small" v-loading="loading" empty-text="暂无通知">
      <el-table-column label="状态" width="70">
        <template #default="{ row }">
          <el-badge v-if="!row.readFlag" is-dot><el-icon><Bell /></el-icon></el-badge>
          <el-icon v-else color="#c0c4cc"><Bell /></el-icon>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" width="220" />
      <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="时间" width="170" />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button v-if="!row.readFlag" link type="primary" @click="doRead(row)">已读</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listNotifications, markRead, readAll } from '../api'

const list = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    list.value = await listNotifications()
  } finally {
    loading.value = false
  }
}

const doRead = async (row) => {
  await markRead(row.id)
  load()
}

const doReadAll = async () => {
  await readAll()
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>
