<template>
  <div class="page-container">
    <div class="grade-submission-page">
      <el-card class="submission-info-card">
        <template #header>
          <div class="card-header">
            <h2>批改作业</h2>
            <div class="header-actions">
              <el-button type="info" @click="checkPlagiarism" plain :loading="checkingPlagiarism">
                <el-icon><Search /></el-icon> 查重
              </el-button>
              <el-button @click="$router.back()" plain>
                <el-icon><ArrowLeft /></el-icon> 返回
              </el-button>
            </div>
          </div>
        </template>
        
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        
        <!-- 错误提示 -->
        <div v-else-if="error" class="error-container">
          <el-icon class="error-icon"><Warning /></el-icon>
          <span>{{ error }}</span>
          <el-button type="primary" size="small" @click="loadSubmission">重试</el-button>
        </div>
        
        <div v-else class="content-container">
          <div class="student-info">
            <div class="info-section">
              <h4>作业信息</h4>
              <div class="info-grid">
                <div class="info-item">
                  <label>学生姓名：</label>
                  <span class="student-name">{{ submission.studentName }}</span>
                </div>
                <div class="info-item">
                  <label>提交时间：</label>
                  <span>{{ formatDate(submission.submittedAt) }}</span>
                </div>
                <div class="info-item">
                  <label>批改状态：</label>
                  <el-tag :type="submission.status === 'graded' ? 'success' : 'warning'" effect="light">
                    {{ submission.status === 'graded' ? '已批改' : '待批改' }}
                  </el-tag>
                </div>
                <div class="info-item" v-if="submission.status === 'graded'">
                  <label>当前成绩：</label>
                  <el-tag type="success" effect="light">{{ submission.score }} / {{ submission.maxScore || 100 }}</el-tag>
                </div>
                <div class="info-item" v-if="submission.homeworkTitle">
                  <label>作业标题：</label>
                  <span>{{ submission.homeworkTitle }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="submission-content-section">
            <h4>作业内容</h4>
            <div class="content-display">
              {{ submission.content }}
            </div>
            <div v-if="submission.filePath" class="attachment-section">
              <h4>附件</h4>
              <el-button type="info" size="small" @click="downloadFile">
                <el-icon><Download /></el-icon> 下载附件
              </el-button>
              <span class="file-name">{{ submission.fileName || getFileNameFromPath(submission.filePath) }}</span>
            </div>
          </div>
        </div>
      </el-card>
      
      <el-card class="grade-form-card">
        <template #header>
          <h2>评分与评语</h2>
        </template>

        <el-form
          ref="gradeFormRef"
          :model="gradeForm"
          label-width="100px"
          class="grade-form"
        >
          <el-form-item label="评分" required>
            <div class="score-input">
              <el-input-number
                v-model="gradeForm.score"
                :min="0"
                :max="submission.maxScore || 100"
                :step="1"
                style="width: 150px"
              />
              <span class="score-max">/ {{ submission.maxScore || 100 }} 分</span>
            </div>
          </el-form-item>

          <el-form-item label="评语">
            <el-input
              v-model="gradeForm.feedback"
              type="textarea"
              :rows="6"
              placeholder="请输入批改评语（选填）..."
              resize="vertical"
            />
          </el-form-item>

          <el-form-item class="form-actions">
            <el-button type="primary" @click="handleSubmit" :loading="submitting" :icon="Check">
              提交批改
            </el-button>
            <el-button @click="$router.back()" :icon="ArrowLeft">
              取消
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-dialog v-model="plagiarismDialogVisible" title="作业查重分析" width="850px" :close-on-click-modal="false" class="plagiarism-dialog">
        <template #header>
          <div class="dialog-header">
            <div class="dialog-title">
              <el-icon><Search /></el-icon>
              <span>作业查重分析</span>
            </div>
          </div>
        </template>

        <template #footer>
          <el-button type="primary" @click="plagiarismDialogVisible = false">确 定</el-button>
        </template>

        <div v-if="checkingPlagiarism" class="dialog-loading">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <span>正在进行查重分析，请稍候...</span>
        </div>

        <div v-else-if="plagiarismResults.length === 0" class="dialog-empty">
          <el-icon class="empty-icon"><Document /></el-icon>
          <p class="empty-title">暂无其他提交记录</p>
          <p class="empty-desc">当前作业没有其他学生的提交可供对比</p>
        </div>

        <div v-else class="plagiarism-content">
          <div class="plagiarism-summary">
            <el-row :gutter="20">
              <el-col :span="8">
                <div class="stat-card">
                  <div class="stat-value">{{ plagiarismResults.length }}</div>
                  <div class="stat-label">对比学生数</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-card" :class="maxSimilarity > 50 ? 'danger' : maxSimilarity > 30 ? 'warning' : 'normal'">
                  <div class="stat-value">{{ maxSimilarity }}%</div>
                  <div class="stat-label">最高相似度</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-card highlight">
                  <div class="stat-value">
                    <el-tag :type="maxSimilarity > 70 ? 'danger' : maxSimilarity > 50 ? 'warning' : maxSimilarity > 30 ? 'info' : 'success'" size="large">
                      {{ maxSimilarity > 70 ? '高危' : maxSimilarity > 50 ? '警告' : maxSimilarity > 30 ? '注意' : '正常' }}
                    </el-tag>
                  </div>
                  <div class="stat-label">风险等级</div>
                </div>
              </el-col>
            </el-row>
          </div>

          <div class="plagiarism-list">
            <div class="list-header">
              <span>相似学生列表</span>
              <span class="count">共 {{ plagiarismResults.length }} 条记录</span>
            </div>
            <div class="list-body">
              <div
                v-for="(item, index) in plagiarismResults"
                :key="item.submissionId"
                class="plagiarism-item"
                :class="{ 'high-risk': item.similarity >= 70, 'medium-risk': item.similarity >= 50 && item.similarity < 70 }"
              >
                <div class="item-index">{{ index + 1 }}</div>
                <div class="item-info">
                  <div class="item-name">{{ item.studentName }}</div>
                  <div class="item-meta">共同词汇 {{ item.commonWords }} 个 · 总词汇 {{ item.totalWords }} 个</div>
                </div>
                <div class="item-similarity">
                  <el-progress
                    :percentage="item.similarity"
                    :color="getProgressColor(item.similarity)"
                    :stroke-width="10"
                    :show-text="true"
                    :format="(val) => val + '%'"
                    style="width: 120px;"
                  />
                </div>
                <div class="item-action">
                  <el-button type="primary" size="small" plain @click="showComparison(item)">
                    对比详情
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-dialog>

      <el-dialog v-model="comparisonDialogVisible" title="详细对比" width="1000px" :close-on-click-modal="false" class="comparison-dialog">
        <template #header>
          <div class="comparison-dialog-header">
            <div class="header-left">
              <el-icon><Search /></el-icon>
              <span>作业相似度详细对比</span>
            </div>
            <div class="header-right">
              <el-tag :type="getSimilarityType(selectedComparison?.similarity || 0)" size="large">
                相似度: {{ selectedComparison?.similarity }}%
              </el-tag>
            </div>
          </div>
        </template>

        <template #footer>
          <el-button type="primary" @click="comparisonDialogVisible = false">关 闭</el-button>
        </template>

        <div v-if="selectedComparison" class="comparison-content">
          <div class="comparison-stats-panel">
            <el-row :gutter="16">
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-icon danger"><Document /></div>
                  <div class="stat-info">
                    <div class="stat-number">{{ selectedComparison.similarity }}%</div>
                    <div class="stat-text">余弦相似度</div>
                  </div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-icon warning"><Tickets /></div>
                  <div class="stat-info">
                    <div class="stat-number">{{ selectedComparison.commonWords }}</div>
                    <div class="stat-text">共同词汇数</div>
                  </div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-icon info"><Collection /></div>
                  <div class="stat-info">
                    <div class="stat-number">{{ selectedComparison.totalWords }}</div>
                    <div class="stat-text">总词汇数</div>
                  </div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-icon" :class="getSimilarityType(selectedComparison.similarity)"><Warning /></div>
                  <div class="stat-info">
                    <div class="stat-number">{{ selectedComparison.similarity >= 70 ? '高危' : selectedComparison.similarity >= 50 ? '警告' : selectedComparison.similarity >= 30 ? '注意' : '正常' }}</div>
                    <div class="stat-text">风险等级</div>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>

          <div class="comparison-body">
            <div class="panel-left">
              <div class="panel-header">
                <div class="student-badge current">
                  <el-avatar :size="32" :icon="UserFilled" />
                  <span>当前学生</span>
                </div>
                <div class="student-name">{{ submission.studentName }}</div>
              </div>
              <div class="content-panel">
                <div class="panel-label">作业内容</div>
                <div class="content-text">{{ submission.content }}</div>
              </div>
            </div>

            <div class="panel-divider">
              <el-icon><DArrowLeft /></el-icon>
              <span>VS</span>
              <el-icon><DArrowRight /></el-icon>
            </div>

            <div class="panel-right">
              <div class="panel-header">
                <div class="student-badge compare">
                  <el-avatar :size="32" :icon="UserFilled" />
                  <span>对比学生</span>
                </div>
                <div class="student-name">{{ selectedComparison.studentName }}</div>
              </div>
              <div class="content-panel">
                <div class="panel-label">作业内容</div>
                <div class="content-text">{{ selectedComparison.content }}</div>
              </div>
            </div>
          </div>

          <div v-if="selectedComparison.commonWordList && selectedComparison.commonWordList.length > 0" class="common-words-panel">
            <div class="panel-title">
              <el-icon><Connection /></el-icon>
              <span>共同词汇分析</span>
              <span class="word-count">共 {{ selectedComparison.commonWordList.length }} 个</span>
            </div>
            <div class="words-cloud">
              <el-tag
                v-for="word in selectedComparison.commonWordList.slice(0, 50)"
                :key="word"
                size="default"
                :type="selectedComparison.similarity >= 70 ? 'danger' : selectedComparison.similarity >= 50 ? 'warning' : 'info'"
                class="word-tag"
              >
                {{ word }}
              </el-tag>
              <el-tag v-if="selectedComparison.commonWordList.length > 50" size="default" type="info">
                +{{ selectedComparison.commonWordList.length - 50 }} 更多
              </el-tag>
            </div>
          </div>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getSubmissionById, gradeSubmission, downloadAttachment, checkPlagiarism as checkPlagiarismApi, getSubmissionsByHomework } from '@/api/submission'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Download, Check, Loading, Warning, Search, Document, Tickets, Collection, Connection, DArrowLeft, DArrowRight, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const gradeFormRef = ref(null)
const submitting = ref(false)

const loading = ref(false)
const error = ref('')
const checkingPlagiarism = ref(false)
const plagiarismDialogVisible = ref(false)
const plagiarismResults = ref([])
const selectedComparison = ref(null)
const comparisonDialogVisible = ref(false)

const submission = ref({
  studentName: '',
  homeworkTitle: '',
  submittedAt: '',
  content: '',
  status: '',
  score: 0,
  feedback: '',
  filePath: '',
  fileName: ''
})

const gradeForm = reactive({
  score: 0,
  feedback: ''
})

const maxSimilarity = computed(() => {
  if (plagiarismResults.value.length === 0) return 0
  return Math.max(...plagiarismResults.value.map(s => s.similarity))
})

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const getFileNameFromPath = (filePath) => {
  if (!filePath) return '附件文件'
  const parts = filePath.split(/[\\/]/)
  return parts[parts.length - 1] || '附件文件'
}

const loadSubmission = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await getSubmissionById(route.params.submissionId)
    submission.value = res.data || res
    if (submission.value.status === 'graded') {
      gradeForm.score = submission.value.score || 0
      gradeForm.feedback = submission.value.feedback || ''
    }
  } catch (e) {
    console.error('加载提交失败:', e)
    error.value = '加载提交失败，请重试'
    ElMessage.error('加载提交失败')
  } finally {
    loading.value = false
  }
}

const downloadFile = () => {
  if (submission.value.filePath || submission.value.fileName) {
    downloadAttachment(submission.value.id)
  }
}

const handleSubmit = async () => {
  const maxScore = submission.value.maxScore || 100
  if (gradeForm.score < 0 || gradeForm.score > maxScore) {
    ElMessage.warning(`评分必须在 0-${maxScore} 之间`)
    return
  }

  submitting.value = true

  try {
    await ElMessageBox.confirm(
      `确定要提交批改吗？评分：${gradeForm.score} 分`,
      '确认批改',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await gradeSubmission(
      route.params.submissionId,
      gradeForm.score,
      gradeForm.feedback?.trim() || ''
    )
    ElMessage.success('批改成功')
    router.push('/grade')
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批改失败:', e)
      ElMessage.error('批改失败')
    }
  } finally {
    submitting.value = false
  }
}

const tokenize = (text) => {
  if (!text) return []
  const cleaned = text.replace(/[^\u4e00-\u9fa5a-zA-Z0-9]/g, ' ').toLowerCase().trim()
  const tokens = []

  if (/[\u4e00-\u9fa5]/.test(cleaned)) {
    for (let i = 0; i < cleaned.length; i++) {
      if (i < cleaned.length - 1) {
        const bigram = cleaned.substring(i, i + 2)
        if (!bigram.includes(' ') && /[\u4e00-\u9fa5a-zA-Z0-9]{2}/.test(bigram)) {
          tokens.push(bigram)
        }
      }
    }
    const words = cleaned.split(/\s+/)
    words.forEach(word => {
      if (word.length > 0) tokens.push(word)
    })
  } else {
    const words = cleaned.split(/\s+/)
    words.forEach(word => {
      if (word.length >= 2) {
        for (let i = 0; i < word.length - 1; i++) {
          tokens.push(word.substring(i, i + 2))
        }
      }
      if (word.length > 0) tokens.push(word)
    })
  }

  return tokens
}

const getTermFrequency = (tokens) => {
  const freq = {}
  tokens.forEach(token => {
    freq[token] = (freq[token] || 0) + 1
  })
  return freq
}

const cosineSimilarity = (text1, text2) => {
  const tokens1 = tokenize(text1)
  const tokens2 = tokenize(text2)

  if (tokens1.length === 0 || tokens2.length === 0) {
    return { similarity: 0, commonWords: 0, totalWords: 0, commonWordList: [] }
  }

  const freq1 = getTermFrequency(tokens1)
  const freq2 = getTermFrequency(tokens2)

  const allTerms = new Set([...Object.keys(freq1), ...Object.keys(freq2)])

  let dotProduct = 0
  let mag1 = 0
  let mag2 = 0
  const commonWords = new Set()

  allTerms.forEach(term => {
    const count1 = freq1[term] || 0
    const count2 = freq2[term] || 0
    if (count1 > 0 && count2 > 0) {
      commonWords.add(term)
    }
    dotProduct += count1 * count2
    mag1 += count1 * count1
    mag2 += count2 * count2
  })

  mag1 = Math.sqrt(mag1)
  mag2 = Math.sqrt(mag2)

  if (mag1 === 0 || mag2 === 0) {
    return { similarity: 0, commonWords: commonWords.size, totalWords: allTerms.size, commonWordList: Array.from(commonWords) }
  }

  const similarity = (dotProduct / (mag1 * mag2)) * 100
  return {
    similarity: Math.round(similarity * 100) / 100,
    commonWords: commonWords.size,
    totalWords: allTerms.size,
    commonWordList: Array.from(commonWords)
  }
}

const checkPlagiarism = async () => {
  if (!submission.value.homeworkId) {
    ElMessage.warning('作业信息不完整，无法查重')
    return
  }

  if (!submission.value.content || submission.value.content.trim().length < 10) {
    ElMessage.warning('当前作业内容太短，无法进行有效查重')
    return
  }

  checkingPlagiarism.value = true
  plagiarismResults.value = []

  try {
    const res = await getSubmissionsByHomework(submission.value.homeworkId)
    const allSubmissions = (res.data || res).filter(s => s.id !== submission.value.id && s.studentId !== submission.value.studentId)

    if (allSubmissions.length === 0) {
      ElMessage.info('暂无其他提交记录可供对比')
      plagiarismResults.value = []
      plagiarismDialogVisible.value = true
      return
    }

    const results = []
    const currentContent = submission.value.content

    for (const sub of allSubmissions) {
      if (!sub.content || sub.content.trim().length < 10) continue

      const similarity = cosineSimilarity(currentContent, sub.content)
      results.push({
        submissionId: sub.id,
        studentId: sub.studentId,
        studentName: sub.studentName || '未知学生',
        content: sub.content,
        similarity: similarity.similarity,
        commonWords: similarity.commonWords,
        totalWords: similarity.totalWords,
        commonWordList: similarity.commonWordList,
        submittedAt: sub.submittedAt
      })
    }

    results.sort((a, b) => b.similarity - a.similarity)
    plagiarismResults.value = results

    if (results.length > 0) {
      ElMessage.success(`查重完成，发现 ${results.length} 条相似记录`)
    } else {
      ElMessage.info('未发现其他有效提交记录')
    }

    plagiarismDialogVisible.value = true
  } catch (e) {
    console.error('查重失败:', e)
    ElMessage.error('查重失败')
  } finally {
    checkingPlagiarism.value = false
  }
}

const showComparison = (item) => {
  selectedComparison.value = item
  comparisonDialogVisible.value = true
}

const getSimilarityType = (score) => {
  if (score >= 70) return 'danger'
  if (score >= 50) return 'warning'
  if (score >= 30) return 'info'
  return 'success'
}

const getProgressColor = (percentage) => {
  if (percentage >= 70) return '#F56C6C'
  if (percentage >= 50) return '#E6A23C'
  if (percentage >= 30) return '#409EFF'
  return '#67C23A'
}

onMounted(() => {
  loadSubmission()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/custom.css';

.grade-submission-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 0;
}

.submission-info-card {
  margin-bottom: 20px;
  
  :deep(.el-card__header) {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      h2 {
        margin: 0;
        color: #4CAF50;
        font-size: 18px;
        font-weight: bold;
      }

      .header-actions {
        display: flex;
        gap: 10px;
      }
    }
  }
}

.student-info {
  margin-bottom: 25px;
  
  .info-section {
    h4 {
      color: #4CAF50;
      margin-bottom: 15px;
      font-size: 16px;
      font-weight: bold;
    }
  }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
  
  label {
    font-weight: bold;
    color: #555;
    margin-bottom: 5px;
    font-size: 14px;
  }
  
  span {
    color: #333;
    font-size: 14px;
  }
  
  .student-name {
    font-weight: bold;
    color: #4CAF50;
    font-size: 15px;
  }
}

.submission-content-section {
  margin-top: 30px;
  
  h4 {
    color: #4CAF50;
    margin-bottom: 12px;
    font-size: 16px;
    font-weight: bold;
  }
  
  .content-display {
    background: #f9f9f9;
    padding: 20px;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    min-height: 200px;
    white-space: pre-wrap;
    line-height: 1.6;
    font-size: 14px;
    color: #333;
    margin-bottom: 20px;
    box-shadow: inset 0 0 5px rgba(0, 0, 0, 0.05);
  }
  
  .attachment-section {
    padding: 15px;
    background: #f5f5f5;
    border-radius: 8px;
    border-left: 4px solid #409EFF;
    
    h4 {
      margin-bottom: 10px;
    }
    
    .file-name {
      margin-left: 10px;
      color: #666;
      font-size: 13px;
    }
  }
}

.grade-form-card {
  :deep(.el-card__header) h2 {
    margin: 0;
    color: #4CAF50;
    font-size: 18px;
    font-weight: bold;
  }
}

.grade-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
  
  .score-input {
    display: flex;
    align-items: center;
  }
  
  .score-max {
    margin-left: 10px;
    color: #999;
    font-size: 14px;
  }
  
  .form-actions {
    display: flex;
    gap: 10px;
    margin-top: 30px;
  }
}

.loading-container,
.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 10px;
  
  .loading-icon {
    font-size: 24px;
    color: #409EFF;
    animation: rotate 1s linear infinite;
  }
  
  .error-icon {
    font-size: 24px;
    color: #F56C6C;
  }
  
  span {
    font-size: 16px;
    color: #666;
  }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.content-container {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

:deep(.el-textarea) {
  :deep(.el-textarea__inner) {
    font-size: 14px;
    line-height: 1.6;
  }
}

:deep(.el-input-number) {
  :deep(.el-input__inner) {
    font-size: 14px;
  }
}

.dialog-loading,
.dialog-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 15px;
  color: #666;
  font-size: 16px;

  .empty-icon {
    font-size: 64px;
    color: #dcdfe6;
  }

  .empty-title {
    margin: 0;
    font-size: 18px;
    color: #303133;
  }

  .empty-desc {
    margin: 0;
    font-size: 14px;
    color: #909399;
  }

  .loading-icon {
    font-size: 32px;
    color: #409EFF;
    animation: rotate 1s linear infinite;
  }
}

.dialog-header {
  .dialog-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 500;
    color: #303133;

    .el-icon {
      font-size: 20px;
      color: #409EFF;
    }
  }
}

.plagiarism-content {
  .plagiarism-summary {
    margin-bottom: 24px;
  }
}

.stat-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ed 100%);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
    line-height: 1.2;
  }

  .stat-label {
    font-size: 13px;
    color: #909399;
    margin-top: 8px;
  }

  &.danger {
    background: linear-gradient(135deg, #fef0f0 0%, #fee2e2 100%);
    .stat-value { color: #F56C6C; }
  }

  &.warning {
    background: linear-gradient(135deg, #fdf6ec 0%, #f5e6d3 100%);
    .stat-value { color: #E6A23C; }
  }

  &.highlight {
    background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
  }
}

.plagiarism-list {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;

  .list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #f5f7fa;
    border-bottom: 1px solid #ebeef5;
    font-size: 14px;
    color: #606266;

    .count {
      color: #909399;
      font-size: 13px;
    }
  }

  .list-body {
    max-height: 400px;
    overflow-y: auto;
  }

  .plagiarism-item {
    display: flex;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #ebeef5;
    transition: background 0.2s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #f5f7fa;
    }

    &.high-risk {
      background: #fef0f0;
      border-left: 3px solid #F56C6C;

      &:hover {
        background: #fee2e2;
      }
    }

    &.medium-risk {
      background: #fdf6ec;
      border-left: 3px solid #E6A23C;

      &:hover {
        background: #f5e6d3;
      }
    }

    .item-index {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: #409EFF;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 500;
      margin-right: 16px;
    }

    .item-info {
      flex: 1;
      min-width: 0;

      .item-name {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        margin-bottom: 4px;
      }

      .item-meta {
        font-size: 12px;
        color: #909399;
      }
    }

    .item-similarity {
      margin: 0 20px;
    }

    .item-action {
      flex-shrink: 0;
    }
  }
}

.comparison-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 500;
    color: #303133;

    .el-icon {
      font-size: 20px;
      color: #409EFF;
    }
  }
}

.comparison-content {
  .comparison-stats-panel {
    margin-bottom: 24px;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 12px;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 12px;

      .stat-icon {
        width: 48px;
        height: 48px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;

        &.danger { background: #fef0f0; color: #F56C6C; }
        &.warning { background: #fdf6ec; color: #E6A23C; }
        &.info { background: #ecf5ff; color: #409EFF; }
        &.success { background: #f0f9eb; color: #67C23A; }
      }

      .stat-info {
        .stat-number {
          font-size: 20px;
          font-weight: 600;
          color: #303133;
          line-height: 1.2;
        }

        .stat-text {
          font-size: 12px;
          color: #909399;
          margin-top: 2px;
        }
      }
    }
  }

  .comparison-body {
    display: flex;
    align-items: stretch;
    gap: 16px;
    margin-bottom: 24px;

    .panel-left,
    .panel-right {
      flex: 1;
      display: flex;
      flex-direction: column;
      border: 1px solid #ebeef5;
      border-radius: 12px;
      overflow: hidden;

      .panel-header {
        padding: 12px 16px;
        background: #f5f7fa;
        border-bottom: 1px solid #ebeef5;

        .student-badge {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 12px;
          color: #909399;
          margin-bottom: 6px;

          &.current {
            color: #409EFF;
          }

          &.compare {
            color: #E6A23C;
          }
        }

        .student-name {
          font-size: 15px;
          font-weight: 600;
          color: #303133;
        }
      }

      .content-panel {
        flex: 1;
        padding: 16px;
        background: #fff;

        .panel-label {
          font-size: 12px;
          color: #909399;
          margin-bottom: 8px;
        }

        .content-text {
          font-size: 13px;
          line-height: 1.8;
          color: #606266;
          white-space: pre-wrap;
          word-break: break-word;
          max-height: 280px;
          overflow-y: auto;
        }
      }
    }

    .panel-divider {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      color: #c0c4cc;
      font-size: 14px;
      font-weight: 600;
      padding: 0 8px;
    }
  }

  .common-words-panel {
    border: 1px solid #ebeef5;
    border-radius: 12px;
    padding: 16px;
    background: #fafafa;

    .panel-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      font-weight: 500;
      color: #606266;
      margin-bottom: 12px;

      .el-icon {
        color: #409EFF;
      }

      .word-count {
        margin-left: auto;
        font-size: 12px;
        color: #909399;
        font-weight: normal;
      }
    }

    .words-cloud {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      .word-tag {
        padding: 4px 10px;
        font-size: 13px;
      }
    }
  }
}
</style>
