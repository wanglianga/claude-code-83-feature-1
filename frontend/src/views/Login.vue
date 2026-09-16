<template>
  <div class="login-page">
    <el-card class="login-card">
      <div class="title">
        <el-icon :size="32" color="#409eff"><Scissor /></el-icon>
        <h2>社区助老理发预约上门<br />与公益补贴平台</h2>
      </div>
      <el-form :model="form" @keyup.enter="doLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock"
            size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="doLogin">
          登 录
        </el-button>
      </el-form>
      <el-divider>演示账号</el-divider>
      <div class="demo-accounts">
        <el-tag v-for="a in demoAccounts" :key="a.u" size="small" style="cursor: pointer; margin: 2px"
          @click="fill(a)">{{ a.label }}</el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login } from '../api'
import { useUserStore } from '../store/user'

const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const demoAccounts = [
  { label: '管理员', u: 'admin', p: 'admin123' },
  { label: '社区', u: 'staff', p: 'staff123' },
  { label: '理发师', u: 'barber1', p: 'barber123' },
  { label: '志愿者', u: 'volunteer1', p: 'volunteer123' },
  { label: '家属', u: 'family', p: 'family123' },
  { label: '财务', u: 'finance', p: 'finance123' },
  { label: '网格员', u: 'grid', p: 'grid123' }
]

const fill = (a) => {
  form.username = a.u
  form.password = a.p
}

const doLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await login(form)
    store.setLogin(data.token, data.user)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  border-radius: 12px;
}
.title {
  text-align: center;
  margin-bottom: 24px;
}
.title h2 {
  font-size: 18px;
  margin-top: 8px;
  line-height: 1.5;
}
.demo-accounts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
}
</style>
