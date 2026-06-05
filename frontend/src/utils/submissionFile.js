export const SUBMISSION_ATTACHMENT_ACCEPT =
  '.pdf,.doc,.docx,.zip,.rar,.jpg,.jpeg,.png,.gif,.webp,image/*'

export const SUBMISSION_ATTACHMENT_TIP =
  '支持 PDF、Word、压缩包、图片（JPG/PNG/GIF/WebP），大小不超过 100MB'

const ALLOWED_EXTENSIONS = new Set([
  '.pdf', '.doc', '.docx', '.zip', '.rar',
  '.jpg', '.jpeg', '.png', '.gif', '.webp'
])

const MAX_FILE_SIZE = 100 * 1024 * 1024

export function validateSubmissionAttachment(file) {
  if (!file) {
    return '请选择附件文件'
  }

  if (file.size > MAX_FILE_SIZE) {
    return '附件大小不能超过 100MB'
  }

  const fileName = file.name || ''
  const dotIndex = fileName.lastIndexOf('.')
  if (dotIndex <= 0) {
    return '文件缺少有效扩展名'
  }

  const extension = fileName.slice(dotIndex).toLowerCase()
  if (!ALLOWED_EXTENSIONS.has(extension)) {
    return '仅支持 PDF、Word、压缩包或图片文件'
  }

  return null
}
