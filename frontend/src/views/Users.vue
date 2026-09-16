<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建用户</el-button>
    </div>
    <el-table :data="list" size="small" v-loading="loading">
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ ROLE_LABELS[row.role] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link :type="row.status === 'ACTIVE' ? 'danger' : 'success'" @click="toggle(row)">
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
          <el-button link type="primary" @click="resetPwd(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建用户" width="420px">
      <el-form label-width="90px">
        <el-form-item label="用户名" required><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码" required><el-input v-model="form.password" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option v-for="(v, k) in ROLE_LABELS" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, createUser, changeUserStatus, resetPassword } from '../api'
import { ROLE_LABELS } from '../utils/labels'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = reactive({ username: '', password: '', realName: '', phone: '', role: 'BARBER' })

const load = async () => {
  loading.value = true
  try {
    list.value = await listUsers()
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  Object.assign(form, { username: '', password: '', realName: '', phone: '', role: 'BARBER' })
  dialogVisible.value = true
}

const doCreate = async () => {
  if (!form.username || !form.password || !form.realName) {
    ElMessage.warning('请填写完整信息')
    return
  }
  await createUser(form)
  ElMessage.success('创建成功')
  dialogVisible.value = false
  load()
}

const toggle = async (row) => {
  const target = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await changeUserStatus(row.id, target)
  ElMessage.success('已更新')
  load()
}

const resetPwd = async (row) => {
  const { value } = await ElMessageBox.prompt('请输入新密码', `重置密码 - ${row.realName}`, {
    inputValue: '123456'
  })
  if (value) {
    await resetPassword(row.id, { password: value })
    ElMessage.success('密码已重置')
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>
