<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="PENDING">待确认</el-radio-button>
          <el-radio-button value="CONFIRMED">已确认</el-radio-button>
        </el-radio-group>
        <el-button v-if="['ADMIN', 'STAFF'].includes(role)" type="primary" :loading="scanning" @click="doScan">
          立即扫描（长期未预约 / 连续取消）
        </el-button>
      </div>
      <el-table :data="list" size="small" v-loading="loading">
        <el-table-column prop="elderName" label="老人" width="100" />
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.source === 'REPEATED_CANCEL' ? 'danger' : 'warning'">
              {{ CARE_SOURCE_LABELS[row.source] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="notes" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'PENDING' ? 'warning' : 'success'">
              {{ CARE_STATUS_LABELS[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="关怀确认结果" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.result || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="primary" @click="openConfirm(row)">关怀确认</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="关怀确认" width="480px">
      <el-form label-width="110px">
        <el-form-item label="确认结果">
          <el-input v-model="form.result" type="textarea" :rows="3"
            placeholder="上门/电话关怀情况：老人身体、精神状态、未预约或取消原因等" />
        </el-form-item>
        <el-form-item label="需要额外关怀">
          <el-switch v-model="form.needExtraCare" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doConfirm">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listCareTasks, scanCareTasks, confirmCareTask } from '../api'
import { useUserStore } from '../store/user'
import { CARE_SOURCE_LABELS, CARE_STATUS_LABELS } from '../utils/labels'

const store = useUserStore()
const role = computed(() => store.role)
const list = ref([])
const loading = ref(false)
const scanning = ref(false)
const statusFilter = ref('')
const dialogVisible = ref(false)
const current = ref(null)
const form = reactive({ result: '', needExtraCare: false })

const load = async () => {
  loading.value = true
  try {
    const params = {}
    if (statusFilter.value) params.status = statusFilter.value
    list.value = await listCareTasks(params)
  } finally {
    loading.value = false
  }
}

const doScan = async () => {
  scanning.value = true
  try {
    const count = await scanCareTasks()
    ElMessage.success(`扫描完成，新增 ${count} 条关怀任务`)
    load()
  } finally {
    scanning.value = false
  }
}

const openConfirm = (row) => {
  current.value = row
  form.result = ''
  form.needExtraCare = false
  dialogVisible.value = true
}

const doConfirm = async () => {
  if (!form.result) {
    ElMessage.warning('请填写确认结果')
    return
  }
  await confirmCareTask(current.value.id, form)
  ElMessage.success('关怀确认完成')
  dialogVisible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}
</style>
