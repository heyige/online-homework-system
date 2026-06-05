<template>
  <div class="profile-requests-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>申请审批</span>
          <el-button type="primary" @click="showCreateDialog" v-if="userStore.role !== 'ADMIN'">
            <el-icon><Plus /></el-icon>
            发起申请
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我的申请" name="my" v-if="userStore.role !== 'ADMIN'"></el-tab-pane>
        <el-tab-pane label="待审批" name="pending" v-if="userStore.role === 'ADMIN'"></el-tab-pane>
        <el-tab-pane label="全部申请" name="all" v-if="userStore.role === 'ADMIN'"></el-tab-pane>
      </el-tabs>

      <el-table :data="requestList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :size="40" :src="resolveAvatarUrl(row.avatar)" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="department" label="院系" width="120" />
        <el-table-column prop="major" label="专业" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" v-if="userStore.role === 'ADMIN'">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button type="success" size="small" @click="approveRequest(row)">
                批准
              </el-button>
              <el-button type="danger" size="small" @click="showRejectDialog(row.id)">
                拒绝
              </el-button>
            </template>
            <template v-else>
              <el-button type="info" size="small" disabled>
                已处理
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadRequests"
        @current-change="loadRequests"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="发起个人信息修改申请" width="500px">
      <el-form :model="requestForm" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="requestForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="requestForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="requestForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="院系" prop="department">
          <el-select
            v-model="requestForm.department"
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
            v-model="requestForm.major"
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
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="拒绝申请" width="400px">
      <el-form :model="rejectForm" ref="rejectFormRef" label-width="80px">
        <el-form-item label="拒绝理由" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝理由"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :loading="submitting">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getMyProfileRequests,
  getAllProfileRequests,
  getPendingProfileRequests,
  createProfileRequest,
  approveProfileRequest,
  rejectProfileRequest
} from '@/api/profileRequest'
import { resolveAvatarUrl } from '@/utils/avatar'
import {
  DEPARTMENT_OPTIONS,
  getMajorsByDepartment,
  onDepartmentChange,
  onMajorChange
} from '@/utils/departmentMajor'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const requestList = ref([])
const dialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const activeTab = ref('my')
const formRef = ref(null)
const rejectFormRef = ref(null)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const currentRejectId = ref(null)

const requestForm = reactive({
  name: '',
  email: '',
  phone: '',
  department: '',
  major: ''
})

const availableMajors = computed(() => getMajorsByDepartment(requestForm.department))

const handleDepartmentChange = (value) => {
  onDepartmentChange(requestForm, value)
}

const handleMajorChange = (value) => {
  onMajorChange(requestForm, value)
}

const rejectForm = reactive({
  reason: ''
})

const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ]
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const getStatusTag = (status) => {
  const tagMap = {
    'pending': 'warning',
    'approved': 'success',
    'rejected': 'danger'
  }
  return tagMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    'pending': '待审批',
    'approved': '已批准',
    'rejected': '已拒绝'
  }
  return textMap[status] || status
}

const loadRequests = async () => {
  loading.value = true
  try {
    let res
    if (activeTab.value === 'my') {
      res = await getMyProfileRequests()
    } else if (activeTab.value === 'pending') {
      res = await getPendingProfileRequests()
    } else {
      res = await getAllProfileRequests()
    }
    if (res.code === 200) {
      requestList.value = res.data || []
      pagination.total = requestList.value.length
    }
  } catch (error) {
    ElMessage.error('加载申请列表失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  loadRequests()
}

const showCreateDialog = () => {
  Object.assign(requestForm, {
    name: userStore.user?.name || '',
    email: userStore.user?.email || '',
    phone: userStore.user?.phone || '',
    department: userStore.user?.department || '',
    major: userStore.user?.major || ''
  })
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await createProfileRequest(requestForm)
      ElMessage.success('申请提交成功')
      dialogVisible.value = false
      loadRequests()
    } catch (error) {
      ElMessage.error(error.message || '申请提交失败')
    } finally {
      submitting.value = false
    }
  })
}

const approveRequest = async (row) => {
  try {
    await ElMessageBox.confirm('确定要批准该申请吗？', '确认批准', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await approveProfileRequest(row.id)
    ElMessage.success('批准成功')

    const currentUserId = userStore.user?.id ?? userStore.user?.userId
    if (row.userId === currentUserId) {
      await userStore.getUserInfo()
    }

    loadRequests()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批准失败')
    }
  }
}

const showRejectDialog = (id) => {
  currentRejectId.value = id
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请输入拒绝理由')
    return
  }

  submitting.value = true
  try {
    await rejectProfileRequest(currentRejectId.value, rejectForm.reason)
    ElMessage.success('已拒绝申请')
    rejectDialogVisible.value = false
    loadRequests()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  activeTab.value = userStore.role === 'ADMIN' ? 'pending' : 'my'
  loadRequests()
})
</script>

<style lang="scss" scoped>
.profile-requests-container {
  padding: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>