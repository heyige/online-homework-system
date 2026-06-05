<template>
  <div class="page-container">
    <div class="profile-page">
      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <h2>个人信息</h2>
            <el-button type="primary" @click="showEditDialog">
              <el-icon><Edit /></el-icon> 修改个人信息
            </el-button>
          </div>
        </template>
        
        <div class="profile-content">
          <div class="avatar-section">
            <el-avatar :size="120" :src="resolveAvatarUrl(userInfo.avatar)" class="avatar" />
          </div>
          
          <div class="info-list">
            <div class="info-item">
              <label>用户名：</label>
              <span>{{ userInfo.username }}</span>
            </div>
            <div class="info-item">
              <label>姓名：</label>
              <span>{{ userInfo.name }}</span>
            </div>
            <div class="info-item">
              <label>角色：</label>
              <span>{{ getRoleLabel(userInfo.role) }}</span>
            </div>
            <div class="info-item">
              <label>邮箱：</label>
              <span>{{ userInfo.email || '未设置' }}</span>
            </div>
            <div class="info-item">
              <label>手机号：</label>
              <span>{{ userInfo.phone || '未设置' }}</span>
            </div>
            <div class="info-item">
              <label>学院：</label>
              <span>{{ userInfo.department || '未设置' }}</span>
            </div>
            <div class="info-item">
              <label>专业：</label>
              <span>{{ userInfo.major || '未设置' }}</span>
            </div>
            <div class="info-item" v-if="userInfo.studentId">
              <label>学号：</label>
              <span>{{ userInfo.studentId }}</span>
            </div>
            <div class="info-item">
              <label>创建时间：</label>
              <span>{{ formatDate(userInfo.createdAt) }}</span>
            </div>
          </div>
          
          <div v-if="!userInfo.email && !userInfo.phone && (!userInfo.role || userInfo.role.toUpperCase() !== 'ADMIN')" class="message-alert warning">
            <el-icon><Warning /></el-icon>
            <span>您的账户未设置邮箱或手机号，无法使用密码找回功能。建议您在个人信息中添加联系方式。</span>
          </div>
        </div>
      </el-card>
      
      <el-card class="password-card">
        <template #header>
          <h2>修改密码</h2>
        </template>
        
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="120px"
        >
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入旧密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-card>
      
      <el-dialog
        v-model="editDialogVisible"
        title="修改个人信息"
        class="custom-dialog"
        width="600px"
      >
        <el-form
          ref="editFormRef"
          :model="editForm"
          :rules="editRules"
          label-width="100px"
        >
          <el-form-item label="头像">
            <div class="avatar-upload">
              <el-avatar :size="80" :src="resolveAvatarUrl(editForm.avatar)" />
              <div class="upload-buttons">
                <el-upload
                  ref="avatarUploadRef"
                  :auto-upload="false"
                  :limit="1"
                  :on-change="handleAvatarChange"
                  :on-exceed="handleAvatarExceed"
                  accept="image/jpeg,image/png"
                  class="avatar-uploader"
                  :show-file-list="false"
                >
                  <el-button type="primary" size="small">选择头像</el-button>
                </el-upload>
                <el-button 
                  type="danger" 
                  size="small" 
                  @click="deleteAvatar"
                  v-if="editForm.avatar"
                >
                  删除头像
                </el-button>
                <div class="el-upload__tip" style="margin-top: 10px;">支持 JPG、PNG，不超过 2MB</div>
              </div>
            </div>
          </el-form-item>
          
          <el-form-item label="用户名">
            <el-input v-model="editForm.username" disabled />
          </el-form-item>
          
          <el-form-item label="姓名" prop="name">
            <el-input v-model="editForm.name" placeholder="请输入姓名" />
          </el-form-item>
          
          <el-form-item label="角色">
            <span>{{ getRoleLabel(editForm.role) }}</span>
          </el-form-item>
          
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="editForm.email" placeholder="请输入邮箱（选填）" />
          </el-form-item>
          
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="editForm.phone" placeholder="请输入手机号（选填）" />
          </el-form-item>
          
          <el-form-item label="学院">
            <el-select
              v-model="editForm.department"
              placeholder="请选择学院"
              clearable
              style="width: 100%"
              @change="handleDepartmentChange"
            >
              <el-option
                v-for="item in DEPARTMENT_OPTIONS"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="专业">
            <el-select
              v-model="editForm.major"
              placeholder="请选择专业"
              clearable
              style="width: 100%"
              @change="handleMajorChange"
            >
              <el-option
                v-for="item in availableMajors"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="学号" v-if="editForm.role && editForm.role.toUpperCase() === 'STUDENT'">
            <el-input v-model="editForm.studentId" disabled />
          </el-form-item>
        </el-form>
        
        <template #footer>
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitEdit" :loading="avatarUploading">提交修改申请</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { changePassword } from '@/api/user'
import { createProfileRequest } from '@/api/profileRequest'
import { prepareAndUploadAvatar, resolveAvatarUrl } from '@/utils/avatar'
import {
  DEPARTMENT_OPTIONS,
  getMajorsByDepartment,
  onDepartmentChange,
  onMajorChange
} from '@/utils/departmentMajor'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Warning } from '@element-plus/icons-vue'

const userStore = useUserStore()

const userInfo = ref({})
const editDialogVisible = ref(false)
const passwordFormRef = ref(null)
const editFormRef = ref(null)
const avatarUploadRef = ref(null)
const selectedAvatar = ref(null)
const avatarChanged = ref(false)
const avatarUploading = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const editForm = reactive({
  username: '',
  name: '',
  role: '',
  email: '',
  phone: '',
  department: '',
  major: '',
  studentId: '',
  avatar: ''
})

const availableMajors = computed(() => getMajorsByDepartment(editForm.department))

const handleDepartmentChange = (value) => {
  onDepartmentChange(editForm, value)
}

const handleMajorChange = (value) => {
  onMajorChange(editForm, value)
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const editRules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  email: [
    {
      validator: (rule, value, callback) => {
        if (!value || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
          callback()
        } else {
          callback(new Error('请输入正确的邮箱地址'))
        }
      },
      trigger: 'blur'
    }
  ],
  phone: [
    {
      validator: (rule, value, callback) => {
        if (!value || /^1[3-9]\d{9}$/.test(value)) {
          callback()
        } else {
          callback(new Error('请输入正确的手机号'))
        }
      },
      trigger: 'blur'
    }
  ]
}

const getRoleLabel = (role) => {
  const roleMap = {
    'ADMIN': '管理员',
    'TEACHER': '教师',
    'STUDENT': '学生'
  }
  const upperRole = (role || '').toUpperCase()
  return roleMap[upperRole] || role
}

const getRoleType = (role) => {
  const typeMap = {
    'ADMIN': 'danger',
    'TEACHER': 'warning',
    'STUDENT': 'success'
  }
  const upperRole = (role || '').toUpperCase()
  return typeMap[upperRole] || 'info'
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const loadUserInfo = async () => {
  try {
    const res = await userStore.getUserInfo()
    userInfo.value = res.data
    Object.assign(editForm, res.data)
  } catch (e) {
    console.error('加载用户信息失败:', e)
    ElMessage.error('加载用户信息失败')
  }
}

const showEditDialog = () => {
  Object.assign(editForm, userInfo.value)
  avatarChanged.value = false
  selectedAvatar.value = null
  avatarUploading.value = false
  if (avatarUploadRef.value) {
    avatarUploadRef.value.clearFiles()
  }
  editDialogVisible.value = true
}

const handleAvatarExceed = (files) => {
  if (avatarUploadRef.value) {
    avatarUploadRef.value.clearFiles()
  }
  handleAvatarChange({ raw: files[0] })
}

const handleAvatarChange = async (file) => {
  const rawFile = file.raw || file
  const validTypes = ['image/jpeg', 'image/png']
  if (!validTypes.includes(rawFile.type)) {
    ElMessage.error('只支持 JPG、PNG 格式的头像')
    return
  }
  if (rawFile.size > 2 * 1024 * 1024) {
    ElMessage.error('头像文件大小不能超过 2MB')
    return
  }

  avatarUploading.value = true
  try {
    selectedAvatar.value = rawFile
    editForm.avatar = await prepareAndUploadAvatar(rawFile)
    avatarChanged.value = true
    ElMessage.success('头像已更新，提交申请后生效')
  } catch (e) {
    console.error('头像上传失败:', e)
    ElMessage.error(e.message || '头像上传失败，请重试')
  } finally {
    avatarUploading.value = false
  }
}

const deleteAvatar = () => {
  ElMessageBox.confirm(
    '确定要删除头像吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    editForm.avatar = ''
    avatarChanged.value = true
    selectedAvatar.value = null
    // 清空 Upload 组件的文件列表，解决第二次上传失败的问题
    if (avatarUploadRef.value) {
      avatarUploadRef.value.clearFiles()
    }
    ElMessage.success('头像已删除')
  }).catch(() => {})
}

const handleSubmitEdit = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    try {
      await ElMessageBox.confirm(
        '确定要提交修改申请吗？请等待管理员审核。',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'info'
        }
      )
      
      // 准备申请数据
      const requestData = {
        name: editForm.name,
        email: editForm.email || null,
        phone: editForm.phone || null,
        department: editForm.department || null,
        major: editForm.major || null
      }

      if (avatarChanged.value) {
        requestData.avatar = editForm.avatar || ''
      }
      
      // 提交申请
      await createProfileRequest(requestData)
      
      ElMessage.success('修改申请已提交，请等待管理员审核')
      editDialogVisible.value = false
      await loadUserInfo()
    } catch (e) {
      if (e !== 'cancel') {
        console.error('提交申请失败:', e)
        ElMessage.error('提交申请失败')
      }
    }
  })
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    try {
      await ElMessageBox.confirm(
        '确定要修改密码吗？',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      
      // 调用修改密码 API
      const res = await changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      
      if (res.code === 200) {
        ElMessage.success('密码修改成功')
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
      } else {
        ElMessage.error(res.message || '修改密码失败')
      }
    } catch (e) {
      if (e !== 'cancel') {
        console.error('修改密码失败:', e)
        ElMessage.error(e.message || '修改密码失败')
      }
    }
  })
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/custom.css';

.profile-page {
  max-width: 900px;
  margin: 0 auto;
}

.profile-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  h2 {
    margin: 0;
    color: #4CAF50;
  }
}

.profile-content {
  padding: 10px 0;
}

.info-list {
  margin-top: 20px;
}

.password-card {
  h2 {
    margin: 0;
    color: #4CAF50;
  }
}

.avatar-upload {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  
  .upload-buttons {
    display: flex;
    flex-direction: column;
    gap: 10px;
    
    .avatar-uploader {
      display: inline-block;
    }
  }
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}
</style>
