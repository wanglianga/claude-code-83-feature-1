<template>
  <el-card shadow="never">
    <el-tabs v-model="tab">
      <!-- 定期巡访 -->
      <el-tab-pane label="定期巡访名单" name="patrol">
        <el-table :data="patrols" size="small" v-loading="loadingPatrol">
          <el-table-column label="老人" width="100">
            <template #default="{ row }">{{ row.elder.name }}</template>
          </el-table-column>
          <el-table-column label="住址" min-width="180">
            <template #default="{ row }">{{ row.elder.address }}</template>
          </el-table-column>
          <el-table-column label="风险" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="RISK_TYPES[row.elder.riskLevel]">{{ RISK_LABELS[row.elder.riskLevel] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="巡访间隔" width="90">
            <template #default="{ row }">{{ row.elder.patrolIntervalDays }} 天</template>
          </el-table-column>
          <el-table-column label="上次巡访" width="110">
            <template #default="{ row }">{{ row.elder.lastPatrolAt || '从未' }}</template>
          </el-table-column>
          <el-table-column label="下次应巡访" width="110">
            <template #default="{ row }">
              <span :style="{ color: row.due ? '#f56c6c' : 'inherit' }">{{ row.nextPatrolDate }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.due" type="danger" size="small">待巡访</el-tag>
              <el-tag v-else type="success" size="small">正常</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPatrol(row)">登记巡访</el-button>
              <el-button link type="success" @click="createPatrolOrder(row)">巡访理发</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 生日理发 -->
      <el-tab-pane label="生日理发" name="birthday">
        <el-alert type="success" :closable="false" title="未来 7 天过生日的老人，可一键生成公益生日理发服务单（全额补贴）" />
        <el-table :data="birthdays" size="small" style="margin-top: 12px" v-loading="loadingBirthday">
          <el-table-column label="老人" width="100">
            <template #default="{ row }">{{ row.elder.name }}</template>
          </el-table-column>
          <el-table-column label="生日" width="120">
            <template #default="{ row }">{{ row.birthday }}</template>
          </el-table-column>
          <el-table-column label="年龄" width="80">
            <template #default="{ row }">{{ row.age }} 岁</template>
          </el-table-column>
          <el-table-column label="倒计时" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.daysLeft <= 2 ? 'danger' : 'warning'">{{ row.daysLeft }} 天后</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="住址" min-width="180">
            <template #default="{ row }">{{ row.elder.address }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="createBirthdayOrder(row)">生成生日理发单</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 社区活动 -->
      <el-tab-pane label="社区活动集中服务" name="events">
        <div class="toolbar">
          <el-button type="primary" @click="eventDialog = true">发起活动</el-button>
        </div>
        <el-table :data="events" size="small" v-loading="loadingEvents">
          <el-table-column label="活动" min-width="180">
            <template #default="{ row }">{{ row.event.title }}</template>
          </el-table-column>
          <el-table-column label="时间" width="200">
            <template #default="{ row }">{{ row.event.eventDate }} {{ row.event.timeSlot }}</template>
          </el-table-column>
          <el-table-column label="地点" min-width="150">
            <template #default="{ row }">{{ row.event.location }}</template>
          </el-table-column>
          <el-table-column label="报名" width="90">
            <template #default="{ row }">{{ row.bookedCount }}/{{ row.event.maxElders }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.event.status === 'OPEN' ? 'success' : 'info'">
                {{ row.event.status === 'OPEN' ? '报名中' : row.event.status === 'CLOSED' ? '已截止' : '已结束' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.event.status === 'OPEN'" link type="primary" @click="openBook(row)">老人报名</el-button>
              <el-button v-if="row.event.status === 'OPEN'" link type="warning" @click="closeEvent(row)">截止</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 巡访登记 -->
    <el-dialog v-model="patrolDialog" title="登记巡访" width="460px">
      <el-form label-width="90px">
        <el-form-item label="巡访情况">
          <el-input v-model="patrolForm.content" type="textarea" :rows="3" placeholder="老人身体、精神状态、生活情况等" />
        </el-form-item>
        <el-form-item label="结论">
          <el-input v-model="patrolForm.result" placeholder="如：状态良好，无需额外关怀" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="patrolDialog = false">取消</el-button>
        <el-button type="primary" @click="doPatrol">保存</el-button>
      </template>
    </el-dialog>

    <!-- 发起活动 -->
    <el-dialog v-model="eventDialog" title="发起社区活动" width="480px">
      <el-form label-width="90px">
        <el-form-item label="活动名称"><el-input v-model="eventForm.title" /></el-form-item>
        <el-form-item label="活动日期">
          <el-date-picker v-model="eventForm.eventDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时段">
          <el-select v-model="eventForm.timeSlot" style="width: 100%">
            <el-option v-for="s in TIME_SLOTS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="地点"><el-input v-model="eventForm.location" /></el-form-item>
        <el-form-item label="名额"><el-input-number v-model="eventForm.maxElders" :min="1" :max="100" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="eventForm.notes" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="eventDialog = false">取消</el-button>
        <el-button type="primary" @click="doCreateEvent">创建</el-button>
      </template>
    </el-dialog>

    <!-- 活动报名 -->
    <el-dialog v-model="bookDialog" :title="`活动报名 - ${currentEvent?.event.title}`" width="420px">
      <el-select v-model="bookElderId" filterable placeholder="选择老人" style="width: 100%">
        <el-option v-for="e in elders" :key="e.id" :label="`${e.name}（${e.address}）`" :value="e.id" />
      </el-select>
      <template #footer>
        <el-button @click="bookDialog = false">取消</el-button>
        <el-button type="primary" @click="doBook">报名</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import {
  patrolList, recordPatrol, upcomingBirthdays, listEvents, createEvent, updateEventStatus,
  bookEvent, listElders, createOrder
} from '../api'
import { RISK_LABELS, RISK_TYPES, TIME_SLOTS } from '../utils/labels'

const router = useRouter()
const tab = ref('patrol')
const patrols = ref([])
const birthdays = ref([])
const events = ref([])
const elders = ref([])
const loadingPatrol = ref(false)
const loadingBirthday = ref(false)
const loadingEvents = ref(false)

const patrolDialog = ref(false)
const eventDialog = ref(false)
const bookDialog = ref(false)
const currentPatrol = ref(null)
const currentEvent = ref(null)
const bookElderId = ref(null)

const patrolForm = reactive({ content: '', result: '' })
const eventForm = reactive({
  title: '', eventDate: dayjs().add(7, 'day').format('YYYY-MM-DD'),
  timeSlot: '09:00-11:00', location: '社区活动中心', maxElders: 20, notes: ''
})

const loadPatrols = async () => {
  loadingPatrol.value = true
  try {
    patrols.value = await patrolList()
  } finally {
    loadingPatrol.value = false
  }
}
const loadBirthdays = async () => {
  loadingBirthday.value = true
  try {
    birthdays.value = await upcomingBirthdays(7)
  } finally {
    loadingBirthday.value = false
  }
}
const loadEvents = async () => {
  loadingEvents.value = true
  try {
    events.value = await listEvents()
  } finally {
    loadingEvents.value = false
  }
}

const openPatrol = (row) => {
  currentPatrol.value = row
  patrolForm.content = ''
  patrolForm.result = ''
  patrolDialog.value = true
}

const doPatrol = async () => {
  await recordPatrol(currentPatrol.value.elder.id, patrolForm)
  ElMessage.success('巡访已登记')
  patrolDialog.value = false
  loadPatrols()
}

const createPatrolOrder = async (row) => {
  const order = await createOrder({
    elderId: row.elder.id,
    scheduledDate: dayjs().add(1, 'day').format('YYYY-MM-DD'),
    timeSlot: '09:00-11:00',
    type: 'PATROL'
  })
  ElMessage.success(`巡访理发单 ${order.orderNo} 已生成`)
  router.push(`/orders/${order.id}`)
}

const createBirthdayOrder = async (row) => {
  const order = await createOrder({
    elderId: row.elder.id,
    scheduledDate: row.birthday,
    timeSlot: '09:00-11:00',
    type: 'BIRTHDAY'
  })
  ElMessage.success(`生日理发单 ${order.orderNo} 已生成（全额公益补贴）`)
  router.push(`/orders/${order.id}`)
}

const doCreateEvent = async () => {
  if (!eventForm.title) {
    ElMessage.warning('请填写活动名称')
    return
  }
  await createEvent(eventForm)
  ElMessage.success('活动已创建')
  eventDialog.value = false
  loadEvents()
}

const closeEvent = async (row) => {
  await updateEventStatus(row.event.id, 'CLOSED')
  ElMessage.success('已截止报名')
  loadEvents()
}

const openBook = (row) => {
  currentEvent.value = row
  bookElderId.value = null
  bookDialog.value = true
}

const doBook = async () => {
  if (!bookElderId.value) {
    ElMessage.warning('请选择老人')
    return
  }
  const order = await bookEvent(currentEvent.value.event.id, { elderId: bookElderId.value })
  ElMessage.success(`报名成功，服务单 ${order.orderNo} 已生成`)
  bookDialog.value = false
  loadEvents()
}

onMounted(async () => {
  loadPatrols()
  loadBirthdays()
  loadEvents()
  elders.value = (await listElders({})).filter((e) => e.status === 'ACTIVE')
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>
