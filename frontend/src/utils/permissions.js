import { useUserStore } from '@/store/user'

/**
 * 检查用户是否有指定角色
 * @param {string|string[]} roles - 角色或角色数组
 * @returns {boolean} 是否有权限
 */
export const hasRole = (roles) => {
  const userStore = useUserStore()
  const userRole = userStore.role
  
  if (!userRole) return false
  
  // 统一转换为大写进行比较
  const normalizedUserRole = userRole.toUpperCase()
  
  if (Array.isArray(roles)) {
    return roles.some(role => role.toUpperCase() === normalizedUserRole)
  }
  
  return roles.toUpperCase() === normalizedUserRole
}

/**
 * 检查用户是否是管理员
 */
export const isAdmin = () => {
  return hasRole('ADMIN')
}

/**
 * 检查用户是否是教师
 */
export const isTeacher = () => {
  return hasRole('TEACHER')
}

/**
 * 检查用户是否是学生
 */
export const isStudent = () => {
  return hasRole('STUDENT')
}
