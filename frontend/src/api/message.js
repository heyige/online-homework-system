import request from '@/utils/request'

export function getMessages(params) {
  return request({
    url: '/messages',
    method: 'get',
    params
  })
}

export function getUnreadMessages() {
  return request({
    url: '/messages/unread',
    method: 'get'
  })
}

export function getMessagesByReceiver(userId, limit = 10) {
  return request({
    url: `/messages/receiver/${userId}`,
    method: 'get',
    params: { limit }
  })
}

export function getMessageById(id) {
  return request({
    url: `/messages/${id}`,
    method: 'get'
  })
}

export function markMessageAsRead(id) {
  return request({
    url: `/messages/${id}/read`,
    method: 'put'
  })
}

export function deleteMessage(id) {
  return request({
    url: `/messages/${id}`,
    method: 'delete'
  })
}

export function createMessage(data) {
  return request({
    url: '/messages',
    method: 'post',
    data
  })
}

export function updateMessage(id, data) {
  return request({
    url: `/messages/${id}`,
    method: 'put',
    data
  })
}

export function deleteMessagesByNotificationId(notificationId) {
  return request({
    url: `/messages/notification/${notificationId}`,
    method: 'delete'
  })
}
