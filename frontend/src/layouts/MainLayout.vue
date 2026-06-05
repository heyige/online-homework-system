<template>
  <el-container class="layout-container">
    <el-aside :width="appStore.sidebarOpened ? '200px' : '64px'" class="sidebar">
      <div class="logo">
        <el-icon v-if="!appStore.sidebarOpened" size="24"><Odometer /></el-icon>
        <span v-if="appStore.sidebarOpened">在线作业管理系统</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        :collapse="!appStore.sidebarOpened"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>控制台</span>
        </el-menu-item>
        
        <el-menu-item v-if="userStore.role === 'TEACHER' || userStore.role === 'STUDENT'" index="/homework">
          <el-icon><Document /></el-icon>
          <span>作业列表</span>
        </el-menu-item>
        
        <el-menu-item v-if="userStore.role === 'TEACHER'" index="/grade">
          <el-icon><EditPen /></el-icon>
          <span>作业批改</span>
        </el-menu-item>
        
        <el-menu-item index="/messages">
          <el-icon><Bell /></el-icon>
          <span>消息通知</span>
        </el-menu-item>
        
        <el-menu-item v-if="userStore.role === 'ADMIN'" index="/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        
        <el-menu-item v-if="userStore.role === 'ADMIN'" index="/profile-requests">
          <el-icon><DocumentCopy /></el-icon>
          <span>申请审批</span>
        </el-menu-item>
        
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="hamburger" @click="toggleSidebar">
            <Fold v-if="appStore.sidebarOpened" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0">
            <el-icon class="notification-icon" @click="goToMessages">
              <Bell />
            </el-icon>
          </el-badge>
          
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="resolveAvatarUrl(userStore.user?.avatar)" />
              <span class="username">{{ userStore.user?.name || userStore.user?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { getUnreadMessages } from '@/api/message'
import { hasRole } from '@/utils/permissions'
import { resolveAvatarUrl } from '@/utils/avatar'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const unreadCount = ref(0)
const messageTimer = ref(null)

const activeMenu = computed(() => route.path)

const toggleSidebar = () => {
  appStore.toggleSidebar()
}

const goToMessages = () => {
  router.push('/messages')
}

const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'password':
      router.push('/change-password')
      break
    case 'logout':
      // 清除定时器
      if (messageTimer.value) {
        clearInterval(messageTimer.value)
        messageTimer.value = null
      }
      await userStore.logoutAction()
      router.push('/login')
      break
  }
}

const loadUnreadMessages = async () => {
  try {
    const res = await getUnreadMessages()
    unreadCount.value = res.data?.length || 0
  } catch (e) {
    console.error('加载未读消息失败:', e)
  }
}

// 将方法暴露到window对象，供其他组件调用
window.loadUnreadMessages = loadUnreadMessages

onMounted(() => {
  if (userStore.token) {
    userStore.getUserInfo().catch(() => {})
    loadUnreadMessages()
    messageTimer.value = setInterval(loadUnreadMessages, 60000)
    // 暴露定时器到window对象，供user store使用
    window.messageTimer = messageTimer.value
  }
})

onUnmounted(() => {
  // 组件卸载时清除定时器
  if (messageTimer.value) {
    clearInterval(messageTimer.value)
    messageTimer.value = null
  }
})
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow-x: hidden;
  
  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #2b3a4b;
    
    img {
      height: 32px;
      margin-right: 10px;
    }
    
    span {
      color: #fff;
      font-size: 16px;
      font-weight: bold;
      white-space: nowrap;
    }
  }
  
  :deep(.el-menu) {
    border-right: none;
  }
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  
  .header-left {
    display: flex;
    align-items: center;
    
    .hamburger {
      font-size: 20px;
      cursor: pointer;
      margin-right: 15px;
      
      &:hover {
        color: #409EFF;
      }
    }
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 20px;
    
    .notification-icon {
      font-size: 20px;
      cursor: pointer;
      
      &:hover {
        color: #409EFF;
      }
    }
    
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      
      .username {
        font-size: 14px;
      }
    }
  }
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>
