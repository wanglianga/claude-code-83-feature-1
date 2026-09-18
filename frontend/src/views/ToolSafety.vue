<template>
  <div>
    <el-tabs v-model="tab" @tab-change="loadAll">
      <!-- 备用服务包与消毒补录 -->
      <el-tab-pane label="备用服务包 / 消毒补录" name="kits">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span>社区备用服务包</span>
              <el-button v-if="isStaff" type="primary" size="small" @click="kitDialog = true">新增备用服务包</el-button>
            </div>
          </template>
          <el-table :data="kits" size="small" v-loading="loading">
            <el-table-column prop="name" label="名称" min-width="130" />
            <el-table-column prop="sealNo" label="封签编号" width="140" />
            <el-table-column prop="disinfectionMethod" label="消毒方式" min-width="150" show-overflow-tooltip />
            <el-table-column prop="cabinetNo" label="消毒柜" width="90" />
            <el-table-column prop="responsiblePerson" label="责任人" width="90" />
            <el-table-column label="消毒有效期" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="kitValid(row) ? 'success' : 'danger'">
                  {{ row.disinfectionPending ? '待补录' : kitValid(row) ? '有效' : '已过期' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="disinfectedAt" label="消毒时间" width="160" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button v-if="isStaff" link type="primary" @click="openRecordDisinfection(row)">补录消毒</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-alert type="warning" :closable="false" style="margin-top: 10px"
            title="备用服务包使用后必须补录消毒记录（新封签、消毒方式、柜号、责任人），未补录前不得再次派单。" />
        </el-card>
      </el-tab-pane>

      <!-- 工具问题与空跑补偿 -->
      <el-tab-pane label="工具问题 / 空跑补偿" name="issues">
        <el-card shadow="never">
          <div class="toolbar">
            <el-radio-group v-model="issueFilter" @change="loadIssues">
              <el-radio-button value="all">全部</el-radio-button>
              <el-radio-button value="pending">待补偿认定</el-radio-button>
            </el-radio-group>
          </div>
          <el-table :data="issues" size="small" v-loading="loading" empty-text="暂无工具问题记录">
            <el-table-column label="服务单" width="90">
              <template #default="{ row }">
                <el-button link type="primary" @click="$router.push(`/orders/${row.orderId}`)">#{{ row.orderId }}</el-button>
              </template>
            </el-table-column>
            <el-table-column label="问题" width="130">
              <template #default="{ row }">{{ TOOL_ISSUE_LABELS[row.issueType] }}</template>
            </el-table-column>
            <el-table-column prop="issueDetail" label="详情" min-width="160" show-overflow-tooltip />
            <el-table-column label="处置" width="120">
              <template #default="{ row }">{{ TOOL_ACTION_LABELS[row.action] }}</template>
            </el-table-column>
            <el-table-column label="补偿认定" width="140">
              <template #default="{ row }">
                <el-tag size="small" :type="COMPENSATION_TYPES[row.compensationStatus]">
                  {{ COMPENSATION_LABELS[row.compensationStatus] }}
                </el-tag>
                <span v-if="Number(row.compensationAmount) > 0"> ¥{{ row.compensationAmount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="compensationNote" label="说明" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right" v-if="canReviewComp">
              <template #default="{ row }">
                <el-button link type="primary" @click="openComp(row)">认定补偿</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 交叉感染追溯 -->
      <el-tab-pane label="交叉感染追溯" name="trace">
        <el-card shadow="never">
          <el-table :data="traces" size="small" v-loading="loading" empty-text="暂无追溯记录"
            @row-click="(row) => openTrace(row.id)" highlight-current-row>
            <el-table-column prop="id" label="编号" width="70" />
            <el-table-column prop="sourceElderName" label="反馈老人" width="100" />
            <el-table-column prop="barberName" label="理发师" width="90" />
            <el-table-column prop="sealNo" label="封签/包" width="140" show-overflow-tooltip />
            <el-table-column prop="symptom" label="症状" width="120" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="INFECTION_STATUS_TYPES[row.status]">
                  {{ INFECTION_STATUS_LABELS[row.status] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="通知" width="70">
              <template #default="{ row }">{{ row.notified ? '已通知' : '待通知' }}</template>
            </el-table-column>
            <el-table-column label="资格" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.barberSuspended" size="small" type="danger">已暂停</el-tag>
                <span v-else>正常</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="openTrace(row.id)">处置</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增备用服务包 -->
    <el-dialog v-model="kitDialog" title="新增备用服务包" width="520px">
      <el-form label-width="100px">
        <el-form-item label="名称"><el-input v-model="kitForm.name" placeholder="如：社区备用服务包③" /></el-form-item>
        <el-form-item label="工具清单"><el-input v-model="kitForm.items" placeholder="一次性围布,一次性剃刀,消毒棉片" /></el-form-item>
        <el-form-item label="封签编号"><el-input v-model="kitForm.sealNo" /></el-form-item>
        <el-form-item label="消毒方式"><el-input v-model="kitForm.method" /></el-form-item>
        <el-form-item label="消毒柜编号"><el-input v-model="kitForm.cabinetNo" /></el-form-item>
        <el-form-item label="责任人"><el-input v-model="kitForm.responsiblePerson" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="kitDialog = false">取消</el-button>
        <el-button type="primary" @click="doCreateKit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 补录消毒 -->
    <el-dialog v-model="recordDialog" title="补录消毒记录" width="480px">
      <el-alert type="info" :closable="false" style="margin-bottom: 10px"
        title="补录后该备用服务包恢复可派单状态，并生成新的 48 小时消毒有效期。" />
      <el-form label-width="100px">
        <el-form-item label="新封签编号"><el-input v-model="recordForm.sealNo" /></el-form-item>
        <el-form-item label="消毒方式"><el-input v-model="recordForm.method" placeholder="紫外线/高温蒸煮/含氯浸泡" /></el-form-item>
        <el-form-item label="消毒柜编号"><el-input v-model="recordForm.cabinetNo" /></el-form-item>
        <el-form-item label="责任人"><el-input v-model="recordForm.responsiblePerson" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordDialog = false">取消</el-button>
        <el-button type="primary" @click="doRecord">补录完成</el-button>
      </template>
    </el-dialog>

    <!-- 补偿认定 -->
    <el-dialog v-model="compDialog" title="理发师空跑补偿认定" width="480px">
      <el-form label-width="110px">
        <el-form-item label="认定结果">
          <el-radio-group v-model="compForm.result">
            <el-radio value="COMMUNITY">社区规则补偿</el-radio>
            <el-radio value="BARBER_BEAR">理发师承担</el-radio>
            <el-radio value="NONE">无需补偿</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="compForm.result === 'COMMUNITY'" label="补偿金额">
          <el-input-number v-model="compForm.amount" :min="0" :precision="2" /> 元（公益资金支出）
        </el-form-item>
        <el-form-item label="认定说明"><el-input v-model="compForm.note" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="compDialog = false">取消</el-button>
        <el-button type="primary" @click="doComp">确认认定</el-button>
      </template>
    </el-dialog>

    <!-- 感染追溯处置抽屉 -->
    <el-drawer v-model="traceDialog" title="交叉感染追溯处置" size="640px">
      <template v-if="trace">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="INFECTION_STATUS_TYPES[trace.status]">{{ INFECTION_STATUS_LABELS[trace.status] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="理发师">{{ trace.barberName }}</el-descriptions-item>
          <el-descriptions-item label="反馈老人">{{ trace.sourceElderName }}</el-descriptions-item>
          <el-descriptions-item label="症状">{{ trace.symptom }}</el-descriptions-item>
          <el-descriptions-item label="服务包/封签" :span="2">{{ trace.sealNo || '—' }}（包 #{{ trace.kitId || '—' }}）</el-descriptions-item>
          <el-descriptions-item label="说明" :span="2">{{ trace.description }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 16px 0 8px">同工具 / 同日服务老人（反查名单）</h4>
        <el-table :data="contacts" size="small" border>
          <el-table-column prop="elderName" label="老人" width="90" />
          <el-table-column prop="volunteerName" label="志愿者" width="90">
            <template #default="{ row }">{{ row.volunteerName || '—' }}</template>
          </el-table-column>
          <el-table-column label="通知" width="130">
            <template #default="{ row }">
              <el-tag size="small" :type="row.familyNotified ? 'success' : 'info'">
                家属{{ row.familyNotified ? '已通知' : '待通知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="就医结果" min-width="120">
            <template #default="{ row }">{{ row.medicalResult || '待反馈' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="doNotify(row)">通知/建议就医</el-button>
              <el-button link type="warning" size="small" @click="openResult(row)">记录结果</el-button>
            </template>
          </el-table-column>
        </el-table>

        <template v-if="isStaff && trace.status !== 'RESOLVED'">
          <el-divider />
          <h4>责任认定与处置</h4>
          <el-form label-width="130px">
            <el-form-item label="确认交叉感染"><el-switch v-model="confirmForm.crossInfection" /></el-form-item>
            <el-form-item label="确认消毒责任"><el-switch v-model="confirmForm.disinfectionFault" /></el-form-item>
            <el-form-item label="派单权重下调至">
              <el-input-number v-model="confirmForm.weight" :min="0" :max="1" :step="0.1" />
            </el-form-item>
            <el-form-item label="说明"><el-input v-model="confirmForm.note" type="textarea" :rows="2" /></el-form-item>
          </el-form>
          <el-alert type="error" :closable="false" show-icon style="margin-bottom: 10px"
            title="确认后将暂停该理发师上门资格、安排消毒培训与工具复检、下调派单权重；问题服务包停用待复检。" />
          <el-button type="danger" @click="doConfirm">确认责任并处置</el-button>
        </template>

        <template v-if="trace.barberSuspended && trace.status !== 'RESOLVED' && isStaff">
          <el-divider />
          <h4>复岗复检</h4>
          <el-form label-width="130px">
            <el-form-item label="消毒培训通过"><el-switch v-model="resolveForm.trainingPassed" /></el-form-item>
            <el-form-item label="工具复检通过"><el-switch v-model="resolveForm.toolReinspected" /></el-form-item>
            <el-form-item label="恢复派单权重"><el-switch v-model="resolveForm.restoreWeight" /></el-form-item>
            <el-form-item label="结案结论"><el-input v-model="resolveForm.resolution" type="textarea" :rows="2" /></el-form-item>
          </el-form>
          <el-button type="success" @click="doResolve">培训复检通过，结案复岗</el-button>
        </template>

        <el-alert v-if="trace.status === 'RESOLVED'" type="success" :closable="false"
          :title="'已结案：' + (trace.resolution || '')" style="margin-top: 12px" />
      </template>
    </el-drawer>

    <!-- 接触者就医结果 -->
    <el-dialog v-model="resultDialog" title="记录就医 / 排查结果" width="460px">
      <el-form label-width="100px">
        <el-form-item label="排查结果">
          <el-select v-model="resultForm.medicalResult" style="width: 100%" allow-create filterable>
            <el-option label="无异常" value="无异常" />
            <el-option label="轻微皮疹，已处理" value="轻微皮疹，已处理" />
            <el-option label="确诊皮肤感染，就医中" value="确诊皮肤感染，就医中" />
          </el-select>
        </el-form-item>
        <el-form-item label="回访记录"><el-input v-model="resultForm.followUpNote" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resultDialog = false">取消</el-button>
        <el-button type="primary" @click="doRecordResult">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import {
  listBackupKits, createBackupKit, listToolIssues, reviewCompensation,
  listInfections, infectionDetail, notifyContact, recordContactResult,
  confirmInfectionResponsibility, resolveInfection, recordDisinfection
} from '../api'
import { useUserStore } from '../store/user'
import {
  TOOL_ISSUE_LABELS, TOOL_ACTION_LABELS, COMPENSATION_LABELS, COMPENSATION_TYPES,
  INFECTION_STATUS_LABELS, INFECTION_STATUS_TYPES
} from '../utils/labels'

const store = useUserStore()
const isStaff = ['ADMIN', 'STAFF'].includes(store.role)
const canReviewComp = ['ADMIN', 'STAFF', 'FINANCE'].includes(store.role)

const tab = ref('kits')
const loading = ref(false)
const kits = ref([])
const issues = ref([])
const traces = ref([])
const issueFilter = ref('all')

const kitDialog = ref(false)
const recordDialog = ref(false)
const compDialog = ref(false)
const traceDialog = ref(false)
const resultDialog = ref(false)

const kitForm = reactive({ name: '', items: '', sealNo: '', method: '', cabinetNo: '', responsiblePerson: '' })
const recordForm = reactive({ id: null, sealNo: '', method: '', cabinetNo: '', responsiblePerson: '' })
const compForm = reactive({ id: null, result: 'COMMUNITY', amount: 10, note: '' })
const trace = ref(null)
const contacts = ref([])
const confirmForm = reactive({ crossInfection: true, disinfectionFault: true, weight: 0.5, note: '' })
const resolveForm = reactive({ trainingPassed: false, toolReinspected: false, restoreWeight: false, resolution: '' })
const resultForm = reactive({ id: null, medicalResult: '', followUpNote: '' })

const kitValid = (row) =>
  !row.disinfectionPending && row.disinfectedAt && dayjs(row.disinfectedAt).add(48, 'hour').isAfter(dayjs())

const loadKits = async () => { kits.value = await listBackupKits() }
const loadIssues = async () => {
  issues.value = await listToolIssues(issueFilter.value === 'pending' ? { pendingOnly: true } : {})
}
const loadTraces = async () => { traces.value = await listInfections({}) }

const loadAll = async () => {
  loading.value = true
  try {
    await Promise.all([loadKits(), loadIssues(), loadTraces()])
  } finally {
    loading.value = false
  }
}

const doCreateKit = async () => {
  await createBackupKit({ ...kitForm })
  ElMessage.success('备用服务包已创建')
  kitDialog.value = false
  Object.keys(kitForm).forEach((k) => (kitForm[k] = ''))
  loadKits()
}

const openRecordDisinfection = (row) => {
  Object.assign(recordForm, {
    id: row.id, sealNo: row.sealNo || '', method: row.disinfectionMethod || '',
    cabinetNo: row.cabinetNo || '', responsiblePerson: row.responsiblePerson || ''
  })
  recordDialog.value = true
}
const doRecord = async () => {
  await recordDisinfection(recordForm.id, { ...recordForm, id: undefined })
  ElMessage.success('消毒记录已补录，备用包恢复可派单')
  recordDialog.value = false
  loadKits()
}

const openComp = (row) => {
  Object.assign(compForm, {
    id: row.id,
    result: row.compensationStatus === 'BARBER_BEAR' ? 'BARBER_BEAR' : 'COMMUNITY',
    amount: Number(row.compensationAmount) || 10, note: row.compensationNote || ''
  })
  compDialog.value = true
}
const doComp = async () => {
  await reviewCompensation(compForm.id, { result: compForm.result, amount: compForm.amount, note: compForm.note })
  ElMessage.success('补偿认定已记录，进入公益资金复盘')
  compDialog.value = false
  loadIssues()
}

const openTrace = async (id) => {
  const data = await infectionDetail(id)
  trace.value = data.trace
  contacts.value = data.contacts
  Object.assign(confirmForm, {
    crossInfection: trace.value.crossInfectionConfirmed || true,
    disinfectionFault: trace.value.disinfectionResponsibility || true,
    weight: 0.5, note: ''
  })
  Object.assign(resolveForm, { trainingPassed: false, toolReinspected: false, restoreWeight: false, resolution: '' })
  traceDialog.value = true
}

const doNotify = async (row) => {
  await notifyContact(row.id)
  ElMessage.success('已通知家属和志愿者并建议就医')
  openTrace(trace.value.id)
}
const openResult = (row) => {
  Object.assign(resultForm, { id: row.id, medicalResult: row.medicalResult || '', followUpNote: row.followUpNote || '' })
  resultDialog.value = true
}
const doRecordResult = async () => {
  await recordContactResult(resultForm.id, { ...resultForm, id: undefined })
  ElMessage.success('就医排查结果已记录')
  resultDialog.value = false
  openTrace(trace.value.id)
}

const doConfirm = async () => {
  await confirmInfectionResponsibility(trace.value.id, { ...confirmForm })
  ElMessage.success('已暂停上门资格、安排培训复检并下调派单权重')
  traceDialog.value = false
  loadTraces()
}
const doResolve = async () => {
  if (!resolveForm.trainingPassed || !resolveForm.toolReinspected) {
    ElMessage.warning('消毒培训与工具复检均需通过才能复岗')
    return
  }
  await resolveInfection(trace.value.id, { ...resolveForm })
  ElMessage.success('已结案，理发师上门资格恢复')
  traceDialog.value = false
  loadTraces()
}

onMounted(loadAll)
</script>

<style scoped>
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.toolbar {
  margin-bottom: 12px;
}
</style>
