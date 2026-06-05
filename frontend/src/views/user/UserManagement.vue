<template>
  <div class="user-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            创建用户
          </el-button>
        </div>
      </template>
      
      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="用户名" clearable />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="姓名" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="请选择角色" clearable style="width: 200px">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUserList">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 用户表格 -->
      <el-table :data="userList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="getRoleTag(row.role)">
              {{ getRoleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="department" label="院系" width="120" />
        <el-table-column prop="major" label="专业" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="showEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDeleteUser(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadUserList"
        @current-change="loadUserList"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
    
    <!-- 创建/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '创建用户'"
      width="600px"
    >
      <el-form :model="userForm" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="头像" v-if="isEdit">
          <div class="avatar-upload">
            <el-avatar :size="80" :src="resolveAvatarUrl(userForm.avatar)" />
            <el-upload
              :auto-upload="false"
              :limit="1"
              :on-change="handleAvatarChange"
              accept="image/*"
              class="avatar-uploader"
            >
              <el-button type="primary" size="small">选择头像</el-button>
              <template #tip>
                <div class="el-upload__tip">支持 JPG、PNG，不超过 2MB</div>
              </template>
            </el-upload>
          </div>
        </el-form-item>
        <template v-if="!isEdit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="userForm.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="userForm.password" type="password" placeholder="请输入密码" />
          </el-form-item>
        </template>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" style="width: 100%" @change="handleRoleChange">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="学号" prop="studentId" v-if="userForm.role === 'STUDENT'">
          <el-input v-model="userForm.studentId" placeholder="系统自动生成" :disabled="!isEdit" />
        </el-form-item>
        <el-form-item label="院系" prop="department">
          <el-select
            v-model="userForm.department"
            placeholder="请选择院系"
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
        <el-form-item label="专业" prop="major">
          <el-select
            v-model="userForm.major"
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
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getAllUsers, createUser, updateUser, deleteUser } from '@/api/user'
import { prepareAndUploadAvatar, resolveAvatarUrl } from '@/utils/avatar'
import {
  DEPARTMENT_OPTIONS,
  getMajorsByDepartment,
  onDepartmentChange,
  onMajorChange
} from '@/utils/departmentMajor'

const loading = ref(false)
const submitting = ref(false)
const userList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const selectedAvatar = ref(null)

const searchForm = reactive({
  username: '',
  name: '',
  role: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const userForm = reactive({
  id: null,
  username: '',
  password: '',
  name: '',
  role: 'STUDENT',
  studentId: '',
  department: '',
  major: '',
  email: '',
  phone: '',
  avatar: ''
})

const availableMajors = computed(() => getMajorsByDepartment(userForm.department))

const handleDepartmentChange = (value) => {
  onDepartmentChange(userForm, value)
}

const handleMajorChange = (value) => {
  onMajorChange(userForm, value)
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在 3 到 50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, message: '密码长度不能少于 4 个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// 加载用户列表
const loadUserList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      limit: pagination.pageSize,
      username: searchForm.username || null,
      name: searchForm.name || null,
      role: searchForm.role || null
    }
    const res = await getAllUsers(params)
    if (res.code === 200) {
      userList.value = res.data?.content || res.data || []
      pagination.total = res.data?.totalElements || res.data?.length || 0
    }
  } catch (error) {
    ElMessage.error('加载用户列表失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 显示创建对话框
const showCreateDialog = () => {
  isEdit.value = false
  Object.assign(userForm, {
    id: null,
    username: '',
    password: '',
    name: '',
    role: 'STUDENT',
    studentId: '',
    department: '',
    major: '',
    email: '',
    phone: '',
    avatar: ''
  })
  // 为学生角色自动生成学号
  if (userForm.role === 'STUDENT') {
    userForm.studentId = generateStudentId()
  }
  dialogVisible.value = true
}

// 处理角色变化
const handleRoleChange = () => {
  if (userForm.role === 'STUDENT' && !isEdit.value) {
    userForm.studentId = generateStudentId()
  } else {
    userForm.studentId = ''
  }
}

// 生成学号
const generateStudentId = () => {
  // 生成规则：年份（4位）+ 随机数（6位）
  const year = new Date().getFullYear()
  const random = Math.floor(Math.random() * 1000000).toString().padStart(6, '0')
  return `${year}${random}`
}

// 显示编辑对话框
const showEditDialog = (row) => {
  isEdit.value = true
  Object.assign(userForm, {
    id: row.id,
    username: row.username,
    password: '',
    name: row.name || '',
    role: row.role,
    studentId: row.studentId || '',
    department: row.department || '',
    major: row.major || '',
    email: row.email || '',
    phone: row.phone || '',
    avatar: row.avatar || ''
  })
  dialogVisible.value = true
}

// 头像选择处理
const handleAvatarChange = async (file) => {
  const validTypes = ['image/jpeg', 'image/png']
  if (!validTypes.includes(file.raw.type)) {
    ElMessage.error('只支持 JPG、PNG 格式的头像')
    return
  }
  if (file.raw.size > 2 * 1024 * 1024) {
    ElMessage.error('头像文件大小不能超过 2MB')
    return
  }
  try {
    selectedAvatar.value = file.raw
    userForm.avatar = await prepareAndUploadAvatar(file.raw)
  } catch (e) {
    console.error('头像上传失败:', e)
    ElMessage.error(e.message || '头像上传失败，请重试')
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const submitData = { ...userForm }
      if (isEdit.value) {
        delete submitData.password
        delete submitData.username
        if (!submitData.studentId) delete submitData.studentId
        if (!submitData.department) delete submitData.department
        if (!submitData.major) delete submitData.major
        if (!submitData.avatar) delete submitData.avatar
        await updateUser(userForm.id, submitData)
        ElMessage.success('用户更新成功')
      } else {
        if (!submitData.password) {
          ElMessage.error('请输入密码')
          submitting.value = false
          return
        }
        // 确保邮箱和手机号为空时也能正常提交
        if (!submitData.email) delete submitData.email
        if (!submitData.phone) delete submitData.phone
        await createUser(submitData)
        ElMessage.success('用户创建成功')
      }
      dialogVisible.value = false
      loadUserList()
    } catch (error) {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    } finally {
      submitting.value = false
    }
  })
}

// 删除用户
const handleDeleteUser = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteUser(id)
    ElMessage.success('删除成功')
    loadUserList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.username = ''
  searchForm.name = ''
  searchForm.role = ''
  pagination.page = 1
  loadUserList()
}

// 获取角色标签（支持大小写不敏感）
const getRoleTag = (role) => {
  const tagMap = {
    ADMIN: 'danger',
    TEACHER: 'warning',
    STUDENT: 'success'
  }
  const upperRole = (role || '').toUpperCase()
  return tagMap[upperRole] || 'info'
}

// 获取角色文本（支持大小写不敏感）
const getRoleText = (role) => {
  const textMap = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  }
  const upperRole = (role || '').toUpperCase()
  return textMap[upperRole] || role
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  loadUserList()
})
</script>

<style lang="scss" scoped>
.user-management-container {
  padding: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-form {
    margin-bottom: 20px;
  }

  .avatar-upload {
    display: flex;
    align-items: center;
    gap: 20px;

    .avatar-uploader {
      display: inline-block;
    }
  }
}
</style>
