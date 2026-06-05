import request from '@/utils/request'

export function getMyProfileRequests() {
  return request({
    url: '/profile-requests/my',
    method: 'get'
  })
}

export function getAllProfileRequests() {
  return request({
    url: '/profile-requests',
    method: 'get'
  })
}

export function getPendingProfileRequests() {
  return request({
    url: '/profile-requests/pending',
    method: 'get'
  })
}

export function getProfileRequestById(id) {
  return request({
    url: `/profile-requests/${id}`,
    method: 'get'
  })
}

export function createProfileRequest(data) {
  return request({
    url: '/profile-requests',
    method: 'post',
    data
  })
}

export function approveProfileRequest(id) {
  return request({
    url: `/profile-requests/${id}/approve`,
    method: 'put'
  })
}

export function rejectProfileRequest(id, reason) {
  return request({
    url: `/profile-requests/${id}/reject?reason=${encodeURIComponent(reason)}`,
    method: 'put'
  })
}