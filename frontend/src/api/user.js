import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export function refreshToken(refreshToken) {
  return request({
    url: '/auth/refresh',
    method: 'post',
    data: { refreshToken }
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function getCurrentUser() {
  return request({
    url: '/users/me',
    method: 'get'
  })
}

export function getUserById(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

export function getAllUsers(params) {
  return request({
    url: '/users',
    method: 'get',
    params
  })
}

export function createUser(data) {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

export function getUsersByRole(role) {
  return request({
    url: `/users/role/${role}`,
    method: 'get'
  })
}

export function updateUser(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}

export function changePassword(data) {
  return request({
    url: '/users/change-password',
    method: 'put',
    data
  })
}

export function generateVerificationCode(identifier) {
  return request({
    url: `/auth/forgot-password/send-code`,
    method: 'post',
    data: { identifier }
  })
}

export function verifyCode(identifier, code) {
  return request({
    url: `/auth/forgot-password/verify-code`,
    method: 'post',
    data: { identifier, code }
  })
}

export function resetPassword(identifier, code, newPassword) {
  return request({
    url: `/auth/forgot-password/reset`,
    method: 'post',
    data: { identifier, newPassword }
  })
}
