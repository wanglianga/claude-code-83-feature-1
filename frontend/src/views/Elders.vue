<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按姓名搜索" clearable style="width: 220px" @change="load" />
        <el-button type="primary" :icon="Plus" @click="openEdit(null)">录入老人</el-button>
      </div>
      <el-table :data="list" v-loading="loading" size="small">
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column label="年龄" width="60">
          <template #default="{ row }">{{ age(row.birthDate) }}</template>
        </el-table-column>
        <el-table-column prop="address" label="住址" min-width="180" show-overflow-tooltip />
        <el-table-column label="风险" width="80">
          <template #default="{ row }">
            <el-tag :type="RISK_TYPES[row.riskLevel]" size="small">{{ RISK_LABELS[row.riskLevel] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="独居" width="60">
          <template #default="{ row }">
            <el-tag v-if="row.livingAlone" type="danger" size="small">独居</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="healthCondition" label="身体状况" min-width="130" show-overflow-tooltip />
        <el-table-column label="家属在场" width="80">
          <template #default="{ row }">{{ row.needFamilyPresent ? '需要' : '—' }}</template>
        </el-table-column>
        <el-table-column label="补贴资格" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.subsidyType === 'NONE' ? 'info' : 'success'">
              {{ SUBSIDY_LABELS[row.subsidyType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '在册' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="goCreateOrder(row)">预约</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑老人档案' : '录入老人档案'" width="720px" top="4vh">
      <el-form :model="form" label-width="110px" :inline="false">
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="性别">
              <el-select v-model="form.gender"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出生日期">
              <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="所属社区"><el-input v-model="form.community" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="楼栋"><el-input v-model="form.building" placeholder="如 3栋" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="单元"><el-input v-model="form.unit" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="房号"><el-input v-model="form.room" /></el-form-item></el-col>
          <el-col :span="6">
            <el-form-item label="楼栋距离(米)"><el-input-number v-model="form.buildingDistance" :min="0" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="完整住址"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="风险等级">
              <el-select v-model="form.riskLevel">
                <el-option v-for="(v, k) in RISK_LABELS" :key="k" :label="v" :value="k" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="行动能力">
              <el-select v-model="form.mobility">
                <el-option v-for="m in ['自如', '拄拐', '轮椅', '卧床']" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="补贴资格">
              <el-select v-model="form.subsidyType">
                <el-option v-for="(v, k) in SUBSIDY_LABELS" :key="k" :label="v" :value="k" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="独居老人"><el-switch v-model="form.livingAlone" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="需家属在场"><el-switch v-model="form.needFamilyPresent" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="纳入巡访"><el-switch v-model="form.patrolEnabled" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="身体状况"><el-input v-model="form.healthCondition" type="textarea" :rows="2" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="理发偏好"><el-input v-model="form.hairPreference" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="上门时间偏好"><el-input v-model="form.preferredTime" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="家属姓名"><el-input v-model="form.familyContactName" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="家属电话"><el-input v-model="form.familyContactPhone" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="巡访间隔(天)"><el-input-number v-model="form.patrolIntervalDays" :min="7" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.notes" type="textarea" :rows="2" /></el-form-item></el-col>
          <el-col :span="8" v-if="form.id">
            <el-form-item label="档案状态">
              <el-select v-model="form.status">
                <el-option label="在册" value="ACTIVE" /><el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="`老人档案 - ${current?.name}`" size="520px">
      <template v-if="current">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="姓名">{{ current.name }}</el-descriptions-item>
          <el-descriptions-item label="年龄">{{ age(current.birthDate) }} 岁</el-descriptions-item>
          <el-descriptions-item label="住址" :span="2">{{ current.address }}</el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="RISK_TYPES[current.riskLevel]" size="small">{{ RISK_LABELS[current.riskLevel] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="独居">{{ current.livingAlone ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="身体状况" :span="2">{{ current.healthCondition }}</el-descriptions-item>
          <el-descriptions-item label="理发偏好" :span="2">{{ current.hairPreference }}</el-descriptions-item>
          <el-descriptions-item label="需家属在场">{{ current.needFamilyPresent ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="补贴资格">{{ SUBSIDY_LABELS[current.subsidyType] }}</el-descriptions-item>
          <el-descriptions-item label="家属">{{ current.familyContactName }} {{ current.familyContactPhone }}</el-descriptions-item>
          <el-descriptions-item label="楼栋距离">{{ current.buildingDistance }} 米</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 16px 0 8px">服务历史</h4>
        <el-table :data="history" size="small" empty-text="暂无服务记录">
          <el-table-column prop="orderNo" label="单号" width="140" />
          <el-table-column prop="scheduledDate" label="日期" width="100" />
          <el-table-column label="状态">
            <template #default="{ row }">
              <el-tag size="small" :type="ORDER_STATUS_TYPES[row.status]">{{ ORDER_STATUS_LABELS[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="$router.push(`/orders/${row.id}`)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { listElders, createElder, updateElder, elderOrders } from '../api'
import { RISK_LABELS, RISK_TYPES, SUBSIDY_LABELS, ORDER_STATUS_LABELS, ORDER_STATUS_TYPES } from '../utils/labels'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const keyword = ref('')
const editVisible = ref(false)
const detailVisible = ref(false)
const saving = ref(false)
const current = ref(null)
const history = ref([])

const emptyForm = () => ({
  id: null, name: '', gender: '男', birthDate: '', idCard: '', phone: '', community: '阳光社区',
  building: '', unit: '', room: '', address: '', buildingDistance: 0, livingAlone: false,
  healthCondition: '', riskLevel: 'LOW', mobility: '自如', needFamilyPresent: false,
  hairPreference: '', preferredTime: '', subsidyType: 'NONE', familyContactName: '',
  familyContactPhone: '', familyUserId: null, patrolEnabled: false, patrolIntervalDays: 30,
  notes: '', status: 'ACTIVE'
})
const form = ref(emptyForm())

const age = (birth) => (birth ? dayjs().diff(dayjs(birth), 'year') : '-')

const load = async () => {
  loading.value = true
  try {
    list.value = await listElders({ keyword: keyword.value || undefined })
  } finally {
    loading.value = false
  }
}

const openEdit = (row) => {
  form.value = row ? { ...row } : emptyForm()
  editVisible.value = true
}

const save = async () => {
  if (!form.value.name) {
    ElMessage.warning('请填写姓名')
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await updateElder(form.value.id, form.value)
    } else {
      await createElder(form.value)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const openDetail = async (row) => {
  current.value = row
  detailVisible.value = true
  history.value = await elderOrders(row.id)
}

const goCreateOrder = (row) => {
  router.push({ path: '/orders/create', query: { elderId: row.id } })
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
