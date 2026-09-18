<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="24" color="#fff"><Scissor /></el-icon>
        <span>助老理发平台</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#1f2d3d" text-color="#bfcbd9"
        active-text-color="#ffd04b">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="breadcrumb">{{ $route.meta.title || '工作台' }}</div>
        <div class="right">
          <el-badge :value="unread" :hidden="unread === 0" class="bell">
            <el-icon :size="20" style="cursor: pointer" @click="$router.push('/notifications')"><Bell /></el-icon>
          </el-badge>
          <el-dropdown @command="onCommand">
            <span class="user">
              {{ user?.realName }}
              <el-tag size="small" effect="plain">{{ roleLabel }}</el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { unreadCount } from '../api'
import { ROLE_LABELS } from '../utils/labels'

const store = useUserStore()
const router = useRouter()
const unread = ref(0)

const user = computed(() => store.user)
const roleLabel = computed(() => ROLE_LABELS[store.role] || store.role)

const ALL_MENUS = [
  { path: '/dashboard', title: '工作台', icon: 'Odometer', roles: ['ADMIN', 'STAFF', 'BARBER', 'VOLUNTEER', 'FAMILY', 'FINANCE', 'GRID'] },
  { path: '/elders', title: '老人档案', icon: 'UserFilled', roles: ['ADMIN', 'STAFF'] },
  { path: '/orders/create', title: '新建预约', icon: 'CirclePlus', roles: ['ADMIN', 'STAFF'] },
  { path: '/orders', title: '服务单', icon: 'Tickets', roles: ['ADMIN', 'STAFF', 'BARBER', 'VOLUNTEER', 'FINANCE'] },
  { path: '/barber', title: '理发师工作台', icon: 'Scissor', roles: ['BARBER'] },
  { path: '/volunteer', title: '志愿者工作台', icon: 'HelpFilled', roles: ['VOLUNTEER'] },
  { path: '/family', title: '家属服务', icon: 'HomeFilled', roles: ['FAMILY'] },
  { path: '/exceptions', title: '异常处理', icon: 'WarningFilled', roles: ['ADMIN', 'STAFF', 'FINANCE', 'BARBER', 'VOLUNTEER'] },
  { path: '/tool-disinfection', title: '工具消毒与备用包', icon: 'Box', roles: ['ADMIN', 'STAFF', 'BARBER'] },
  { path: '/infection-trace', title: '交叉感染追溯', icon: 'Aim', roles: ['ADMIN', 'STAFF', 'FINANCE', 'VOLUNTEER', 'GRID'] },
  { path: '/finance', title: '补贴财务', icon: 'Money', roles: ['FINANCE', 'ADMIN', 'STAFF'] },
  { path: '/follow-ups', title: '回访管理', icon: 'PhoneFilled', roles: ['ADMIN', 'STAFF', 'GRID', 'VOLUNTEER'] },
  { path: '/care-tasks', title: '关怀任务', icon: 'FirstAidKit', roles: ['ADMIN', 'STAFF', 'GRID'] },
  { path: '/welfare', title: '公益服务', icon: 'Sunny', roles: ['ADMIN', 'STAFF'] },
  { path: '/stats', title: '复盘统计', icon: 'DataAnalysis', roles: ['ADMIN', 'STAFF', 'FINANCE', 'GRID'] },
  { path: '/users', title: '用户管理', icon: 'Setting', roles: ['ADMIN'] }
]

const menus = computed(() => ALL_MENUS.filter((m) => m.roles.includes(store.role)))

const loadUnread = async () => {
  try {
    unread.value = await unreadCount()
  } catch (e) {
    // ignore
  }
}

onMounted(() => {
  loadUnread()
  setInterval(loadUnread, 30000)
})

const onCommand = (cmd) => {
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background-color: #1f2d3d;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
}
.aside :deep(.el-menu) {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}
.breadcrumb {
  font-size: 16px;
  font-weight: 600;
}
.right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.user {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
}
.main {
  background: #f0f2f5;
  overflow-y: auto;
}
</style>
