<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>我的服务任务</template>
          <el-table :data="orders" size="small" v-loading="loading">
            <el-table-column prop="orderNo" label="单号" width="140" />
            <el-table-column prop="elderName" label="老人" width="90" />
            <el-table-column prop="scheduledDate" label="日期" width="105" />
            <el-table-column prop="timeSlot" label="时段" width="105" />
            <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="ORDER_STATUS_TYPES[row.status]">{{ ORDER_STATUS_LABELS[row.status] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="$router.push(`/orders/${row.id}`)">去处理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 排班 -->
        <el-card shadow="never" style="margin-top: 16px">
          <template #header>
            <div class="card-head">
              <span>我的排班（未来 7 天）</span>
              <el-button size="small" type="primary" @click="scheduleDialog = true">添加排班</el-button>
            </div>
          </template>
          <el-table :data="schedules" size="small" empty-text="暂无排班">
            <el-table-column prop="workDate" label="日期" width="120" />
            <el-table-column label="时段">
              <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
            </el-table-column>
            <el-table-column prop="maxOrders" label="最大单量" width="90" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-popconfirm title="删除该排班？" @confirm="delSchedule(row.id)">
                  <template #reference><el-button link type="danger" size="small">删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <!-- 工具包 -->
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span>我的工具包</span>
              <el-space :size="4">
                <el-tag v-if="toolkit" :type="DISINFECTION_TYPES[effectiveStatus]" size="small">
                  {{ DISINFECTION_LABELS[effectiveStatus] }}
                </el-tag>
                <el-button link type="primary" size="small" @click="$router.push('/tool-disinfection')">消毒/备用包</el-button>
              </el-space>
            </div>
          </template>
          <template v-if="toolkit">
            <p><b>{{ toolkit.name }}</b></p>
            <p class="muted">封签编号：{{ toolkit.sealCode || '未加封' }}</p>
            <p class="muted">上次消毒：{{ toolkit.disinfectedAt || '从未消毒' }}</p>
            <p class="muted">消毒方式：{{ DISINFECTION_METHOD_LABELS[toolkit.disinfectionMethod] || '未登记' }}；消毒柜 {{ toolkit.cabinetNo || '—' }}；责任人 {{ toolkit.responsiblePerson || '—' }}</p>
            <p class="muted">消毒 48 小时内有效，过期/封签破损将无法派单且不可开始服务</p>
            <el-button type="primary" size="small" style="margin-top: 8px" @click="doDisinfect">完成消毒</el-button>
          </template>
          <el-empty v-else description="暂无工具包" :image-size="40" />
        </el-card>

        <!-- 信用 -->
        <el-card shadow="never" style="margin-top: 16px" v-if="profile">
          <template #header>我的信用</template>
          <el-progress type="dashboard" :percentage="profile.creditScore" :color="creditColor" />
          <el-descriptions :column="2" size="small" style="margin-top: 8px">
            <el-descriptions-item label="完成">{{ profile.completedCount }}</el-descriptions-item>
            <el-descriptions-item label="取消">{{ profile.cancelCount }}</el-descriptions-item>
            <el-descriptions-item label="迟到">{{ profile.lateCount }}</el-descriptions-item>
            <el-descriptions-item label="事故">{{ profile.incidentCount }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="scheduleDialog" title="添加排班" width="420px">
      <el-form label-width="90px">
        <el-form-item label="日期">
          <el-date-picker v-model="scheduleForm.workDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-select v-model="scheduleForm.startTime" start="06:00" step="00:30" end="20:00" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-select v-model="scheduleForm.endTime" start="06:00" step="00:30" end="21:00" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最大单量">
          <el-input-number v-model="scheduleForm.maxOrders" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialog = false">取消</el-button>
        <el-button type="primary" @click="addMySchedule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import {
  listOrders, listBarbers, listSchedules, addSchedule, deleteSchedule, getToolKit, disinfectToolKit
} from '../api'
import { useUserStore } from '../store/user'
import { ORDER_STATUS_LABELS, ORDER_STATUS_TYPES, DISINFECTION_LABELS, DISINFECTION_TYPES, DISINFECTION_METHOD_LABELS } from '../utils/labels'

const store = useUserStore()
const orders = ref([])
const schedules = ref([])
const toolkit = ref(null)
const profile = ref(null)
const loading = ref(false)
const scheduleDialog = ref(false)
const scheduleForm = reactive({
  workDate: dayjs().add(1, 'day').format('YYYY-MM-DD'),
  startTime: '08:00',
  endTime: '12:00',
  maxOrders: 4
})

const uid = store.user?.id

const effectiveStatus = computed(() => {
  if (!toolkit.value) return 'PENDING'
  if (toolkit.value.status === 'DISINFECTED' && toolkit.value.disinfectedAt) {
    const expired = dayjs(toolkit.value.disinfectedAt).add(48, 'hour').isBefore(dayjs())
    return expired ? 'EXPIRED' : 'DISINFECTED'
  }
  return toolkit.value.status
})

const creditColor = (score) => (score >= 90 ? '#67c23a' : score >= 70 ? '#e6a23c' : '#f56c6c')

const load = async () => {
  loading.value = true
  try {
    orders.value = (await listOrders({})).filter((o) => !['COMPLETED', 'CANCELLED'].includes(o.status))
    schedules.value = await listSchedules({
      barberId: uid,
      from: dayjs().format('YYYY-MM-DD'),
      to: dayjs().add(7, 'day').format('YYYY-MM-DD')
    })
    toolkit.value = await getToolKit(uid)
    const barbers = await listBarbers()
    profile.value = barbers.find((b) => b.profile.userId === uid)?.profile
  } finally {
    loading.value = false
  }
}

const addMySchedule = async () => {
  await addSchedule({ ...scheduleForm, barberId: uid })
  ElMessage.success('排班已添加')
  scheduleDialog.value = false
  load()
}

const delSchedule = async (id) => {
  await deleteSchedule(id)
  ElMessage.success('已删除')
  load()
}

const doDisinfect = async () => {
  await disinfectToolKit(uid)
  ElMessage.success('消毒完成，48 小时内可被派单')
  load()
}

onMounted(load)
</script>

<style scoped>
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.muted {
  color: #909399;
  font-size: 13px;
}
</style>
