<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-radio-group v-model="statusFilter" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="OPEN">待处理</el-radio-button>
        <el-radio-button value="PROCESSING">处理中</el-radio-button>
        <el-radio-button value="RESOLVED">已解决</el-radio-button>
      </el-radio-group>
      <el-checkbox v-if="['FINANCE', 'ADMIN', 'STAFF'].includes(role)" v-model="financeOnly" @change="load">
        仅看需财务介入
      </el-checkbox>
    </div>
    <el-table :data="list" size="small" v-loading="loading">
      <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ EXCEPTION_TYPE_LABELS[row.type] }}</template>
      </el-table-column>
      <el-table-column prop="orderId" label="服务单" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/orders/${row.orderId}`)">#{{ row.orderId }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="reportedByName" label="上报人" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="EXCEPTION_STATUS_TYPES[row.status]">{{ EXCEPTION_STATUS_LABELS[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="财务介入" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.financeInvolved" type="warning" size="small">是</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="上报时间" width="160" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="canHandle">
            <el-button v-if="row.status === 'OPEN'" link type="warning" @click="doHandle(row)">接手处理</el-button>
            <el-button v-if="row.status !== 'RESOLVED'" link type="success" @click="openResolve(row)">解决</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="resolveDialog" title="解决异常" width="480px">
      <el-form label-width="110px">
        <el-form-item label="处理结果">
          <el-input v-model="resolveForm.resolution" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item v-if="current?.type === 'SUBSIDY_CHANGE'" label="新补贴资格">
          <el-select v-model="resolveForm.newSubsidyType" style="width: 100%">
            <el-option v-for="(v, k) in SUBSIDY_LABELS" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="current?.type === 'REFUSE_PAYMENT'" label="费用已补缴">
          <el-switch v-model="resolveForm.paymentPaid" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialog = false">取消</el-button>
        <el-button type="success" @click="doResolve">确认解决</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listExceptions, handleException, resolveException } from '../api'
import { useUserStore } from '../store/user'
import {
  EXCEPTION_TYPE_LABELS, EXCEPTION_STATUS_LABELS, EXCEPTION_STATUS_TYPES, SUBSIDY_LABELS
} from '../utils/labels'

const store = useUserStore()
const role = computed(() => store.role)
const list = ref([])
const loading = ref(false)
const statusFilter = ref('')
const financeOnly = ref(false)
const resolveDialog = ref(false)
const current = ref(null)
const resolveForm = reactive({ resolution: '', newSubsidyType: 'PARTIAL', paymentPaid: false })

const canHandle = computed(() => ['ADMIN', 'STAFF', 'FINANCE'].includes(store.role))

const load = async () => {
  loading.value = true
  try {
    const params = {}
    if (statusFilter.value) params.status = statusFilter.value
    if (financeOnly.value) params.financeOnly = true
    list.value = await listExceptions(params)
  } finally {
    loading.value = false
  }
}

const doHandle = async (row) => {
  await handleException(row.id)
  ElMessage.success('已接手处理')
  load()
}

const openResolve = (row) => {
  current.value = row
  resolveForm.resolution = ''
  resolveDialog.value = true
}

const doResolve = async () => {
  if (!resolveForm.resolution) {
    ElMessage.warning('请填写处理结果')
    return
  }
  await resolveException(current.value.id, resolveForm)
  ElMessage.success('异常已解决')
  resolveDialog.value = false
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
