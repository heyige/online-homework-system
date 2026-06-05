import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, register, logout, getCurrentUser } from '@/api/user'
import Cookies from 'js-cookie'

export const useUserStore = defineStore('user', () => {
  const token = ref(Cookies.get('token') || '')
  const userInfoStr = Cookies.get('userInfo')
  let userInfo = null
  if (userInfoStr) {
    try {
      userInfo = JSON.parse(userInfoStr)
      // 将角色转换为大写，以匹配后端的角色命名
      if (userInfo && userInfo.role) {
        userInfo.role = userInfo.role.toUpperCase()
      }
    } catch (e) {
      console.error('解析用户信息失败:', e)
      Cookies.remove('userInfo')
      Cookies.remove('token')
    }
  }
  const user = ref(userInfo)
  const role = ref(userInfo?.role || '')
  
  const loginAction = async (loginForm) => {
    const res = await login(loginForm)
    token.value = res.data.token
    // 确保角色是大写的
    const userData = { ...res.data, role: res.data.role?.toUpperCase() }
    user.value = userData
    role.value = userData.role
    
    Cookies.set('token', res.data.token)
    Cookies.set('userInfo', JSON.stringify(userData))
    
    return res
  }
  
  const registerAction = async (registerForm) => {
    return await register(registerForm)
  }
  
  const getUserInfo = async () => {
    const res = await getCurrentUser()
    // 确保角色是大写的，并统一 id 字段
    const userData = {
      ...res.data,
      role: res.data.role?.toUpperCase(),
      userId: res.data.id ?? res.data.userId
    }
    user.value = userData
    role.value = userData.role
    Cookies.set('userInfo', JSON.stringify(userData))
    return res
  }
  
  const logoutAction = async () => {
    try {
      await logout()
    } catch (e) {
      console.error('登出失败:', e)
    } finally {
      token.value = ''
      user.value = null
      role.value = ''
      Cookies.remove('token')
      Cookies.remove('userInfo')
      localStorage.removeItem('rememberedPassword')
      // 清除消息定时器
      if (window.messageTimer) {
        clearInterval(window.messageTimer)
        window.messageTimer = null
      }
    }
  }
  
  const resetToken = () => {
    token.value = ''
    user.value = null
    role.value = ''
    Cookies.remove('token')
    Cookies.remove('userInfo')
  }
  
  return {
    token,
    user,
    role,
    loginAction,
    registerAction,
    getUserInfo,
    logoutAction,
    resetToken
  }
})
