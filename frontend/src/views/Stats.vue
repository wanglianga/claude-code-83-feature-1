<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>楼栋服务覆盖</template>
          <div ref="buildingChart" style="height: 280px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>志愿者陪同服务量</template>
          <div ref="volunteerChart" style="height: 280px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>理发师服务质量与信用复盘</template>
      <el-table :data="barbers" size="small">
        <el-table-column prop="name" label="理发师" width="110" />
        <el-table-column label="信用分" width="160">
          <template #default="{ row }">
            <el-progress :percentage="row.profile.creditScore" :stroke-width="14"
              :color="row.profile.creditScore >= 90 ? '#67c23a' : row.profile.creditScore >= 70 ? '#e6a23c' : '#f56c6c'" />
          </template>
        </el-table-column>
        <el-table-column label="完成单数" width="90">
          <template #default="{ row }">{{ row.completed }}</template>
        </el-table-column>
        <el-table-column label="取消" width="70">
          <template #default="{ row }">{{ row.cancelled }}</template>
        </el-table-column>
        <el-table-column label="迟到" width="70">
          <template #default="{ row }">{{ row.profile.lateCount }}</template>
        </el-table-column>
        <el-table-column label="事故" width="70">
          <template #default="{ row }">{{ row.profile.incidentCount }}</template>
        </el-table-column>
        <el-table-column label="感染事故" width="80">
          <template #default="{ row }">{{ row.profile.infectionCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="派单权重" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.profile.dispatchWeight >= 90 ? 'success' : 'warning'">{{ row.profile.dispatchWeight }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上门资格" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.profile.suspended ? 'danger' : 'success'">
              {{ row.profile.suspended ? '已暂停' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="平均满意度" width="110">
          <template #default="{ row }">
            <el-rate :model-value="row.avgSatisfaction" disabled size="small" allow-half />
          </template>
        </el-table-column>
        <el-table-column label="操作" v-if="['ADMIN', 'STAFF'].includes(role)">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCredit(row)">信用调整</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="card-head">
          <span>工具消毒 · 交叉感染投诉 · 复检与补偿复盘</span>
          <el-button size="small" type="primary" @click="$router.push('/infection-trace')">前往追溯处置</el-button>
        </div>
      </template>
      <el-row :gutter="12">
        <el-col :span="4"><div class="kpi"><div class="num">{{ infection.caseTotal ?? 0 }}</div><div class="label">追溯单总数</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="num" style="color:#f56c6c">{{ infection.caseOpen ?? 0 }}</div><div class="label">处理中</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="num" style="color:#e6a23c">{{ infection.contactTotal ?? 0 }}</div><div class="label">接触老人</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="num" style="color:#f56c6c">{{ infection.contactInfected ?? 0 }}</div><div class="label">确诊感染</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="num">{{ infection.supplementaryTotal ?? 0 }}</div><div class="label">备用包补录</div></div></el-col>
        <el-col :span="4"><div class="kpi"><div class="num" style="color:#e6a23c">{{ infection.compensationPending ?? 0 }}</div><div class="label">空跑补偿待认定</div></div></el-col>
      </el-row>
      <el-descriptions :column="3" border size="small" style="margin-top: 12px">
        <el-descriptions-item label="工具类异常/投诉">{{ infection.toolExceptions ?? 0 }} 起</el-descriptions-item>
        <el-descriptions-item label="已通知接触老人">{{ infection.contactNotified ?? 0 }} 人</el-descriptions-item>
        <el-descriptions-item label="公益资金空跑补偿">¥{{ infection.communityCompensationTotal ?? 0 }}（理发师承担 {{ infection.barberBorneTotal ?? 0 }} 单）</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>公益资金复盘（按月）</template>
          <div ref="fundChart" style="height: 260px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>预警名单（自动转网格员关怀）</template>
          <el-tabs>
            <el-tab-pane :label="`长期未预约 (${alerts.noAppointment?.length || 0})`">
              <el-table :data="alerts.noAppointment" size="small">
                <el-table-column label="老人" width="90">
                  <template #default="{ row }">{{ row.elder.name }}</template>
                </el-table-column>
                <el-table-column label="住址" min-width="160">
                  <template #default="{ row }">{{ row.elder.address }}</template>
                </el-table-column>
                <el-table-column label="最近服务" width="110">
                  <template #default="{ row }">{{ row.lastOrderAt || '从未预约' }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane :label="`连续取消 (${alerts.repeatedCancel?.length || 0})`">
              <el-table :data="alerts.repeatedCancel" size="small">
                <el-table-column label="老人" width="90">
                  <template #default="{ row }">{{ row.elder.name }}</template>
                </el-table-column>
                <el-table-column label="住址" min-width="160">
                  <template #default="{ row }">{{ row.elder.address }}</template>
                </el-table-column>
                <el-table-column label="连续取消" width="90">
                  <template #default="{ row }">
                    <el-tag type="danger" size="small">{{ row.cancelCount }} 次</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="creditDialog" title="理发师信用调整" width="420px">
      <el-form label-width="90px">
        <el-form-item label="调整分值">
          <el-input-number v-model="creditForm.delta" :min="-20" :max="20" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="creditForm.reason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="creditDialog = false">取消</el-button>
        <el-button type="primary" @click="doAdjust">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { statsCoverage, statsBarbers, statsFunds, statsAlerts, statsInfectionReview, adjustCredit } from '../api'
import { useUserStore } from '../store/user'
import { RISK_LABELS } from '../utils/labels'

const store = useUserStore()
const role = computed(() => store.role)
const barbers = ref([])
const alerts = ref({})
const infection = ref({})
const buildingChart = ref(null)
const volunteerChart = ref(null)
const fundChart = ref(null)
const creditDialog = ref(false)
const currentBarber = ref(null)
const creditForm = reactive({ delta: 1, reason: '' })

const load = async () => {
  const promises = [statsCoverage(), statsBarbers(), statsFunds(), statsAlerts()]
  if (['ADMIN', 'STAFF', 'FINANCE'].includes(store.role)) {
    promises.push(statsInfectionReview().catch(() => ({})))
  }
  const [cov, b, funds, al, inf] = await Promise.all(promises)
  barbers.value = b
  alerts.value = al
  infection.value = inf || {}
  await nextTick()

  echarts.init(buildingChart.value).setOption({
    tooltip: {},
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: cov.byBuilding.map((d) => d.building) },
    yAxis: { type: 'value' },
    series: [
      { name: '在册老人', type: 'bar', data: cov.byBuilding.map((d) => d.elderCount), itemStyle: { color: '#409eff' } },
      { name: '已服务', type: 'bar', data: cov.byBuilding.map((d) => d.servedCount), itemStyle: { color: '#67c23a' } }
    ]
  })

  echarts.init(volunteerChart.value).setOption({
    tooltip: {},
    xAxis: { type: 'category', data: cov.byVolunteer.map((d) => d.name) },
    yAxis: { type: 'value' },
    series: [{ name: '陪同服务次数', type: 'bar', data: cov.byVolunteer.map((d) => d.orderCount), itemStyle: { color: '#9b59b6' } }]
  })

  const months = Object.keys(funds.byMonth || {})
  echarts.init(fundChart.value).setOption({
    tooltip: {},
    grid: { left: 60, right: 20, top: 20, bottom: 25 },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value' },
    series: [{ name: '补贴金额', type: 'line', smooth: true, areaStyle: {}, data: months.map((m) => funds.byMonth[m]), itemStyle: { color: '#e6a23c' } }]
  })
}

const openCredit = (row) => {
  currentBarber.value = row
  creditForm.delta = 1
  creditForm.reason = ''
  creditDialog.value = true
}

const doAdjust = async () => {
  await adjustCredit(currentBarber.value.profile.userId, creditForm)
  ElMessage.success('信用分已调整')
  creditDialog.value = false
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
.kpi {
  text-align: center;
  background: #f7f9fc;
  border-radius: 6px;
  padding: 12px 4px;
}
.kpi .num {
  font-size: 24px;
  font-weight: 700;
}
.kpi .label {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
</style>
