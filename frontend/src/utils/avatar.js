import request from '@/utils/request'

/**
 * 解析头像地址，兼容 Base64、相对路径与完整 URL。
 */
export function resolveAvatarUrl(avatar, fallback = '/avatars/default.svg') {
  if (!avatar) return fallback
  if (avatar.startsWith('data:') || avatar.startsWith('http://') || avatar.startsWith('https://')) {
    return avatar
  }
  if (avatar.startsWith('/public/')) {
    return `/api${avatar}`
  }
  if (avatar.startsWith('/api/')) {
    return avatar
  }
  return avatar
}

function dataUrlToBlob(dataUrl) {
  const [header, base64] = dataUrl.split(',')
  const mime = header.match(/:(.*?);/)[1]
  const binary = atob(base64)
  const array = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i += 1) {
    array[i] = binary.charCodeAt(i)
  }
  return new Blob([array], { type: mime })
}

/**
 * 将头像图片压缩为 Base64。
 */
export function compressAvatarToDataUrl(file, maxSize = 256, quality = 0.85) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error('读取头像失败'))
    reader.onload = () => {
      const img = new Image()
      img.onerror = () => reject(new Error('图片格式无效'))
      img.onload = () => {
        let { width, height } = img
        if (width > maxSize || height > maxSize) {
          const ratio = Math.min(maxSize / width, maxSize / height)
          width = Math.round(width * ratio)
          height = Math.round(height * ratio)
        }

        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, width, height)

        const mimeType = file.type === 'image/png' ? 'image/png' : 'image/jpeg'
        resolve(canvas.toDataURL(mimeType, quality))
      }
      img.src = reader.result
    }
    reader.readAsDataURL(file)
  })
}

/**
 * 压缩并上传头像，返回后端存储的 URL 路径。
 */
export async function prepareAndUploadAvatar(file) {
  const dataUrl = await compressAvatarToDataUrl(file)
  const blob = dataUrlToBlob(dataUrl)
  const extension = file.type === 'image/png' ? 'png' : 'jpg'
  const formData = new FormData()
  formData.append('file', blob, `avatar.${extension}`)

  const res = await request({
    url: '/users/me/avatar',
    method: 'post',
    data: formData
  })

  return res.data
}
