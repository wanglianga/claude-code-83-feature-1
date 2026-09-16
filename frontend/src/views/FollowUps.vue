<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-radio-group v-model="statusFilter" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING">待回访</el-radio-button>
        <el-radio-button value="DONE">已回访</el-radio-button>
      </el-radio-group>
    </div>
    <el-table :data="list" size="small" v-loading="loading">
      <el-table-column prop="elderName" label="老人" width="100" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 'EXCEPTION' ? 'danger' : row.type === 'CARE' ? 'warning' : 'primary'">
            {{ FOLLOWUP_TYPE_LABELS[row.type] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="回访内容" min-width="240" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'DONE' ? 'success' : 'warning'">
            {{ FOLLOWUP_STATUS_LABELS[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="result" label="回访结果" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.result || '—' }}</template>
      </el-table-column>
      <el-table-column label="额外关怀" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.needExtraCare" type="danger" size="small">需要</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="primary" @click="openComplete(row)">登记回访</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="登记回访结果" width="480px">
      <el-form label-width="100px">
        <el-form-item label="回访结果">
          <el-input v-model="form.result" type="textarea" :rows="3"
            placeholder="老人精神状态、服务反馈、是否需要额外关怀等" />
        </el-form-item>
        <el-form-item label="需要额外关怀">
          <el-switch v-model="form.needExtraCare" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doComplete">提交</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listFollowUps, completeFollowUp } from '../api'
import { FOLLOWUP_TYPE_LABELS, FOLLOWUP_STATUS_LABELS } from '../utils/labels'

const list = ref([])
const loading = ref(false)
const statusFilter = ref('')
const dialogVisible = ref(false)
const current = ref(null)
const form = reactive({ result: '', needExtraCare: false })

const load = async () => {
  loading.value = true
  try {
    const params = {}
    if (statusFilter.value) params.status = statusFilter.value
    list.value = await listFollowUps(params)
  } finally {
    loading.value = false
  }
}

const openComplete = (row) => {
  current.value = row
  form.result = ''
  form.needExtraCare = false
  dialogVisible.value = true
}

const doComplete = async () => {
  if (!form.result) {
    ElMessage.warning('请填写回访结果')
    return
  }
  await completeFollowUp(current.value.id, form)
  ElMessage.success('回访已登记')
  dialogVisible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>
