<template>
  <div class="page-container">
    <div class="message-page">
      <el-card class="message-card">
        <template #header>
          <div class="card-header">
            <h2>消息通知</h2>
            <div class="header-actions">
              <el-button 
                v-if="userStore.role && userStore.role.toUpperCase() === 'ADMIN'"
                type="primary" 
                @click="showCreateDialog"
              >
                <el-icon><Plus /></el-icon>
                创建系统通知
              </el-button>
              <el-dropdown v-if="!batchOperationMode" :hide-on-click="false">
                <el-button type="danger">
                  批量操作<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="startBatchOperation('delete')">
                      <el-icon><Delete /></el-icon> 批量删除
                    </el-dropdown-item>
                    <el-dropdown-item @click="startBatchOperation('markAsRead')">
                      <el-icon><Check /></el-icon> 批量标记已读
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button type="primary" @click="loadMessages">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </div>
        </template>
        

        
        <div class="message-list" v-loading="loading">
          <div v-if="messages.length === 0" class="empty-state">
            <el-icon :size="64"><Bell /></el-icon>
            <p>暂无消息</p>
          </div>
          
          <div v-else>
            <div
              v-for="message in messages"
              :key="message.id"
              class="message-item"
              :class="{ unread: !message.isRead }"
            >
              <div class="message-checkbox" v-if="batchOperationMode">
                <el-checkbox v-model="message.selected" />
              </div>
              <div class="message-content">
                <div class="message-header">
                  <h4>{{ message.title }}</h4>
                  <div class="message-meta">
                    <el-tag :type="getTypeTag(message.type)" size="small">
                      {{ getMessageTypeLabel(message.type) }}
                    </el-tag>
                    <el-tag :type="message.isRead ? 'info' : 'danger'" size="small" style="margin-left: 8px;">
                      {{ message.isRead ? '已读' : '未读' }}
                    </el-tag>
                    <el-tag v-if="message.type === 'scheduled'" :type="getStatusTag(message)" size="small" style="margin-left: 8px;">
                      {{ getStatusLabel(message) }}
                    </el-tag>
                    <span class="time">{{ formatDate(message.type === 'scheduled' && message.publishTime ? message.publishTime : message.createdAt) }}</span>
                  </div>
                </div>
                <div class="message-body">
                  {{ message.content }}
                </div>
                <div class="message-actions">
                  <el-button 
                    v-if="!message.isRead"
                    type="primary" 
                    size="small" 
                    @click="markAsRead(message.id)"
                  >
                    标记已读
                  </el-button>
                  <el-button 
                    v-if="userStore.role && userStore.role.toUpperCase() === 'ADMIN'"
                    type="warning" 
                    size="small" 
                    @click="showEditDialog(message)"
                  >
                    编辑
                  </el-button>
                  <el-button 
                    type="danger" 
                    size="small" 
                    @click="deleteMessage(message.id)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
      
      <div v-if="batchOperationMode" class="batch-operation-bar show" :class="batchOperationType">
        <div class="left-section">
          <el-checkbox v-model="selectAll" @change="handleSelectAllChange" />
          <span>全选</span>
          <span class="selected-count">已选择 {{ selectedCount }} 项</span>
        </div>
        <div class="right-section">
          <span class="operation-mode" :class="'mode-' + batchOperationType">
            {{ batchOperationType === 'delete' ? '批量删除' : '批量标记已读' }}
          </span>
          <el-button @click="cancelBatchOperation">取消</el-button>
          <el-button 
            :type="batchOperationType === 'delete' ? 'danger' : 'primary'"
            @click="confirmBatchOperation"
            :disabled="selectedCount === 0"
          >
            确定
          </el-button>
        </div>
      </div>
      
      <el-dialog
        v-model="dialogVisible"
        title="创建系统通知"
        class="custom-dialog"
        width="600px"
      >
        <el-form :model="messageForm" :rules="rules" ref="formRef" label-width="100px">
          <el-form-item label="标题" prop="title">
            <el-input v-model="messageForm.title" placeholder="请输入消息标题" />
          </el-form-item>
          
          <el-form-item label="内容" prop="content">
            <el-input 
              v-model="messageForm.content" 
              type="textarea" 
              :rows="6"
              placeholder="请输入消息内容" 
            />
          </el-form-item>
          
          <el-form-item label="公告类型" prop="type">
            <el-select v-model="messageForm.type" style="width: 100%">
              <el-option label="长期活跃" value="permanent" />
              <el-option label="定时公告" value="scheduled" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="状态" prop="status" v-if="messageForm.type === 'permanent'">
            <el-select v-model="messageForm.status" style="width: 100%">
              <el-option label="活跃" value="active" />
              <el-option label="过期" value="expired" />
            </el-select>
          </el-form-item>

          <el-form-item label="发布时间" prop="publishTime" v-if="messageForm.type === 'scheduled'" :rules="[{ required: true, message: '请选择发布时间', trigger: 'change' }]">
            <el-date-picker
              v-model="messageForm.publishTime"
              type="datetime"
              placeholder="选择发布时间"
              style="width: 100%"
              :disabled-date="disabledDate"
            />
          </el-form-item>

          <el-form-item label="过期时间" prop="expireTime" v-if="messageForm.type === 'scheduled'">
            <el-date-picker
              v-model="messageForm.expireTime"
              type="datetime"
              placeholder="选择过期时间"
              style="width: 100%"
              :disabled-date="disabledDate"
            />
          </el-form-item>

          <el-form-item label="接收者" prop="receiverRole">
            <el-select v-model="messageForm.receiverRole" style="width: 100%">
              <el-option label="所有用户" value="ALL" />
              <el-option label="仅教师" value="TEACHER" />
              <el-option label="仅学生" value="STUDENT" />
            </el-select>
          </el-form-item>
        </el-form>
        
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            保存
          </el-button>
        </template>
      </el-dialog>
      
      <el-dialog
        v-model="editDialogVisible"
        title="编辑消息"
        class="custom-dialog"
        width="600px"
      >
        <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
          <el-form-item label="标题" prop="title">
            <el-input v-model="editForm.title" placeholder="请输入消息标题" />
          </el-form-item>
          
          <el-form-item label="内容" prop="content">
            <el-input 
              v-model="editForm.content" 
              type="textarea" 
              :rows="6"
              placeholder="请输入消息内容" 
            />
          </el-form-item>
          
          <el-form-item label="公告类型" prop="type">
            <el-select v-model="editForm.type" style="width: 100%" :disabled="true">
              <el-option label="长期活跃" value="permanent" />
              <el-option label="定时公告" value="scheduled" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="状态" prop="status" v-if="editForm.type === 'permanent'">
            <el-select v-model="editForm.status" style="width: 100%">
              <el-option label="活跃" value="active" />
              <el-option label="过期" value="expired" />
            </el-select>
          </el-form-item>

          <el-form-item label="发布时间" prop="publishTime" v-if="editForm.type === 'scheduled'">
            <el-date-picker
              v-model="editForm.publishTime"
              type="datetime"
              placeholder="选择发布时间"
              style="width: 100%"
              :disabled-date="disabledDate"
            />
          </el-form-item>

          <el-form-item label="过期时间" prop="expireTime" v-if="editForm.type === 'scheduled'">
            <el-date-picker
              v-model="editForm.expireTime"
              type="datetime"
              placeholder="选择过期时间"
              style="width: 100%"
              :disabled-date="disabledDate"
            />
          </el-form-item>
        </el-form>
        
        <template #footer>
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitEditForm" :loading="editing">
            保存修改
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { Plus, Refresh, Select, Delete, Bell } from '@element-plus/icons-vue'
import { getMessages, getUnreadMessages, markMessageAsRead, deleteMessage as deleteMessageApi, createMessage, updateMessage } from '@/api/message'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const loading = ref(false)
const messages = ref([])
const unreadMessages = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const editDialogVisible = ref(false)
const editing = ref(false)
const editFormRef = ref(null)
const editingMessageId = ref(null)

// 批量操作相关
const batchOperationMode = ref(false)
const batchOperationType = ref('')
const selectAll = ref(false)

const selectedCount = computed(() => {
  return messages.value.filter(m => m.selected).length
})

const messageForm = reactive({
  type: 'permanent',
  title: '',
  content: '',
  status: 'active',
  receiverRole: 'ALL',
  publishTime: null,
  expireTime: null
})

const editForm = reactive({
  type: 'permanent',
  title: '',
  content: '',
  status: 'active',
  publishTime: null,
  expireTime: null
})

const editRules = {
  title: [
    { required: true, message: '请输入消息标题', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入消息内容', trigger: 'blur' }
  ]
}

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7
}

const rules = {
  title: [
    { required: true, message: '请输入消息标题', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入消息内容', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择公告类型', trigger: 'change' }
  ],
  receiverRole: [
    { required: true, message: '请选择接收者', trigger: 'change' }
  ]
}

const getTypeTag = (type) => {
  const typeMap = {
    'notification': '',
    'permanent': 'success',
    'system': 'warning',
    'scheduled': 'info'
  }
  return typeMap[type] || ''
}

const getMessageTypeLabel = (type) => {
  const labelMap = {
    'notification': '通知',
    'homework_create': '作业发布',
    'homework_update': '作业更新',
    'homework_delete': '作业删除',
    'submission_graded': '作业批改',
    'permanent': '永久消息',
    'system': '系统消息',
    'scheduled': '定时公告'
  }
  return labelMap[type] || type
}

const getStatusTag = (message) => {
  if (message.type !== 'scheduled') {
    return ''
  }
  const now = Date.now()
  const publishTime = message.publishTime ? new Date(message.publishTime).getTime() : 0
  const expireTime = message.expireTime ? new Date(message.expireTime).getTime() : 0
  
  if (publishTime > now) {
    return 'warning'
  }
  if (expireTime > 0 && expireTime < now) {
    return 'danger'
  }
  return 'success'
}

const getStatusLabel = (message) => {
  if (message.type !== 'scheduled') {
    return ''
  }
  const now = Date.now()
  const publishTime = message.publishTime ? new Date(message.publishTime).getTime() : 0
  const expireTime = message.expireTime ? new Date(message.expireTime).getTime() : 0
  
  if (publishTime > now) {
    return '未发布'
  }
  if (expireTime > 0 && expireTime < now) {
    return '已过期'
  }
  return '已发布'
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const loadMessages = async () => {
  loading.value = true
  try {
    const res = await getMessages()
    messages.value = (res.data || []).map(msg => ({
      ...msg,
      selected: false
    })).sort((a, b) => {
      // 首先按未读状态排序（未读在前）
      if (a.isRead !== b.isRead) {
        return a.isRead ? 1 : -1
      }
      // 然后按创建时间排序（最新的在前）
      return new Date(b.createdAt) - new Date(a.createdAt)
    })
  } catch (e) {
    console.error('加载消息失败:', e)
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

const loadUnreadMessages = async () => {
  try {
    const res = await getUnreadMessages()
    unreadMessages.value = res.data || []
  } catch (e) {
    console.error('加载未读消息失败:', e)
  }
}

const markAsRead = async (id) => {
  try {
    await markMessageAsRead(id)
    ElMessage.success('标记成功')
    await loadMessages()
    await loadUnreadMessages()
    // 重新加载未读消息计数，更新红点
    if (window.loadUnreadMessages) {
      window.loadUnreadMessages()
    }
  } catch (e) {
    console.error('标记失败:', e)
    ElMessage.error('标记失败')
  }
}

const markAllAsRead = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要标记所有未读消息为已读吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    const unreadIds = messages.value.filter(m => !m.isRead).map(m => m.id)
    for (const id of unreadIds) {
      await markMessageAsRead(id)
    }
    
    ElMessage.success('批量标记成功')
    await loadMessages()
    await loadUnreadMessages()
    // 重新加载未读消息计数，更新红点
    if (window.loadUnreadMessages) {
      window.loadUnreadMessages()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量标记失败:', e)
      ElMessage.error('批量标记失败')
    }
  }
}

const deleteMessage = async (id) => {
  try {
    const message = messages.value.find(m => m.id === id)
    const isAnnouncement = message && (message.type === 'permanent' || message.type === 'scheduled')
    
    let confirmMessage = '确定要删除此消息吗？'
    if (isAnnouncement && userStore.role && userStore.role.toUpperCase() === 'ADMIN') {
      confirmMessage = '此操作将删除发送给所有用户的该公告，确定要继续吗？'
    }
    
    await ElMessageBox.confirm(
      confirmMessage,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteMessageApi(id)
    ElMessage.success('删除成功')
    await loadMessages()
    await loadUnreadMessages()
    // 重新加载未读消息计数，更新红点
    if (window.loadUnreadMessages) {
      window.loadUnreadMessages()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败:', e)
      ElMessage.error('删除失败')
    }
  }
}

const deleteAllRead = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要删除所有已读消息吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const readIds = messages.value.filter(m => m.isRead).map(m => m.id)
    for (const id of readIds) {
      await deleteMessageApi(id)
    }
    
    ElMessage.success('批量删除成功')
    await loadMessages()
    await loadUnreadMessages()
    // 重新加载未读消息计数，更新红点
    if (window.loadUnreadMessages) {
      window.loadUnreadMessages()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量删除失败:', e)
      ElMessage.error('批量删除失败')
    }
  }
}

const showCreateDialog = () => {
  Object.assign(messageForm, {
    type: 'permanent',
    title: '',
    content: '',
    status: 'active',
    receiverRole: 'ALL',
    publishTime: null,
    expireTime: null
  })
  dialogVisible.value = true
}

const showEditDialog = (message) => {
  editingMessageId.value = message.id
  Object.assign(editForm, {
    type: message.type || 'permanent',
    title: message.title || '',
    content: message.content || '',
    status: message.status || 'active',
    publishTime: message.publishTime ? new Date(message.publishTime) : null,
    expireTime: message.expireTime ? new Date(message.expireTime) : null
  })
  editDialogVisible.value = true
}

const submitEditForm = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    editing.value = true
    try {
      const data = { ...editForm }
      if (data.publishTime) {
        data.publishTime = data.publishTime.getTime()
      }
      if (data.expireTime) {
        data.expireTime = data.expireTime.getTime()
      }
      await updateMessage(editingMessageId.value, data)
      ElMessage.success('消息更新成功')
      editDialogVisible.value = false
      await loadMessages()
    } catch (e) {
      console.error('更新失败:', e)
      ElMessage.error('更新失败')
    } finally {
      editing.value = false
    }
  })
}

const submitForm = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      const data = { ...messageForm }
      if (data.publishTime) {
        data.publishTime = data.publishTime.getTime()
      }
      if (data.expireTime) {
        data.expireTime = data.expireTime.getTime()
      }
      await createMessage(data)
      ElMessage.success('消息发送成功')
      dialogVisible.value = false
      await loadMessages()
    } catch (e) {
      console.error('发送失败:', e)
      ElMessage.error('发送失败')
    } finally {
      submitting.value = false
    }
  })
}

// 批量操作方法
const startBatchOperation = (type) => {
  batchOperationMode.value = true
  batchOperationType.value = type
  selectAll.value = false
  messages.value.forEach(m => m.selected = false)
}

const cancelBatchOperation = () => {
  batchOperationMode.value = false
  batchOperationType.value = ''
  selectAll.value = false
  messages.value.forEach(m => m.selected = false)
}

const handleSelectAllChange = (value) => {
  messages.value.forEach(m => {
    m.selected = value
  })
}

const batchMarkAsRead = async () => {
  try {
    const selectedIds = messages.value.filter(m => m.selected).map(m => m.id)
    if (selectedIds.length === 0) {
      ElMessage.warning('请选择要标记的消息')
      return
    }
    
    await ElMessageBox.confirm(
      `确定要标记选中的 ${selectedIds.length} 条消息为已读吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    for (const id of selectedIds) {
      await markMessageAsRead(id)
    }
    
    ElMessage.success('批量标记成功')
    cancelBatchOperation()
    await loadMessages()
    await loadUnreadMessages()
    // 重新加载未读消息计数，更新红点
    if (window.loadUnreadMessages) {
      window.loadUnreadMessages()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量标记失败:', e)
      ElMessage.error('批量标记失败')
    }
  }
}

const batchDeleteMessages = async () => {
  try {
    const selectedIds = messages.value.filter(m => m.selected).map(m => m.id)
    if (selectedIds.length === 0) {
      ElMessage.warning('请选择要删除的消息')
      return
    }
    
    const selectedMessages = messages.value.filter(m => m.selected)
    const hasAnnouncement = selectedMessages.some(m => m.type === 'permanent' || m.type === 'scheduled')
    
    let confirmMessage = `确定要删除选中的 ${selectedIds.length} 条消息吗？`
    if (hasAnnouncement && userStore.role && userStore.role.toUpperCase() === 'ADMIN') {
      confirmMessage = `选中的消息中包含公告，删除将删除发送给所有用户的该公告。确定要删除选中的 ${selectedIds.length} 条消息吗？`
    }
    
    await ElMessageBox.confirm(
      confirmMessage,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    for (const id of selectedIds) {
      await deleteMessageApi(id)
    }
    
    ElMessage.success('批量删除成功')
    cancelBatchOperation()
    await loadMessages()
    await loadUnreadMessages()
    // 重新加载未读消息计数，更新红点
    if (window.loadUnreadMessages) {
      window.loadUnreadMessages()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量删除失败:', e)
      ElMessage.error('批量删除失败')
    }
  }
}

const confirmBatchOperation = async () => {
  if (batchOperationType.value === 'delete') {
    await batchDeleteMessages()
  } else if (batchOperationType.value === 'markAsRead') {
    await batchMarkAsRead()
  }
}

onMounted(() => {
  loadMessages()
  loadUnreadMessages()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/custom.css';

.message-page {
  max-width: 1000px;
  margin: 0 auto;
}

.message-card {
  :deep(.el-card__header) {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h2 {
        margin: 0;
        color: #4CAF50;
      }
      
      .header-actions {
        display: flex;
        gap: 10px;
      }
    }
  }
}

.batch-actions {
  margin-top: 15px;
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  
  .controls {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    align-items: center;
    
    .selected-count {
      font-size: 14px;
      color: #666;
      margin: 0 10px;
    }
  }
}

.message-list {
  margin-top: 15px;
}

.message-item {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;
  border-left: 4px solid #4CAF50;
  display: flex;
  gap: 15px;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
  }
  
  &.unread {
    background-color: #f9f9f9;
    border-left-color: #2196F3;
  }
  
  .message-checkbox {
    display: flex;
    align-items: flex-start;
    padding-top: 3px;
  }
  
  .message-content {
    flex: 1;
    
    .message-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      
      h4 {
        color: #4CAF50;
        margin: 0;
        font-size: 18px;
      }
      
      .message-meta {
        display: flex;
        align-items: center;
        gap: 8px;
        
        .time {
          font-size: 13px;
          color: #999;
        }
      }
    }
    
    .message-body {
      color: #333;
      line-height: 1.8;
      margin-bottom: 15px;
      white-space: pre-wrap;
    }
    
    .message-actions {
      display: flex;
      gap: 10px;
    }
  }
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

.batch-operation-bar {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: white;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: none;
  align-items: center;
  gap: 20px;
  z-index: 1000;
  min-width: 400px;
  
  &.show {
    display: flex;
  }
  
  &.delete {
    border: 2px solid #f44336;
  }
  
  &.markAsRead {
    border: 2px solid #4CAF50;
  }
  
  .left-section {
    display: flex;
    align-items: center;
    gap: 10px;
    
    .selected-count {
      color: #666;
      font-size: 14px;
    }
  }
  
  .right-section {
    display: flex;
    align-items: center;
    gap: 10px;
    
    .operation-mode {
      font-weight: bold;
      margin-right: 5px;
      
      &.mode-delete {
        color: #f44336;
      }
      
      &.mode-markAsRead {
        color: #4CAF50;
      }
    }
  }
}
</style>
