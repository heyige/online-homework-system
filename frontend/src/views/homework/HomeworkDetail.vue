<template>
  <div class="homework-detail-container">
    <el-card>
      <template #header>
        <span>作业详情</span>
      </template>
      
      <!-- 作业信息 -->
      <el-descriptions :column="2" border>
        <el-descriptions-item label="作业标题">{{ homework.title }}</el-descriptions-item>
        <el-descriptions-item label="课程">{{ homework.courseName }}</el-descriptions-item>
        <el-descriptions-item label="教师">{{ homework.teacherName }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">
          <span :class="{ 'is-expired': isExpired(homework.deadline) }">
            {{ formatDate(homework.deadline) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTag(homework.status)">
            {{ getStatusLabel(homework.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="满分">{{ homework.maxScore }}</el-descriptions-item>
        <el-descriptions-item label="允许修改">
          {{ homework.allowModification ? '是' : '否' }}
        </el-descriptions-item>
        <el-descriptions-item label="作业描述" :span="2">
          <div style="white-space: pre-wrap">{{ homework.description }}</div>
        </el-descriptions-item>
      </el-descriptions>
      
      <!-- 学生提交信息 -->
      <template v-if="userStore.role === 'STUDENT' && submission">
        <el-divider />
        <h3>我的提交</h3>
        
        <el-alert
          :title="submission.status === 'graded' ? '已批改' : '待批改'"
          :type="submission.status === 'graded' ? 'success' : 'warning'"
          :closable="false"
          style="margin-bottom: 20px"
        />
        
        <el-descriptions :column="2" border>
          <el-descriptions-item label="提交时间">
            {{ formatDate(submission.submittedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="submission.status === 'graded' ? 'success' : 'warning'">
              {{ submission.status === 'graded' ? '已批改' : '待批改' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="submission.score" label="得分">
            <span style="color: #67C23A; font-weight: bold; font-size: 18px">
              {{ submission.score }} / {{ homework.maxScore }}
            </span>
          </el-descriptions-item>
        </el-descriptions>
        
        <!-- 作业内容 -->
        <div class="submission-content" v-if="submission.content">
          <h4>作业内容</h4>
          <el-card class="content-card">
            <div style="white-space: pre-wrap; line-height: 1.8;">{{ submission.content }}</div>
          </el-card>
        </div>
        
        <!-- 附件下载 -->
        <div class="submission-files" v-if="submission.filePath || submission.fileName">
          <h4>附件</h4>
          <el-card class="file-card">
            <div class="file-item">
              <el-icon><Document /></el-icon>
              <span class="file-name">{{ submission.fileName || getFileNameFromPath(submission.filePath) }}</span>
              <el-button type="primary" size="small" @click="downloadFile(submission)">
                <el-icon><Download /></el-icon> 下载
              </el-button>
            </div>
          </el-card>
        </div>
        
        <!-- 教师反馈 -->
        <div class="teacher-feedback" v-if="submission.feedback">
          <h4>教师反馈</h4>
          <el-card class="feedback-card">
            <div style="white-space: pre-wrap; line-height: 1.8;">{{ submission.feedback }}</div>
          </el-card>
        </div>
      </template>
      
      <!-- 学生未提交时的提示 -->
      <template v-if="userStore.role === 'STUDENT' && !submission">
        <el-divider />
        <el-alert
          title="尚未提交"
          description="您还没有提交这份作业，请尽快提交"
          type="warning"
          :closable="false"
        >
          <template #default>
            <div style="margin-top: 15px; text-align: right;">
              <el-button type="primary" @click="submitHomework">
                <el-icon><Upload /></el-icon> 立即提交
              </el-button>
            </div>
          </template>
        </el-alert>
      </template>
      
      <el-divider />
      
      <div class="action-buttons">
        <el-button type="primary" @click="$router.back()">返回</el-button>
        <el-button 
          v-if="userStore.role === 'STUDENT' && !submission && !isExpired(homework.deadline)"
          type="success" 
          @click="submitHomework"
        >
          <el-icon><Upload /></el-icon> 提交作业
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getHomeworkById } from '@/api/homework'
import { getSubmissionByHomeworkForStudent, downloadAttachment } from '@/api/submission'
import { ElMessage } from 'element-plus'
import { Document, Download, Upload } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const homework = ref({})
const submission = ref(null)

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const isExpired = (deadline) => {
  if (!deadline) return false
  return new Date(deadline) < new Date()
}

const getStatusTag = (status) => {
  const statusMap = {
    'published': '',
    'wait': 'warning',
    'expired': 'info',
    'withdrawn': 'danger'
  }
  return statusMap[status] || ''
}

const getStatusLabel = (status) => {
  const labelMap = {
    'published': '已发布',
    'wait': '待发布',
    'expired': '已过期',
    'withdrawn': '已撤回'
  }
  return labelMap[status] || status
}

const loadHomework = async () => {
  try {
    const res = await getHomeworkById(route.params.id)
    homework.value = res.data
    
    // 如果是学生，加载该学生在当前作业的提交记录
    if (userStore.role === 'STUDENT') {
      await loadStudentSubmission()
    }
  } catch (e) {
    console.error('加载作业详情失败:', e)
    ElMessage.error('加载作业详情失败')
  }
}

const loadStudentSubmission = async () => {
  try {
    // 使用学生专用的 API，获取当前学生在指定作业的提交
    const res = await getSubmissionByHomeworkForStudent(homework.value.id)
    if (res.data) {
      submission.value = res.data
    }
  } catch (e) {
    console.error('加载提交记录失败:', e)
    // 404 表示没有提交记录，这是正常情况
    submission.value = null
  }
}

const submitHomework = () => {
  router.push({
    path: '/submission',
    query: { homeworkId: homework.value.id }
  })
}

const getFileNameFromPath = (filePath) => {
  if (!filePath) return '附件文件'
  // 从文件路径中提取文件名
  const parts = filePath.split(/[\\/]/)
  return parts[parts.length - 1] || '附件文件'
}

const downloadFile = (submission) => {
  if (!submission.filePath && !submission.fileName) {
    ElMessage.warning('没有可下载的文件')
    return
  }
  
  downloadAttachment(submission.id)
}

onMounted(() => {
  loadHomework()
})
</script>

<style lang="scss" scoped>
.homework-detail-container {
  padding: 20px;
  
  .is-expired {
    color: #f56c6c;
    font-weight: bold;
  }
  
  h3 {
    color: #4CAF50;
    margin: 20px 0 15px;
    font-size: 18px;
  }
  
  h4 {
    color: #333;
    margin: 15px 0 10px;
    font-size: 16px;
  }
  
  .submission-content,
  .submission-files,
  .teacher-feedback {
    margin-top: 20px;
  }
  
  .content-card,
  .file-card,
  .feedback-card {
    background: #f9f9f9;
    margin-bottom: 15px;
  }
  
  .file-item {
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
  
  .action-buttons {
    display: flex;
    gap: 10px;
    margin-top: 20px;
  }
}
</style>
