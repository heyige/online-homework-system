import request from '@/utils/request'

export function submitHomework(homeworkId, content, file) {
  const formData = new FormData()
  formData.append('homeworkId', homeworkId)
  if (content) formData.append('content', content)
  if (file) formData.append('file', file)
  
  return request({
    url: '/submissions',
    method: 'post',
    data: formData
  })
}

export function getSubmissionById(id) {
  return request({
    url: `/submissions/${id}`,
    method: 'get'
  })
}

export function getSubmissionsByStudent() {
  return request({
    url: '/submissions/student/all',
    method: 'get'
  })
}

export function getAllSubmissionsForTeacher() {
  return request({
    url: '/submissions/teacher/all',
    method: 'get'
  })
}

export function getSubmissionByHomeworkForStudent(homeworkId) {
  return request({
    url: `/submissions/homework/${homeworkId}/student`,
    method: 'get'
  })
}

export function getSubmissionsByHomework(homeworkId) {
  return request({
    url: `/submissions/homework/${homeworkId}`,
    method: 'get'
  })
}

export function gradeSubmission(id, score, feedback) {
  return request({
    url: `/submissions/${id}/grade?score=${score}&feedback=${encodeURIComponent(feedback || '')}`,
    method: 'put'
  })
}

export function updateSubmission(id, content, file, deleteFile = false) {
  const formData = new FormData()
  if (content !== undefined && content !== null) formData.append('content', content)
  if (file) formData.append('file', file)
  if (deleteFile) formData.append('deleteFile', 'true')
  
  return request({
    url: `/submissions/${id}`,
    method: 'put',
    data: formData
  })
}

export function deleteSubmission(id) {
  return request({
    url: `/submissions/${id}`,
    method: 'delete'
  })
}

export function checkPlagiarism(homeworkId) {
  return request({
    url: `/submissions/homework/${homeworkId}/plagiarism`,
    method: 'post'
  })
}

export function getHomeworkStatistics(homeworkId) {
  return request({
    url: `/submissions/homework/${homeworkId}/statistics`,
    method: 'get'
  })
}

function getTokenFromCookie() {
  const cookieValue = document.cookie
    .split('; ')
    .find(row => row.startsWith('token='))
    ?.split('=')[1]
  return cookieValue ? decodeURIComponent(cookieValue) : ''
}

export function downloadAttachment(submissionId) {
  const token = getTokenFromCookie()
  const url = `/api/submissions/attachment/${submissionId}`

  const xhr = new XMLHttpRequest()
  xhr.open('GET', url, true)
  if (token) {
    xhr.setRequestHeader('Authorization', `Bearer ${token}`)
  }
  xhr.responseType = 'blob'

  xhr.onload = function () {
    if (xhr.status !== 200) {
      return
    }

    const contentType = xhr.getResponseHeader('Content-Type') || 'application/octet-stream'
    const blob = new Blob([xhr.response], { type: contentType })
    const blobUrl = window.URL.createObjectURL(blob)

    let fileName = 'attachment'
    const contentDisposition = xhr.getResponseHeader('Content-Disposition')

    if (contentDisposition) {
      let filenameMatch = contentDisposition.match(/filename="([^"]+)"/)
      if (!filenameMatch) {
        filenameMatch = contentDisposition.match(/filename=([^;]+)/)
      }

      if (filenameMatch && filenameMatch[1]) {
        fileName = filenameMatch[1].trim()
        if (fileName.startsWith('"') && fileName.endsWith('"')) {
          fileName = fileName.substring(1, fileName.length - 1)
        }
        try {
          fileName = fileName.replace(/\+/g, ' ')
          fileName = decodeURIComponent(fileName)
        } catch (e) {
          console.error('Failed to decode filename:', e)
        }
      }
    }

    if (!fileName.includes('.')) {
      const extensionMap = {
        'application/pdf': '.pdf',
        'application/msword': '.doc',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document': '.docx',
        'application/zip': '.zip',
        'application/x-rar-compressed': '.rar',
        'image/jpeg': '.jpg',
        'image/png': '.png',
        'image/gif': '.gif',
        'image/webp': '.webp',
        'text/plain': '.txt',
        'application/json': '.json'
      }
      fileName += extensionMap[contentType] || '.bin'
    }

    const a = document.createElement('a')
    a.href = blobUrl
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(blobUrl)
    document.body.removeChild(a)
  }

  xhr.onerror = function () {
    console.error('下载附件失败')
  }

  xhr.send()
}
