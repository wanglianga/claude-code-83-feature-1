<template>
  <div>
    <el-card shadow="never" style="max-width: 760px">
      <template #header>
        <span>新建预约（智能派单）</span>
        <div class="tip">系统将根据理发师排班、志愿者陪同、老人风险等级、楼栋距离、工具消毒状态自动匹配</div>
      </template>
      <el-form :model="form" label-width="100px">
        <el-form-item label="选择老人" required>
          <el-select v-model="form.elderId" filterable placeholder="搜索老人姓名" style="width: 100%" @change="onElderChange">
            <el-option v-for="e in elders" :key="e.id" :label="`${e.name}（${e.address}）`" :value="e.id" />
          </el-select>
        </el-form-item>
        <template v-if="elder">
          <el-form-item label="老人信息">
            <el-space wrap>
              <el-tag :type="RISK_TYPES[elder.riskLevel]">{{ RISK_LABELS[elder.riskLevel] }}</el-tag>
              <el-tag v-if="elder.livingAlone" type="danger">独居</el-tag>
              <el-tag v-if="elder.needFamilyPresent" type="warning">需家属在场</el-tag>
              <el-tag type="success">{{ SUBSIDY_LABELS[elder.subsidyType] }}</el-tag>
              <el-tag type="info">距服务点 {{ elder.buildingDistance }} 米</el-tag>
            </el-space>
          </el-form-item>
          <el-form-item label="理发偏好">
            <span>{{ elder.hairPreference || '—' }}；上门时间偏好：{{ elder.preferredTime || '—' }}</span>
          </el-form-item>
        </template>
        <el-form-item label="服务类型">
          <el-radio-group v-model="form.type">
            <el-radio-button value="NORMAL">普通预约</el-radio-button>
            <el-radio-button value="BIRTHDAY">生日理发</el-radio-button>
            <el-radio-button value="PATROL">巡访理发</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="预约日期" required>
          <el-date-picker v-model="form.scheduledDate" type="date" value-format="YYYY-MM-DD"
            :disabled-date="(d) => d.isBefore(dayjs().startOf('day'))" />
        </el-form-item>
        <el-form-item label="上门时段" required>
          <el-select v-model="form.timeSlot" placeholder="选择时段" style="width: 240px">
            <el-option v-for="s in TIME_SLOTS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="指定理发师">
          <el-select v-model="form.barberId" clearable placeholder="默认智能匹配" style="width: 240px">
            <el-option v-for="b in barbers" :key="b.profile.userId" :label="b.realName" :value="b.profile.userId" />
          </el-select>
          <span class="hint">不指定时按楼栋距离、负载与消毒状态自动匹配</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">提交并智能派单</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="resultVisible" title="派单成功" width="480px">
      <el-result icon="success" :title="`服务单 ${created?.orderNo}`"
        :sub-title="`理发师：${created?.barberName}；志愿者：${created?.volunteerName || '无需陪同'}；补贴 ¥${created?.subsidyAmount}，自费 ¥${created?.selfPayAmount}`" />
      <template #footer>
        <el-button @click="resultVisible = false">继续预约</el-button>
        <el-button type="primary" @click="$router.push(`/orders/${created.id}`)">查看服务单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { listElders, listBarbers, createOrder } from '../api'
import { RISK_TYPES, RISK_LABELS, SUBSIDY_LABELS, TIME_SLOTS } from '../utils/labels'

const route = useRoute()
const elders = ref([])
const barbers = ref([])
const submitting = ref(false)
const resultVisible = ref(false)
const created = ref(null)

const form = ref({
  elderId: route.query.elderId ? Number(route.query.elderId) : null,
  scheduledDate: dayjs().add(1, 'day').format('YYYY-MM-DD'),
  timeSlot: '09:00-11:00',
  type: 'NORMAL',
  barberId: null
})

const elder = computed(() => elders.value.find((e) => e.id === form.value.elderId))

const onElderChange = () => {}

const submit = async () => {
  if (!form.value.elderId || !form.value.scheduledDate || !form.value.timeSlot) {
    return
  }
  submitting.value = true
  try {
    created.value = await createOrder(form.value)
    resultVisible.value = true
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  elders.value = (await listElders({})).filter((e) => e.status === 'ACTIVE')
  barbers.value = await listBarbers()
})
</script>

<style scoped>
.tip {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}
.hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
