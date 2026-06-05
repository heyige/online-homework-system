<template>
  <div class="page-container">
    <div class="submit-homework-page">
      <!-- 选择作业视图 -->
      <el-card class="homework-list-card" v-if="!selectedHomework && !submissionDetail">
        <template #header>
          <div class="card-header">
            <h2>选择作业</h2>
          </div>
        </template>
        
        <div v-loading="loading">
          <div v-if="homeworkList.length === 0" class="empty-state">
            <el-icon :size="64"><Document /></el-icon>
            <p>暂无可提交的作业</p>
          </div>
          
          <div v-else class="info-list">
            <div
              v-for="homework in homeworkList"
              :key="homework.id"
              class="homework-item"
            >
              <div class="info-item">
                <label>作业标题：</label>
                <span>{{ homework.title }}</span>
              </div>
              <div class="info-item">
                <label>课程：</label>
                <span>{{ homework.courseName }}</span>
              </div>
              <div class="info-item">
                <label>截止时间：</label>
                <span>{{ formatDate(homework.deadline) }}</span>
              </div>
              <div class="info-item">
                <label>状态：</label>
                <el-tag :type="homework.status === 'published' ? 'success' : 'danger'" size="small">
                  {{ homework.status === 'published' ? '可提交' : '已截止' }}
                </el-tag>
              </div>
              <div class="info-item">
                <label>作业描述：</label>
                <span class="description">{{ homework.description }}</span>
              </div>
              <div class="info-item actions">
                <label>操作：</label>
                <el-button 
                  type="primary" 
                  size="small" 
                  :disabled="homework.status !== 'published'"
                  @click="selectHomework(homework)"
                >
                  提交作业
                </el-button>
              </div>
              <div class="divider"></div>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- 提交作业视图 -->
      <el-card class="submission-form-card" v-else-if="selectedHomework && !submissionDetail">
        <template #header>
          <div class="card-header">
            <h2>提交作业：{{ selectedHomework.title }}</h2>
            <el-button @click="backToHomeworkList">
              <el-icon><ArrowLeft /></el-icon> 返回
            </el-button>
          </div>
        </template>

        <!-- 作业描述 -->
        <div v-if="selectedHomework.description" class="homework-description-section">
          <div class="info-item">
            <label>作业描述：</label>
            <div class="description-display">{{ selectedHomework.description }}</div>
          </div>
          <el-divider></el-divider>
        </div>

        <div v-if="existingSubmission" class="existing-submission">
          <div class="message-alert info">
            <el-icon><InfoFilled /></el-icon>
            <span>您已提交过此作业，以下是您之前提交的内容</span>
          </div>
          <div class="info-list">
            <div class="info-item">
              <label>提交时间：</label>
              <span>{{ formatDate(existingSubmission.submittedAt) }}</span>
            </div>
            <div class="info-item" v-if="existingSubmission.status === 'graded'">
              <label>成绩：</label>
              <span style="color: #67C23A; font-weight: bold;">{{ existingSubmission.score }} / 100</span>
            </div>
            <div class="info-item" v-if="existingSubmission.feedback">
              <label>教师评语：</label>
              <span class="feedback">{{ existingSubmission.feedback }}</span>
            </div>
            <div class="info-item">
              <label>已提交内容：</label>
              <div class="content-display">{{ existingSubmission.content }}</div>
            </div>
          </div>
        </div>
        
        <div v-if="existingSubmission?.status === 'graded'" class="message-alert warning" style="margin-bottom: 16px;">
          <el-icon><InfoFilled /></el-icon>
          <span>该作业已批改，不能再修改或重新提交</span>
        </div>

        <el-form
          v-if="canSubmitForm"
          ref="submitFormRef"
          :model="submitForm"
          label-width="100px"
          class="custom-form"
        >
          <el-form-item label="作业内容" required>
            <el-input
              v-model="submitForm.content"
              type="textarea"
              :rows="12"
              placeholder="请输入作业内容..."
            />
          </el-form-item>
          
          <el-form-item label="附件上传">
            <el-upload
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-exceed="handleFileExceed"
              :show-file-list="false"
              :accept="SUBMISSION_ATTACHMENT_ACCEPT"
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon> 选择文件
              </el-button>
              <template #tip>
                <div class="el-upload__tip">
                  {{ SUBMISSION_ATTACHMENT_TIP }}
                </div>
              </template>
            </el-upload>
            <div v-if="selectedFile" class="selected-file-info">
              <el-icon><Document /></el-icon>
              <span class="file-name">{{ selectedFile.name }}</span>
              <span class="file-size">({{ formatFileSize(selectedFile.size) }})</span>
              <el-button type="danger" size="small" @click="removeSelectedFile">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </div>
            <div v-else-if="existingSubmission && (existingSubmission.filePath || existingSubmission.fileName)" class="selected-file-info">
              <el-icon><Document /></el-icon>
              <span class="file-name">{{ existingSubmission.fileName || getFileNameFromPath(existingSubmission.filePath) }}</span>
              <el-button type="primary" size="small" @click="downloadFile(existingSubmission)">
                <el-icon><Download /></el-icon> 下载
              </el-button>
              <el-button type="danger" size="small" @click="deleteExistingAttachment">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </div>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="handleSubmit" :loading="submitting">
              <el-icon><Check /></el-icon> 提交作业
            </el-button>
            <el-button @click="backToHomeworkList">取消</el-button>
          </el-form-item>
        </el-form>
      </el-card>
      
      <!-- 提交详情视图（当有 submissionId 参数时显示） -->
      <el-card class="submission-detail-card" v-if="submissionDetail">
        <template #header>
          <div class="card-header">
            <h2>提交详情</h2>
            <div>
              <el-button 
                v-if="canModify" 
                type="primary" 
                size="small" 
                @click="startModify"
              >
                <el-icon><Edit /></el-icon> 修改
              </el-button>
              <el-button @click="backToSubmissionList">
                <el-icon><ArrowLeft /></el-icon> 返回列表
              </el-button>
            </div>
          </div>
        </template>
        
        <!-- 查看模式 -->
        <div v-if="!isModifying" class="info-list">
          <div class="info-item">
            <label>作业标题：</label>
            <span>{{ submissionDetail.homeworkTitle }}</span>
          </div>
          <div class="info-item">
            <label>课程：</label>
            <span>{{ submissionDetail.courseName }}</span>
          </div>
          <div class="info-item" v-if="submissionDetail.description">
            <label>作业描述：</label>
            <div class="description-display">{{ submissionDetail.description }}</div>
          </div>
          <div class="info-item">
            <label>提交时间：</label>
            <span>{{ formatDate(submissionDetail.submittedAt) }}</span>
          </div>
          <div class="info-item">
            <label>状态：</label>
            <el-tag :type="submissionDetail.status === 'graded' ? 'success' : 'warning'">
              {{ submissionDetail.status === 'graded' ? '已批改' : '待批改' }}
            </el-tag>
          </div>
          <div class="info-item" v-if="submissionDetail.score !== null">
            <label>得分：</label>
            <span style="color: #67C23A; font-weight: bold; font-size: 18px">
              {{ submissionDetail.score }}
            </span>
          </div>
          <div class="info-item">
            <label>作业内容：</label>
            <div class="content-display">{{ submissionDetail.content }}</div>
          </div>
          <div class="info-item" v-if="submissionDetail.filePath || submissionDetail.fileName">
            <label>附件：</label>
            <div class="file-item">
              <el-icon><Document /></el-icon>
              <span class="file-name">{{ submissionDetail.fileName || getFileNameFromPath(submissionDetail.filePath) }}</span>
              <el-button type="primary" size="small" @click="downloadFile(submissionDetail)">
                <el-icon><Download /></el-icon> 下载
              </el-button>
            </div>
          </div>
          <div class="info-item" v-if="submissionDetail.feedback">
            <label>教师反馈：</label>
            <div class="feedback-display">{{ submissionDetail.feedback }}</div>
          </div>
        </div>
        
        <!-- 修改模式 -->
        <div v-else class="modify-form">
          <!-- 作业描述（只读） -->
          <div v-if="submissionDetail.description" class="info-item" style="margin-bottom: 20px;">
            <label>作业描述：</label>
            <div class="description-display">{{ submissionDetail.description }}</div>
          </div>
          <el-divider v-if="submissionDetail.description"></el-divider>

          <el-form
            ref="modifyFormRef"
            :model="modifyForm"
            label-width="80px"
            class="custom-form"
          >
            <el-form-item label="作业内容" required>
              <el-input
                v-model="modifyForm.content"
                type="textarea"
                :rows="10"
                placeholder="请输入作业内容..."
                class="content-textarea"
              />
            </el-form-item>
            
            <el-form-item label="附件上传">
              <div class="upload-section">
                <el-upload
                  :auto-upload="false"
                  :limit="1"
                  :on-change="handleModifyFileChange"
                  :on-exceed="handleModifyFileExceed"
                  :show-file-list="false"
                  :accept="SUBMISSION_ATTACHMENT_ACCEPT"
                  class="upload-button"
                >
                  <el-button type="primary">
                    <el-icon><Upload /></el-icon> 选择文件
                  </el-button>
                </el-upload>
                <div class="upload-tip">
                  {{ SUBMISSION_ATTACHMENT_TIP }}
                </div>
                <div v-if="modifyFile" class="selected-file-info">
                  <el-icon><Document /></el-icon>
                  <span class="file-name">{{ modifyFile.name }}</span>
                  <span class="file-size">({{ formatFileSize(modifyFile.size) }})</span>
                  <el-button type="danger" size="small" @click="removeModifyFile">
                    <el-icon><Delete /></el-icon> 删除
                  </el-button>
                </div>
                <div v-else-if="submissionDetail.filePath || submissionDetail.fileName" class="selected-file-info">
                  <el-icon><Document /></el-icon>
                  <span class="file-name">{{ submissionDetail.fileName || getFileNameFromPath(submissionDetail.filePath) }}</span>
                  <el-button type="primary" size="small" @click="downloadFile(submissionDetail)">
                    <el-icon><Download /></el-icon> 下载
                  </el-button>
                  <el-button type="danger" size="small" @click="deleteAttachment">
                    <el-icon><Delete /></el-icon> 删除
                  </el-button>
                </div>
              </div>
            </el-form-item>
            
            <el-form-item class="form-actions">
              <el-button type="primary" @click="handleModifySubmit" :loading="modifying" class="action-button">
                <el-icon><Check /></el-icon> 提交修改
              </el-button>
              <el-button @click="cancelModify" class="action-button">取消</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getHomeworkByStudent, getHomeworkById } from '@/api/homework'
import { getSubmissionById, getSubmissionByHomeworkForStudent, submitHomework, downloadAttachment, updateSubmission } from '@/api/submission'
import { SUBMISSION_ATTACHMENT_ACCEPT, SUBMISSION_ATTACHMENT_TIP, validateSubmissionAttachment } from '@/utils/submissionFile'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Clock, ArrowLeft, InfoFilled, Upload, Check, Download, Delete } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const homeworkList = ref([])
const selectedHomework = ref(null)
const submitForm = reactive({
  content: '',
  file: null
})
const submitFormRef = ref(null)
const selectedFile = ref(null)
const attachmentDeleted = ref(false)
const existingSubmission = ref(null)
const submissionDetail = ref(null)

// 修改功能相关变量
const isModifying = ref(false)
const modifyForm = reactive({
  content: ''
})
const modifyFile = ref(null)
const modifyAttachmentDeleted = ref(false)
const modifying = ref(false)
const modifyFormRef = ref(null)

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

const getFileNameFromPath = (filePath) => {
  if (!filePath) return '附件文件'
  // 从文件路径中提取文件名
  const parts = filePath.split(/[\\/]/)
  return parts[parts.length - 1] || '附件文件'
}

const loadHomeworkList = async () => {
  loading.value = true
  try {
    const res = await getHomeworkByStudent()
    homeworkList.value = (res.data || [])
      .filter(item => item.homework && item.homework.status === 'published')
      .map(item => ({
        id: item.homework.id,
        title: item.homework.title,
        description: item.homework.description,
        courseName: item.homework.courseName,
        deadline: item.homework.deadline,
        status: item.homework.status
      }))
  } catch (e) {
    console.error('加载作业列表失败:', e)
    ElMessage.error('加载作业列表失败')
  } finally {
    loading.value = false
  }
}

const loadHomeworkById = async (homeworkId) => {
  loading.value = true
  try {
    const res = await getHomeworkById(homeworkId)
    if (res.data) {
      // 只加载指定的作业
      homeworkList.value = [res.data]
    }
  } catch (e) {
    console.error('加载作业详情失败:', e)
    ElMessage.error('加载作业详情失败')
  } finally {
    loading.value = false
  }
}

const selectHomework = async (homework) => {
  if (homework.status !== 'published') {
    ElMessage.warning('该作业已截止，无法提交')
    return
  }
  
  // 如果是查看详情模式，不设置selectedHomework，避免闪烁
  const viewDetail = route.query.viewDetail === 'true'
  if (!viewDetail) {
    selectedHomework.value = homework
    submitForm.content = ''
    selectedFile.value = null
    attachmentDeleted.value = false
  }
  
  try {
    // 使用学生专用的 API，获取当前学生在指定作业的提交
    const res = await getSubmissionByHomeworkForStudent(homework.id)
    if (res.data) {
      existingSubmission.value = res.data
      
      // 如果是查看详情模式，直接设置为提交详情
      if (viewDetail) {
        submissionDetail.value = res.data
      } else {
        // 如果是提交作业视图，设置表单内容和附件信息
        submitForm.content = existingSubmission.value.content || ''
      }
    } else {
      // 如果没有提交记录，且是查看详情模式，显示提示
      if (viewDetail) {
        ElMessage.warning('您还没有提交过此作业')
        router.push('/homework')
      }
    }
  } catch (e) {
    existingSubmission.value = null
    if (viewDetail) {
      ElMessage.error('加载提交详情失败')
      router.push('/homework')
    }
  }
}

const backToHomeworkList = () => {
  router.push('/homework')
}

const backToSubmissionList = () => {
  submissionDetail.value = null
  router.push('/homework')
}

const downloadFile = (submission) => {
  if (!submission.filePath && !submission.fileName) {
    ElMessage.warning('没有可下载的文件')
    return
  }

  downloadAttachment(submission.id)
}

const handleFileChange = (file) => {
  const error = validateSubmissionAttachment(file.raw)
  if (error) {
    ElMessage.error(error)
    return
  }
  selectedFile.value = file.raw
  attachmentDeleted.value = false
}

const handleFileExceed = (files) => {
  handleFileChange({ raw: files[0] })
}

const removeSelectedFile = () => {
  // 移除新选择的文件
  selectedFile.value = null
  ElMessage.success('已移除选择的文件')
}

const deleteExistingAttachment = () => {
  // 删除已有附件
  ElMessageBox.confirm(
    '确定要删除附件吗？删除后将无法恢复',
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    if (existingSubmission.value) {
      existingSubmission.value.filePath = null
      existingSubmission.value.fileName = null
    }
    selectedFile.value = null
    attachmentDeleted.value = true
    ElMessage.success('附件已删除，请提交以保存')
  }).catch(() => {
    // 取消删除
  })
}

const handleSubmit = async () => {
  if (!submitForm.content.trim()) {
    ElMessage.warning('请输入作业内容')
    return
  }

  if (isSubmissionGraded(existingSubmission.value)) {
    ElMessage.warning('已批改的作业不能修改')
    return
  }
  
  if (!selectedHomework.value) {
    ElMessage.error('请选择作业')
    return
  }
  
  submitting.value = true
  
  try {
    // 如果有现有提交记录，使用 updateSubmission 来支持附件删除
    if (existingSubmission.value && existingSubmission.value.id) {
      await updateSubmission(
        existingSubmission.value.id,
        submitForm.content,
        selectedFile.value,
        attachmentDeleted.value && !selectedFile.value
      )
    } else {
      // 新提交
      await submitHomework(
        selectedHomework.value.id,
        submitForm.content,
        selectedFile.value
      )
    }
    ElMessage.success('提交成功')
    // 提交成功后返回作业列表
    router.push('/homework')
  } catch (e) {
    console.error('提交作业失败:', e)
    ElMessage.error('提交作业失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  // 支持路由参数和查询参数两种方式
  const homeworkId = route.params.homeworkId || route.query.homeworkId
  const submissionId = route.params.submissionId || route.query.submissionId
  const viewDetail = route.query.viewDetail === 'true'
  
  if (submissionId) {
    // 查看提交详情
    loadSubmissionDetail(submissionId)
  } else if (homeworkId && viewDetail) {
    // 直接查看作业的提交详情
    loadHomeworkById(homeworkId).then(() => {
      const homework = homeworkList.value.find(h => h.id === parseInt(homeworkId))
      if (homework) {
        // 选择作业后直接加载提交详情
        selectHomework(homework).then(() => {
          // 这里可以添加加载提交详情的逻辑
        })
      }
    })
  } else if (homeworkId) {
    // 加载指定的作业
    loadHomeworkById(homeworkId).then(() => {
      const homework = homeworkList.value.find(h => h.id === parseInt(homeworkId))
      if (homework) {
        selectHomework(homework)
      }
    })
  } else {
    loadHomeworkList()
  }
})

const isSubmissionGraded = (submission) => submission?.status === 'graded'

const canSubmitForm = computed(() => {
  return !isSubmissionGraded(existingSubmission.value)
})

const canModify = computed(() => {
  return submissionDetail.value?.allowModification === true
    && !isSubmissionGraded(submissionDetail.value)
})

const startModify = () => {
  if (isSubmissionGraded(submissionDetail.value)) {
    ElMessage.warning('已批改的作业不能修改')
    return
  }
  // 开始修改
  isModifying.value = true
  modifyForm.content = submissionDetail.value.content
  modifyFile.value = null
  modifyAttachmentDeleted.value = false
}

const cancelModify = () => {
  isModifying.value = false
  modifyForm.content = ''
  modifyFile.value = null
  modifyAttachmentDeleted.value = false
}

const handleModifyFileChange = (file) => {
  const error = validateSubmissionAttachment(file.raw)
  if (error) {
    ElMessage.error(error)
    return
  }
  modifyFile.value = file.raw
  modifyAttachmentDeleted.value = false
}

const handleModifyFileExceed = (files) => {
  handleModifyFileChange({ raw: files[0] })
}

const removeModifyFile = () => {
  // 删除新上传的附件
  modifyFile.value = null
  ElMessage.success('已移除选择的文件')
}

const deleteAttachment = () => {
  // 删除附件
  ElMessageBox.confirm(
    '确定要删除附件吗？删除后将无法恢复',
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    submissionDetail.value.filePath = null
    submissionDetail.value.fileName = null
    modifyFile.value = null
    modifyAttachmentDeleted.value = true
    ElMessage.success('附件已删除，请提交修改以保存')
  }).catch(() => {
    // 取消删除
  })
}

const handleModifySubmit = async () => {
  if (!modifyForm.content.trim()) {
    ElMessage.warning('请输入作业内容')
    return
  }

  if (isSubmissionGraded(submissionDetail.value)) {
    ElMessage.warning('已批改的作业不能修改')
    return
  }
  
  if (!submissionDetail.value) {
    ElMessage.error('提交记录不存在')
    return
  }
  
  modifying.value = true
  
  try {
    // 使用 updateSubmission API 来更新提交，支持删除附件
    await updateSubmission(
      submissionDetail.value.id,
      modifyForm.content,
      modifyFile.value,
      modifyAttachmentDeleted.value && !modifyFile.value
    )
    ElMessage.success('修改成功')
    // 重新加载提交详情
    await loadSubmissionDetail(submissionDetail.value.id)
    isModifying.value = false
  } catch (e) {
    console.error('修改作业失败:', e)
    ElMessage.error('修改作业失败')
  } finally {
    modifying.value = false
  }
}

const loadSubmissionDetail = async (id) => {
  try {
    const res = await getSubmissionById(id)
    if (res.data) {
      submissionDetail.value = res.data
    } else {
      ElMessage.error('未找到提交记录')
    }
  } catch (e) {
    console.error('加载提交详情失败:', e)
    ElMessage.error('加载提交详情失败')
  }
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/custom.css';

.submit-homework-page {
  max-width: 900px;
  margin: 0 auto;
}

.homework-list-card,
.submission-form-card,
.submission-detail-card {
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

.info-list {
  margin-top: 20px;
}

.info-item {
  display: flex;
  margin-bottom: 15px;
  align-items: flex-start;
  
  label {
    width: 100px;
    font-weight: 500;
    color: #666;
    flex-shrink: 0;
  }
  
  span {
    flex: 1;
    color: #333;
  }
  
  &.actions {
    align-items: center;
  }
}

.homework-item {
  margin-bottom: 20px;
}

.divider {
  height: 1px;
  background-color: #e0e0e0;
  margin: 20px 0;
}

.description {
  line-height: 1.6;
  white-space: pre-wrap;
  color: #555;
}

.content-display {
  flex: 1;
  background: #f9f9f9;
  padding: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  min-height: 100px;
  white-space: pre-wrap;
  line-height: 1.6;
  color: #333;
}

.description-display {
  flex: 1;
  background: #f0f9ff;
  padding: 15px;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  min-height: 80px;
  white-space: pre-wrap;
  line-height: 1.6;
  color: #333;
}

.feedback-display {
  flex: 1;
  background: #f9f9f9;
  padding: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  min-height: 80px;
  white-space: pre-wrap;
  line-height: 1.6;
  color: #333;
}

.feedback {
  line-height: 1.6;
  white-space: pre-wrap;
  color: #555;
}

.file-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  
  .el-icon {
    color: #409EFF;
    font-size: 20px;
  }
  
  .file-name {
    flex: 1;
    color: #333;
  }
}

.existing-submission {
  margin-bottom: 25px;
}

.selected-file-info {
  margin-top: 10px;
  padding: 8px 12px;
  background: #f5f5f5;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
  
  .file-size {
    color: #999;
  }
}

.custom-form {
  margin-top: 20px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

.content-textarea {
  width: 100%;
  box-sizing: border-box;
}

.upload-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.upload-button {
  margin-bottom: 5px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
  margin-bottom: 10px;
}

.selected-file-info {
  margin-top: 10px;
  padding: 12px 15px;
  background: #f5f5f5;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #666;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  
  .file-name {
    flex: 1;
    color: #333;
    font-weight: 500;
  }
  
  .file-size {
    color: #999;
    font-size: 13px;
  }
}

.form-actions {
  display: flex;
  gap: 15px;
  margin-top: 25px !important;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.action-button {
  padding: 8px 24px;
  font-size: 14px;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #999;
  
  p {
    margin-top: 10px;
  }
}
</style>
