<template>
  <div class="grade-list-container">
    <el-card class="grade-card">
      <template #header>
        <div class="card-header">
          <h2>作业批改</h2>
          <div class="filter-section">
            <el-select v-model="filters.homeworkId" placeholder="选择作业" clearable @change="handleFilterChange" style="width: 200px">
              <el-option 
                v-for="hw in homeworkList" 
                :key="hw.id" 
                :label="hw.title"
                :value="hw.id" 
              />
            </el-select>
            <el-select v-model="filters.status" placeholder="批改状态" clearable @change="handleFilterChange" style="width: 120px">
              <el-option label="已批改" value="graded" />
              <el-option label="未批改" value="submitted" />
            </el-select>
            <el-button type="primary" @click="loadSubmissions">
              <el-icon><Search /></el-icon> 筛选
            </el-button>
            <el-button @click="resetFilters">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
            <el-button @click="$router.back()">
              <el-icon><ArrowLeft /></el-icon> 返回
            </el-button>
          </div>
        </div>
      </template>
      
      <div v-if="loading" class="loading-container">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      
      <div v-else-if="submissionList.length === 0" class="empty-state">
        <el-icon :size="64"><Document /></el-icon>
        <p>暂无待批改作业</p>
      </div>
      
      <el-table v-else :data="submissionList" style="width: 100%" stripe>
        <el-table-column prop="homeworkTitle" label="作业标题" show-overflow-tooltip min-width="200" />
        <el-table-column prop="studentName" label="学生" width="120" />
        <el-table-column prop="submittedAt" label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.submittedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'graded' ? 'success' : 'warning'" effect="light">
              {{ row.status === 'graded' ? '已批改' : '待批改' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分数" width="80">
          <template #default="{ row }">
            <span v-if="row.score">{{ row.score }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small"
              @click="gradeSubmission(row.id)"
              :icon="row.status === 'graded' ? 'Edit' : 'Check'"
            >
              {{ row.status === 'graded' ? '修改' : '批改' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div v-if="submissionList.length > 0" class="pagination-section">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadSubmissions"
          @current-change="loadSubmissions"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getAllSubmissionsForTeacher, gradeSubmission as gradeSubmissionApi } from '@/api/submission'
import { getHomeworkByTeacher } from '@/api/homework'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Loading, Document, Check, Edit, ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const submissionList = ref([])
const homeworkList = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const filters = reactive({
  homeworkId: null,
  status: ''
})

// 保存筛选状态到 localStorage
const saveFilters = () => {
  localStorage.setItem('gradeListFilters', JSON.stringify({
    homeworkId: filters.homeworkId,
    status: filters.status,
    page: pagination.page,
    pageSize: pagination.pageSize
  }))
}

// 从 localStorage 恢复筛选状态
const restoreFilters = () => {
  const saved = localStorage.getItem('gradeListFilters')
  if (saved) {
    try {
      const savedFilters = JSON.parse(saved)
      filters.homeworkId = savedFilters.homeworkId || null
      filters.status = savedFilters.status || ''
      pagination.page = savedFilters.page || 1
      pagination.pageSize = savedFilters.pageSize || 10
    } catch (e) {
      console.error('恢复筛选状态失败:', e)
    }
  }
}

// 清除保存的筛选状态
const clearSavedFilters = () => {
  localStorage.removeItem('gradeListFilters')
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const loadHomeworkList = async () => {
  try {
    const res = await getHomeworkByTeacher()
    if (res.code === 200) {
      homeworkList.value = res.data || []
    }
  } catch (error) {
    console.error('加载作业列表失败:', error)
  }
}

const handleFilterChange = () => {
  pagination.page = 1
  loadSubmissions()
}

const loadSubmissions = async () => {
  loading.value = true
  try {
    const res = await getAllSubmissionsForTeacher()
    if (res.code === 200) {
      let submissions = res.data || []

      if (filters.homeworkId) {
        submissions = submissions.filter(s => s.homeworkId === filters.homeworkId)
      }

      if (filters.status) {
        submissions = submissions.filter(s => s.status === filters.status)
      }

      const normalizedSubmissions = submissions.map(item => ({
        ...item,
        homeworkTitle: item.homeworkTitle || item.homework?.title || '未知作业',
        studentName: item.studentName || item.student?.name || item.student?.username || '未知学生'
      }))

      pagination.total = normalizedSubmissions.length
      const maxPage = Math.max(1, Math.ceil(pagination.total / pagination.pageSize) || 1)
      if (pagination.page > maxPage) {
        pagination.page = maxPage
      }
      const start = (pagination.page - 1) * pagination.pageSize
      submissionList.value = normalizedSubmissions.slice(start, start + pagination.pageSize)

      // 保存筛选状态
      saveFilters()
    }
  } catch (error) {
    ElMessage.error('加载提交列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.homeworkId = null
  filters.status = ''
  pagination.page = 1
  clearSavedFilters()
  loadSubmissions()
}

const gradeSubmission = (submissionId) => {
  sessionStorage.setItem('gradeListReturnFilters', JSON.stringify({
    homeworkId: filters.homeworkId,
    status: filters.status,
    page: pagination.page,
    pageSize: pagination.pageSize
  }))
  router.push(`/grade/${submissionId}`)
}

onMounted(async () => {
  await loadHomeworkList()

  const returnFilters = sessionStorage.getItem('gradeListReturnFilters')
  if (returnFilters) {
    try {
      const saved = JSON.parse(returnFilters)
      filters.homeworkId = saved.homeworkId || null
      filters.status = saved.status || ''
      pagination.page = saved.page || 1
      pagination.pageSize = saved.pageSize || 10
      sessionStorage.removeItem('gradeListReturnFilters')
      loadSubmissions()
      return
    } catch (e) {
      console.error('恢复返回状态失败:', e)
    }
  }

  const homeworkId = route.query.homeworkId
  if (homeworkId) {
    filters.homeworkId = parseInt(homeworkId)
  }

  loadSubmissions()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/custom.css';

.grade-list-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.grade-card {
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
      
      .filter-section {
        display: flex;
        gap: 10px;
        align-items: center;
      }
    }
  }
}

.loading-container {
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
  
  span {
    font-size: 16px;
    color: #666;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  
  .el-icon {
    color: #909399;
    margin-bottom: 16px;
  }
  
  p {
    color: #909399;
    font-size: 16px;
    margin: 0;
  }
}

.pagination-section {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

:deep(.el-table) {
  margin-top: 20px;
  
  .el-table__header-wrapper th {
    background-color: #f9f9f9;
    font-weight: bold;
  }
  
  .el-table__body-wrapper tr:hover {
    background-color: #f5f7fa;
  }
}
</style>
