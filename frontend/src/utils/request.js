import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

// 从 cookie 中获取 token
const getToken = () => {
  const cookieValue = document.cookie
    .split('; ') 
    .find(row => row.startsWith('token='))
    ?.split('=')[1]
  return cookieValue ? decodeURIComponent(cookieValue) : ''
}

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(
  config => {
    const token = getToken()
    // 为公开接口跳过添加 Authorization 头
    const publicPaths = [
      '/auth/login',
      '/auth/register',
      '/users/verification-code',
      '/users/verify-code',
      '/users/reset-password',
      '/auth/forgot-password/send-code',
      '/auth/forgot-password/verify-code',
      '/auth/forgot-password/reset'
    ]
    
    if (token && !publicPaths.some(path => config.url.includes(path))) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    const res = response.data
    
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logoutAction()
        router.push('/login')
      }
      
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    
    return res
  },
  error => {
    console.error('响应错误:', error)
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 401:
          ElMessage.error('未授权，请重新登录')
          const userStore = useUserStore()
          userStore.logoutAction()
          router.push('/login')
          break
        case 403:
          ElMessage.error('拒绝访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error(data?.message || '服务器错误')
          break
        default:
          ElMessage.error(data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    
    return Promise.reject(error)
  }
)

export default request
