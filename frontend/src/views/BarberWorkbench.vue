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
              <el-tag v-if="toolkit" :type="DISINFECTION_TYPES[effectiveStatus]" size="small">
                {{ DISINFECTION_LABELS[effectiveStatus] }}
              </el-tag>
            </div>
          </template>
          <template v-if="toolkit">
            <p><b>{{ toolkit.name }}</b></p>
            <p class="muted">清单：{{ toolkit.items }}</p>
            <el-descriptions :column="1" size="small" border style="margin: 6px 0">
              <el-descriptions-item label="封签编号">
                {{ toolkit.sealNo || '未登记' }}
                <el-tag size="small" :type="toolkit.sealIntact ? 'success' : 'danger'" style="margin-left: 4px">
                  {{ toolkit.sealIntact ? '完好' : '破损' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="消毒时间">{{ toolkit.disinfectedAt || '从未消毒' }}</el-descriptions-item>
              <el-descriptions-item label="消毒方式">{{ toolkit.disinfectionMethod || '—' }}</el-descriptions-item>
              <el-descriptions-item label="消毒柜">{{ toolkit.cabinetNo || '—' }}</el-descriptions-item>
              <el-descriptions-item label="责任人">{{ toolkit.responsiblePerson || '—' }}</el-descriptions-item>
            </el-descriptions>
            <p class="muted">消毒 48 小时内有效，过期/封签破损将无法派单且上门核验不通过</p>
            <el-button type="primary" size="small" style="margin-top: 8px" @click="disinfectDialog = true">完成消毒并封签</el-button>
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

    <el-dialog v-model="disinfectDialog" title="完成消毒并封签" width="460px">
      <el-alert type="info" :closable="false" style="margin-bottom: 10px"
        title="登记新封签编号、消毒方式、消毒柜编号与责任人，生成 48 小时有效消毒记录。" />
      <el-form label-width="100px">
        <el-form-item label="封签编号"><el-input v-model="disinfectForm.sealNo" placeholder="如 FB-20260918-009" /></el-form-item>
        <el-form-item label="消毒方式">
          <el-select v-model="disinfectForm.method" style="width: 100%" allow-create filterable>
            <el-option label="紫外线消毒柜（30分钟）" value="紫外线消毒柜（30分钟）" />
            <el-option label="高温蒸煮（15分钟）" value="高温蒸煮（15分钟）" />
            <el-option label="含氯消毒剂浸泡（30分钟）" value="含氯消毒剂浸泡（30分钟）" />
          </el-select>
        </el-form-item>
        <el-form-item label="消毒柜编号"><el-input v-model="disinfectForm.cabinetNo" placeholder="如 UV-A01" /></el-form-item>
        <el-form-item label="责任人"><el-input v-model="disinfectForm.responsiblePerson" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disinfectDialog = false">取消</el-button>
        <el-button type="primary" @click="doDisinfect">确认消毒封签</el-button>
      </template>
    </el-dialog>

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
import { ORDER_STATUS_LABELS, ORDER_STATUS_TYPES, DISINFECTION_LABELS, DISINFECTION_TYPES } from '../utils/labels'

const store = useUserStore()
const orders = ref([])
const schedules = ref([])
const toolkit = ref(null)
const profile = ref(null)
const loading = ref(false)
const scheduleDialog = ref(false)
const disinfectDialog = ref(false)
const disinfectForm = reactive({ sealNo: '', method: '紫外线消毒柜（30分钟）', cabinetNo: '', responsiblePerson: '李理发' })
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
  await disinfectToolKit(uid, { ...disinfectForm })
  ElMessage.success('消毒封签完成，48 小时内可被派单')
  disinfectDialog.value = false
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
