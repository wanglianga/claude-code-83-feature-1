<template>
  <div>
    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px"
      title="服务包封签、消毒日期、消毒方式、消毒柜编号、责任人扫码可查；备用服务包使用后必须补录消毒，未补录前不得再次派单。" />

    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <span>理发师工具包 / 上门备用服务包</span>
          <el-radio-group v-model="barberFilter" size="small">
            <el-radio-button v-for="b in barbers" :key="b.profile.userId" :value="b.profile.userId">
              {{ b.realName }}
              <el-tag v-if="b.profile.suspended" size="small" type="danger" style="margin-left: 2px">停</el-tag>
            </el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="kits" size="small" v-loading="loading">
        <el-table-column prop="kit.name" label="名称" min-width="160" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.kit.kitType === 'SPARE' ? 'warning' : ''">
              {{ KIT_TYPE_LABELS[row.kit.kitType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="kit.sealCode" label="封签编号" width="170" />
        <el-table-column label="封签" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.kit.sealStatus === 'INTACT' ? 'success' : 'danger'">
              {{ row.kit.sealStatus === 'INTACT' ? '完好' : '破损' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="kit.disinfectedAt" label="消毒日期" width="170" />
        <el-table-column label="消毒方式" width="120">
          <template #default="{ row }">{{ DISINFECTION_METHOD_LABELS[row.kit.disinfectionMethod] || '—' }}</template>
        </el-table-column>
        <el-table-column prop="kit.cabinetNo" label="消毒柜" width="90" />
        <el-table-column prop="kit.responsiblePerson" label="责任人" width="110" />
        <el-table-column label="实时核验" min-width="200">
          <template #default="{ row }">
            <el-tag v-if="!row.checks.length" type="success" size="small">达标可用</el-tag>
            <span v-else class="warn-text">{{ row.checks.join('；') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDisinfect(row.kit)">
              {{ row.kit.kitType === 'SPARE' && row.kit.lastUsedOrderId ? '补录消毒' : '消毒登记' }}
            </el-button>
            <el-button link type="warning" v-if="isStaff" @click="markBroken(row.kit)">封签破损</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 12px">
        <el-button type="primary" plain size="small" @click="spareDialog = true">新建备用服务包</el-button>
      </div>
    </el-card>

    <!-- 理发师资格（暂停/培训/复检/恢复） -->
    <el-card v-if="currentBarber" shadow="never" style="margin-top: 16px">
      <template #header>上门资格与派单权重</template>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="理发师">{{ currentBarber.realName }}</el-descriptions-item>
        <el-descriptions-item label="信用分">{{ currentBarber.profile.creditScore }}</el-descriptions-item>
        <el-descriptions-item label="派单权重">{{ currentBarber.profile.dispatchWeight }}</el-descriptions-item>
        <el-descriptions-item label="上门资格">
          <el-tag size="small" :type="currentBarber.profile.suspended ? 'danger' : 'success'">
            {{ currentBarber.profile.suspended ? '已暂停' : '正常' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="消毒培训">
          <el-tag size="small" :type="currentBarber.profile.disinfectionTrained ? 'success' : 'warning'">
            {{ currentBarber.profile.disinfectionTrained ? '已完成' : '未完成' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="工具复检">
          <el-tag size="small" :type="currentBarber.profile.toolRechecked ? 'success' : 'warning'">
            {{ currentBarber.profile.toolRechecked ? '已通过' : '未通过' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="暂停原因" :span="2">{{ currentBarber.profile.suspendReason || '—' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="isStaff" style="margin-top: 12px">
        <el-button size="small" type="primary" @click="doTraining(currentBarber.profile.userId)">登记消毒培训完成</el-button>
        <el-button size="small" type="warning" @click="doRecheck(currentBarber.profile.userId, true)">工具复检通过</el-button>
        <el-button size="small" type="danger" plain @click="doRecheck(currentBarber.profile.userId, false)">复检不通过</el-button>
        <el-button size="small" type="success" @click="doReinstate(currentBarber.profile.userId)">恢复上门资格</el-button>
      </div>
    </el-card>

    <!-- 消毒记录 -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>消毒 / 补录记录</template>
      <el-table :data="records" size="small">
        <el-table-column prop="disinfectedAt" label="消毒时间" width="170" />
        <el-table-column label="方式" width="130">
          <template #default="{ row }">{{ DISINFECTION_METHOD_LABELS[row.method] }}</template>
        </el-table-column>
        <el-table-column prop="cabinetNo" label="消毒柜" width="90" />
        <el-table-column prop="responsiblePerson" label="责任人" width="110" />
        <el-table-column prop="sealCode" label="封签编号" width="170" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.supplementary ? 'warning' : 'info'">
              {{ row.supplementary ? '使用后补录' : '常规消毒' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="登记人" width="100" />
        <el-table-column prop="note" label="备注" min-width="140" show-overflow-tooltip />
      </el-table>
    </el-card>

    <!-- 消毒登记对话框 -->
    <el-dialog v-model="disinfectDialog" title="消毒登记 / 备用包补录" width="480px">
      <el-form label-width="110px">
        <el-form-item label="服务包">{{ disinfectForm.kitName }}</el-form-item>
        <el-form-item label="消毒方式">
          <el-select v-model="disinfectForm.method" style="width: 100%">
            <el-option v-for="(v, k) in DISINFECTION_METHOD_LABELS" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="消毒柜编号"><el-input v-model="disinfectForm.cabinetNo" placeholder="如 XDG-01" /></el-form-item>
        <el-form-item label="责任人"><el-input v-model="disinfectForm.responsiblePerson" /></el-form-item>
        <el-form-item label="新封签编号"><el-input v-model="disinfectForm.sealCode" placeholder="消毒加封后填写" /></el-form-item>
        <el-form-item label="有效时长(小时)"><el-input-number v-model="disinfectForm.validHours" :min="6" :max="168" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="disinfectForm.note" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disinfectDialog = false">取消</el-button>
        <el-button type="primary" @click="doDisinfect">提交消毒登记</el-button>
      </template>
    </el-dialog>

    <!-- 新建备用包 -->
    <el-dialog v-model="spareDialog" title="新建上门备用服务包" width="440px">
      <el-form label-width="90px">
        <el-form-item label="名称"><el-input v-model="spareForm.name" placeholder="如 上门备用服务包-04" /></el-form-item>
        <el-form-item label="工具清单"><el-input v-model="spareForm.items" type="textarea" :rows="3"
                    placeholder="备用剪刀,一次性围布,一次性毛巾,消毒湿巾" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="spareDialog = false">取消</el-button>
        <el-button type="primary" @click="doCreateSpare">创建（需消毒加封后可用）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listBarbers, listKits, listDisinfections, recordDisinfection, createSpareKit,
  markSealBroken, completeTraining, recheckTools, reinstateBarber
} from '../api'
import { useUserStore } from '../store/user'
import { KIT_TYPE_LABELS, DISINFECTION_METHOD_LABELS } from '../utils/labels'

const store = useUserStore()
const isStaff = computed(() => ['ADMIN', 'STAFF'].includes(store.role))

const barbers = ref([])
const barberFilter = ref(null)
const kits = ref([])
const records = ref([])
const loading = ref(false)
const disinfectDialog = ref(false)
const spareDialog = ref(false)
const disinfectForm = reactive({
  kitId: null, kitName: '', method: 'UV', cabinetNo: 'XDG-01', responsiblePerson: '',
  sealCode: '', validHours: 48, note: '', supplementary: false, orderId: null
})
const spareForm = reactive({ name: '', items: '' })

const currentBarber = computed(() => barbers.value.find((b) => b.profile.userId === barberFilter.value))

const load = async () => {
  loading.value = true
  try {
    barbers.value = await listBarbers()
    if (!barberFilter.value) {
      barberFilter.value = isStaff.value
        ? barbers.value[0]?.profile.userId
        : store.user?.id
    }
    if (barberFilter.value) {
      kits.value = await listKits(barberFilter.value)
      records.value = await listDisinfections({ barberId: barberFilter.value })
    }
  } finally {
    loading.value = false
  }
}

const openDisinfect = (kit) => {
  disinfectForm.kitId = kit.id
  disinfectForm.kitName = kit.name
  disinfectForm.method = kit.disinfectionMethod || 'UV'
  disinfectForm.cabinetNo = kit.cabinetNo || 'XDG-01'
  disinfectForm.responsiblePerson = kit.responsiblePerson || ''
  disinfectForm.sealCode = kit.sealCode || ''
  disinfectForm.validHours = kit.validHours || 48
  disinfectForm.note = ''
  disinfectForm.supplementary = !!(kit.kitType === 'SPARE' && kit.lastUsedOrderId)
  disinfectForm.orderId = kit.lastUsedOrderId
  disinfectDialog.value = true
}

const doDisinfect = async () => {
  await recordDisinfection({ ...disinfectForm })
  ElMessage.success(disinfectForm.supplementary ? '备用包消毒已补录，恢复可派单' : '消毒登记完成')
  disinfectDialog.value = false
  load()
}

const markBroken = async (kit) => {
  await ElMessageBox.confirm(`确认登记「${kit.name}」封签破损？破损后须重新消毒加封才能使用。`, '封签破损', { type: 'warning' })
  await markSealBroken(kit.id)
  ElMessage.success('已登记封签破损，须重新消毒加封')
  load()
}

const doCreateSpare = async () => {
  await createSpareKit(barberFilter.value, { ...spareForm })
  ElMessage.success('备用包已创建，请完成消毒并加封签')
  spareDialog.value = false
  spareForm.name = ''
  spareForm.items = ''
  load()
}

const doTraining = async (userId) => {
  await completeTraining(userId, { note: '社区组织的工具消毒专项培训' })
  ElMessage.success('已登记消毒培训完成')
  load()
}
const doRecheck = async (userId, passed) => {
  await recheckTools(userId, { passed, note: passed ? '工具复检合格' : '仍有污渍/封签问题，需整改' })
  ElMessage.success(passed ? '工具复检通过' : '已登记复检不通过')
  load()
}
const doReinstate = async (userId) => {
  await reinstateBarber(userId)
  ElMessage.success('上门资格已恢复（派单权重维持下调）')
  load()
}

onMounted(load)
</script>

<style scoped>
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.warn-text {
  color: #f56c6c;
  font-size: 12px;
}
</style>
