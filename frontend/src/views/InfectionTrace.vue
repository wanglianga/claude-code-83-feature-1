<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="7">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span>交叉感染追溯单</span>
              <el-select v-model="statusFilter" size="small" style="width: 130px" @change="loadList">
                <el-option label="全部" value="" />
                <el-option v-for="(v, k) in INFECTION_CASE_STATUS_LABELS" :key="k" :label="v" :value="k" />
              </el-select>
            </div>
          </template>
          <el-table :data="cases" size="small" v-loading="loading" highlight-current-row
                    @row-click="(row) => loadDetail(row.id)">
            <el-table-column prop="caseNo" label="单号" width="130" />
            <el-table-column prop="elderName" label="反馈老人" width="90" />
            <el-table-column prop="barberName" label="理发师" width="80" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="INFECTION_CASE_STATUS_TYPES[row.status]">
                  {{ INFECTION_CASE_STATUS_LABELS[row.status] }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="17">
        <el-card v-if="!detail" shadow="never">
          <el-empty description="请选择左侧追溯单查看反查与处置" />
        </el-card>

        <template v-else>
          <el-card shadow="never">
            <template #header>
              <div class="card-head">
                <span>{{ detail.case.caseNo }} · 反查处置</span>
                <el-tag :type="INFECTION_CASE_STATUS_TYPES[detail.case.status]">
                  {{ INFECTION_CASE_STATUS_LABELS[detail.case.status] }}
                </el-tag>
              </div>
            </template>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="触发服务单">
                <el-button link type="primary" @click="$router.push(`/orders/${detail.case.triggerOrderId}`)">
                  #{{ detail.case.triggerOrderId }}
                </el-button>
              </el-descriptions-item>
              <el-descriptions-item label="暴露/服务日期">{{ detail.case.exposureDate }}</el-descriptions-item>
              <el-descriptions-item label="反馈老人">{{ detail.case.elderName }}</el-descriptions-item>
              <el-descriptions-item label="理发师">{{ detail.case.barberName }}</el-descriptions-item>
              <el-descriptions-item label="涉事服务包">{{ detail.case.kitName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="工具编号">{{ detail.case.toolItems || '—' }}</el-descriptions-item>
              <el-descriptions-item label="症状/原因" :span="2">{{ detail.case.symptom }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ detail.case.description }}</el-descriptions-item>
            </el-descriptions>
            <div style="margin-top: 12px">
              <el-button type="warning" :disabled="detail.case.status === 'CONFIRMED' || detail.case.status === 'RULED_OUT'"
                         @click="doNotify">一键通知家属和志愿者并建议就医</el-button>
              <el-button type="danger" plain
                         :disabled="detail.case.status === 'CONFIRMED' || detail.case.status === 'RULED_OUT'"
                         @click="concludeDialog = true">确认结论与处置</el-button>
            </div>
          </el-card>

          <el-card shadow="never" style="margin-top: 16px">
            <template #header>
              同理发师 / 同服务包 / 同工具编号 · 同日服务的其他老人（{{ contacts.length }} 人）
            </template>
            <el-table :data="contacts" size="small" empty-text="未反查到同日同工具服务的其他老人">
              <el-table-column prop="elderName" label="老人" width="90" />
              <el-table-column label="服务单" width="90">
                <template #default="{ row }">
                  <el-button link type="primary" @click="$router.push(`/orders/${row.orderId}`)">#{{ row.orderId }}</el-button>
                </template>
              </el-table-column>
              <el-table-column prop="familyContactName" label="家属" width="90" />
              <el-table-column prop="volunteerName" label="志愿者" width="90" />
              <el-table-column prop="sharedTools" label="共用工具" min-width="140" show-overflow-tooltip />
              <el-table-column label="家属通知" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.familyNotified ? 'success' : 'info'">
                    {{ row.familyNotified ? '已通知' : '—' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="就医建议" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.medicalAdvised ? 'warning' : 'info'">
                    {{ row.medicalAdvised ? '已建议' : '—' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="followUpResult" label="随访/就医结果" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">{{ row.followUpResult || '待反馈' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openResult(row)">记录结果</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-card v-if="detail.case.conclusion" shadow="never" style="margin-top: 16px">
            <template #header>处置结论</template>
            <p>
              确认交叉感染：<b>{{ detail.case.crossInfectionConfirmed ? '是' : '否' }}</b>；
              确认消毒责任：<b>{{ detail.case.disinfectionResponsible ? '是' : '否' }}</b>；
              派单权重下调：<b>-{{ detail.case.weightDeduction || 0 }}</b>
            </p>
            <p class="muted">{{ detail.case.conclusion }}</p>
            <p class="muted">{{ detail.case.concludedByName }} · {{ detail.case.concludedAt }}</p>
          </el-card>
        </template>
      </el-col>
    </el-row>

    <!-- 随访结果 -->
    <el-dialog v-model="resultDialog" title="接触老人就医/随访结果" width="460px">
      <el-form label-width="100px">
        <el-form-item label="随访结果">
          <el-input v-model="resultForm.followUpResult" type="textarea" :rows="3"
                    placeholder="如：已就医，诊断为接触性皮炎，用药后好转 / 无症状" />
        </el-form-item>
        <el-form-item label="是否确诊感染"><el-switch v-model="resultForm.confirmedInfected" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resultDialog = false">取消</el-button>
        <el-button type="primary" @click="doSaveResult">保存</el-button>
      </template>
    </el-dialog>

    <!-- 结论处置 -->
    <el-dialog v-model="concludeDialog" title="确认交叉感染/消毒责任与处置" width="500px">
      <el-alert type="error" :closable="false" show-icon style="margin-bottom: 12px"
        title="确认后：暂停理发师上门资格，安排消毒培训和工具复检，派单权重下调；培训+复检通过后由社区恢复资格。" />
      <el-form label-width="120px">
        <el-form-item label="确认交叉感染"><el-switch v-model="concludeForm.crossInfectionConfirmed" /></el-form-item>
        <el-form-item label="确认消毒责任"><el-switch v-model="concludeForm.disinfectionResponsible" /></el-form-item>
        <el-form-item label="派单权重下调">
          <el-input-number v-model="concludeForm.weightDeduction" :min="0" :max="100" /> 分
        </el-form-item>
        <el-form-item label="结论说明"><el-input v-model="concludeForm.conclusion" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="concludeDialog = false">取消</el-button>
        <el-button type="info" plain @click="doRuleOut">排除（不处置）</el-button>
        <el-button type="danger" @click="doConclude">确认并处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listInfectionCases, infectionCaseDetail, notifyInfectionContacts,
  recordContactResult, concludeInfectionCase
} from '../api'
import { INFECTION_CASE_STATUS_LABELS, INFECTION_CASE_STATUS_TYPES } from '../utils/labels'

const cases = ref([])
const detail = ref(null)
const contacts = ref([])
const loading = ref(false)
const statusFilter = ref('')
const resultDialog = ref(false)
const concludeDialog = ref(false)
const resultForm = reactive({ contactId: null, followUpResult: '', confirmedInfected: false })
const concludeForm = reactive({
  crossInfectionConfirmed: true, disinfectionResponsible: true, weightDeduction: 30, conclusion: ''
})

const loadList = async () => {
  loading.value = true
  try {
    cases.value = await listInfectionCases(statusFilter.value ? { status: statusFilter.value } : {})
    if (cases.value.length) {
      loadDetail(cases.value[0].id)
    } else {
      detail.value = null
    }
  } finally {
    loading.value = false
  }
}

const loadDetail = async (id) => {
  const d = await infectionCaseDetail(id)
  detail.value = d
  contacts.value = d.contacts || []
}

const doNotify = async () => {
  await notifyInfectionContacts(detail.value.case.id)
  ElMessage.success('已通知同工具服务老人的家属和志愿者，并发送就医建议')
  loadDetail(detail.value.case.id)
}

const openResult = (row) => {
  resultForm.contactId = row.id
  resultForm.followUpResult = row.followUpResult || ''
  resultForm.confirmedInfected = row.confirmedInfected
  resultDialog.value = true
}

const doSaveResult = async () => {
  await recordContactResult(resultForm.contactId, {
    followUpResult: resultForm.followUpResult,
    confirmedInfected: resultForm.confirmedInfected
  })
  ElMessage.success('随访结果已记录')
  resultDialog.value = false
  loadDetail(detail.value.case.id)
}

const doConclude = async () => {
  await concludeInfectionCase(detail.value.case.id, { ...concludeForm })
  ElMessage.success('已确认责任并处置：暂停资格、安排培训复检、下调派单权重')
  concludeDialog.value = false
  loadList()
}

const doRuleOut = async () => {
  await concludeInfectionCase(detail.value.case.id, {
    crossInfectionConfirmed: false, disinfectionResponsible: false,
    weightDeduction: 0, conclusion: concludeForm.conclusion || '经排查排除交叉感染与消毒责任'
  })
  ElMessage.success('已排除')
  concludeDialog.value = false
  loadList()
}

onMounted(loadList)
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
