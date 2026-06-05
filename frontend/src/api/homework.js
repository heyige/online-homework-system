import request from '@/utils/request'

export function getHomeworkList(params) {
  return request({
    url: '/homework',
    method: 'get',
    params
  })
}

export function getHomeworkById(id) {
  return request({
    url: `/homework/${id}`,
    method: 'get'
  })
}

export function createHomework(data) {
  return request({
    url: '/homework',
    method: 'post',
    data
  })
}

export function updateHomework(id, data) {
  return request({
    url: `/homework/${id}`,
    method: 'put',
    data
  })
}

export function deleteHomework(id) {
  return request({
    url: `/homework/${id}`,
    method: 'delete'
  })
}

export function getHomeworkByTeacher(params) {
  return request({
    url: '/homework/teacher/all',
    method: 'get',
    params
  })
}

export function getHomeworkByStudent() {
  return request({
    url: '/homework/student/all',
    method: 'get'
  })
}

export function getHomeworkByStatus(status) {
  return request({
    url: `/homework/status/${status}`,
    method: 'get'
  })
}

export function getStudentIdsByHomeworkId(id) {
  return request({
    url: `/homework/${id}/students`,
    method: 'get'
  })
}

export function updateHomeworkStudents(id, studentIds) {
  return request({
    url: `/homework/${id}/students`,
    method: 'put',
    data: studentIds ?? []
  })
}

export function getHomeworkWithSubmissions() {
  return request({
    url: '/homework/teacher/with-submissions',
    method: 'get'
  })
}

export function getExpiredHomework() {
  return request({
    url: '/homework/expired',
    method: 'get'
  })
}
