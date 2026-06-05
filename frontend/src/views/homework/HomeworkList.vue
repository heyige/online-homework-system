<template>
  <div class="page-container">
    <div class="homework-list-page">
      <el-card class="homework-card">
        <template #header>
          <div class="card-header">
            <h2>作业列表</h2>
            <div class="header-actions">
              <el-button 
                v-if="hasRole(['TEACHER'])",
                type="primary" 
                @click.stop="$router.push('/homework/create')"
              >
                <el-icon><Plus /></el-icon>
                添加作业
              </el-button>

              <el-dropdown v-if="hasRole(['TEACHER'])", :hide-on-click="false">
                <el-button type="danger">
                  批量操作<el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="startBatchDelete">
                      <el-icon><Delete /></el-icon> 批量删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </template>
        
        <div class="filter-section" v-if="userStore.role === 'TEACHER'">
          <el-tabs v-model="activeTab" @tab-change="handleTabChange">
            <el-tab-pane label="全部作业" name="all"></el-tab-pane>
            <el-tab-pane label="待发布" name="wait"></el-tab-pane>
            <el-tab-pane label="已发布" name="published"></el-tab-pane>
          </el-tabs>
          
          <div class="search-bar">
            <el-input 
              v-model="searchForm.courseName" 
              placeholder="搜索课程名称" 
              clearable
              prefix-icon="Search"
              style="width: 300px"
            />
            <el-select 
              v-model="searchForm.status" 
              placeholder="选择状态" 
              clearable
              style="width: 150px; margin-left: 15px"
            >
              <el-option label="待发布" value="wait" />
              <el-option label="已发布" value="published" />
            </el-select>
            <el-button type="primary" @click="loadHomeworkList" style="margin-left: 15px">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
            <el-button @click="resetSearch" style="margin-left: 10px">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
          </div>
        </div>
        
        <div class="filter-section" v-else-if="userStore.role === 'STUDENT'">
          <el-tabs v-model="studentActiveTab" @tab-change="handleStudentTabChange">
            <el-tab-pane label="全部作业" name="all"></el-tab-pane>
            <el-tab-pane label="待提交" name="pending"></el-tab-pane>
            <el-tab-pane label="已提交" name="submitted"></el-tab-pane>
          </el-tabs>
          
          <div class="search-bar">
            <el-input 
              v-model="searchForm.courseName" 
              placeholder="搜索课程名称" 
              clearable
              prefix-icon="Search"
              style="width: 300px"
            />
            <el-button type="primary" @click="loadHomeworkList" style="margin-left: 15px">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
            <el-button @click="resetStudentSearch" style="margin-left: 10px">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
          </div>
        </div>
        
        <div class="homework-list" v-loading="loading">
          <div v-if="homeworkList.length === 0" class="empty-state">
            <el-icon :size="64"><Document /></el-icon>
            <p>暂无作业</p>
          </div>
          
          <div v-else class="homework-items">
            <div
              v-for="homework in homeworkList"
              :key="homework.id"
              class="homework-item"
              :class="{ 'batch-delete-mode': batchDeleteMode }"
            >
              <div class="item-checkbox" v-if="isBatchMode">
                <el-checkbox v-model="homework.selected" />
              </div>
              <div class="item-content">
                <div class="item-header">
                  <h4 class="item-title">{{ homework.title }}</h4>
                  <div class="item-meta">
                    <el-tag :type="getStatusTag(homework.status)" size="small">
                      {{ getStatusLabel(homework.status) }}
                    </el-tag>
                    <span class="course-name">{{ homework.courseName }}</span>
                  </div>
                </div>
                
                <div class="item-body">
                  <div class="description">
                    {{ homework.description }}
                  </div>
                  <div class="meta-info">
                    <div class="meta-item">
                      <el-icon><User /></el-icon>
                      <span>教师：{{ homework.teacherName }}</span>
                    </div>
                    <div class="meta-item">
                      <el-icon><Clock /></el-icon>
                      <span>截止：{{ formatDate(homework.deadline) }}</span>
                    </div>
                    <div class="meta-item">
                      <el-icon><Coin /></el-icon>
                      <span>满分：{{ homework.maxScore }}</span>
                    </div>
                    <div class="meta-item" v-if="userStore.role === 'STUDENT'">
                      <el-icon><DocumentChecked /></el-icon>
                      <span :class="getSubmissionStatusClass(homework.submissionStatus)">
                        状态：{{ homework.submissionStatus || '未提交' }}
                      </span>
                    </div>
                    <div class="meta-item" v-if="userStore.role === 'STUDENT' && homework.score">
                      <el-icon><Trophy /></el-icon>
                      <span>得分：{{ homework.score }}</span>
                    </div>
                  </div>
                </div>
                
                <div class="item-actions">
                  <!-- 学生按钮 -->
                  <template v-if="userStore.role === 'STUDENT'">
                    <!-- 未提交状态 -->
                    <template v-if="!homework.submissionStatus || homework.submissionStatus === '未提交'">
                      <el-button 
                        v-if="isOverdue(homework.deadline)"
                        type="info" 
                        size="small"
                        disabled
                      >
                        <el-icon><Clock /></el-icon> 已截止
                      </el-button>
                      <el-button 
                        v-else
                        type="success" 
                        size="small"
                        @click="submitHomework(homework.id)"
                      >
                        <el-icon><Upload /></el-icon> 提交作业
                      </el-button>
                    </template>
                    
                    <!-- 已提交未批改状态 -->
                    <el-button 
                      v-else-if="homework.submissionStatus === '已提交'"
                      type="primary" 
                      size="small"
                      @click="viewSubmission(homework.id)"
                    >
                      <el-icon><View /></el-icon> 查看提交
                    </el-button>
                    
                    <!-- 已批改状态 -->
                    <el-button 
                      v-else-if="homework.submissionStatus === '已批改'"
                      type="success" 
                      size="small"
                      @click="viewSubmission(homework.id)"
                    >
                      <el-icon><Check /></el-icon> 查看结果
                    </el-button>
                  </template>
                  
                  <!-- 教师按钮 -->
                  <template v-if="userStore.role === 'TEACHER'">
                    <el-button type="primary" size="small" @click="viewDetail(homework.id)">
                      <el-icon><View /></el-icon> 查看详情
                    </el-button>
                    <el-button 
                      type="primary" 
                      size="small"
                      @click="editHomework(homework.id)"
                    >
                      <el-icon><Edit /></el-icon> 编辑
                    </el-button>
                    <el-button 
                      type="danger" 
                      size="small"
                      @click="deleteHomework(homework.id)"
                    >
                      <el-icon><Delete /></el-icon> 删除
                    </el-button>
                    <el-button 
                      type="success" 
                      size="small"
                      @click="$router.push('/grade?homeworkId=' + homework.id)"
                    >
                      <el-icon><Edit /></el-icon> 批改作业
                    </el-button>

                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
      
      <div v-if="isBatchMode" class="batch-operation-bar show delete-mode">
        <div class="left-section">
          <el-checkbox v-model="selectAll" @change="handleSelectAllChange" />
          <span>全选</span>
          <span class="selected-count">已选择 {{ selectedCount }} 项</span>
        </div>
        <div class="right-section">
          <span class="operation-mode mode-delete">批量删除</span>
          <el-button @click="cancelBatchOperation">取消</el-button>
          <el-button type="danger" @click="confirmBatchDelete" :disabled="selectedCount === 0">
            确定
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { hasRole } from '@/utils/permissions'
import { Plus, Edit, DataAnalysis, Delete, Search, Refresh, Document, User, Clock, Coin, View, Upload, ArrowDown, DocumentChecked, Trophy } from '@element-plus/icons-vue'
import { getHomeworkByStudent, getHomeworkByTeacher, deleteHomework as deleteHomeworkApi } from '@/api/homework'
import { getUserById } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('all')
const studentActiveTab = ref('all')
const homeworkList = ref([])
const loading = ref(false)
const searchForm = reactive({
  courseName: '',
  status: ''
})
const studentFilter = ref('all')

const batchDeleteMode = ref(false)
const selectAll = ref(false)
const batchOperationType = ref('delete')

const selectedCount = computed(() => {
  return homeworkList.value.filter(h => h.selected).length
})

const isBatchMode = computed(() => {
  return batchDeleteMode.value
})

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const getStatusTag = (status) => {
  const statusMap = {
    'wait': 'warning',
    'published': 'success'
  }
  return statusMap[status] || 'info'
}

const getStatusLabel = (status) => {
  const labelMap = {
    'wait': '待发布',
    'published': '已发布'
  }
  return labelMap[status] || status
}

const getSubmissionStatusClass = (status) => {
  if (!status) return 'status-unsubmitted'
  const statusClassMap = {
    '未提交': 'status-unsubmitted',
    '已提交': 'status-submitted',
    '已批改': 'status-graded'
  }
  return statusClassMap[status] || 'status-unsubmitted'
}

const loadHomeworkList = async () => {
  loading.value = true
  try {
    let res
    if (userStore.role === 'STUDENT') {
      // 学生只能调用学生接口获取作业列表
      res = await getHomeworkByStudent()
      // 过滤出已发布的作业（注意：StudentHomeworkDTO 中作业对象在 homework 属性中）
      let filteredHomework = (res.data || [])
        .filter(h => h.homework && h.homework.status === 'published')
        .map(h => ({
          ...h,
          // 将 homework 对象中的属性提升到顶层，方便模板访问
          id: h.homework.id,
          title: h.homework.title,
          description: h.homework.description,
          courseName: h.homework.courseName,
          teacherName: h.teacherName,
          deadline: h.homework.deadline,
          maxScore: h.homework.maxScore,
          status: h.homework.status,
          selected: false
        }))
      
      // 应用学生筛选条件
      if (studentFilter.value === 'pending') {
        filteredHomework = filteredHomework.filter(h => !h.submissionStatus || h.submissionStatus === '未提交')
      } else if (studentFilter.value === 'submitted') {
        filteredHomework = filteredHomework.filter(h => h.submissionStatus && (h.submissionStatus === '已提交' || h.submissionStatus === '已批改'))
      }
      
      // 应用课程名称搜索
      if (searchForm.courseName) {
        filteredHomework = filteredHomework.filter(h => h.courseName && h.courseName.includes(searchForm.courseName))
      }
      
      homeworkList.value = filteredHomework
    } else {
      // 教师和管理员调用教师接口获取作业列表，并传递筛选条件
      const params = {
        courseName: searchForm.courseName,
        status: searchForm.status
      }
      res = await getHomeworkByTeacher(params)
      // 教师只能看到自己发布的作业，直接使用当前用户的姓名作为教师姓名
      const currentUserName = userStore.user?.name || userStore.user?.username || ''
      console.log('Current user info:', userStore.user)
      console.log('Current user name:', currentUserName)
      
      homeworkList.value = (res.data || []).map(h => {
        let teacherName = currentUserName
        // 如果当前用户没有姓名，且作业对象有teacherId，则尝试根据teacherId查询教师信息
        if (!teacherName && h.teacherId) {
          console.log('Trying to get teacher info by teacherId:', h.teacherId)
          // 这里可以添加异步查询教师信息的逻辑
          // 为了避免页面加载缓慢，暂时使用占位符
          teacherName = '教师'
        }
        return {
          ...h,
          teacherName: teacherName,
          selected: false
        }
      })
    }
    
    if (batchDeleteMode.value) {
      selectAll.value = false
    }
  } catch (e) {
    console.error('加载作业列表失败:', e)
    ElMessage.error('加载作业列表失败：' + (e.message || '未知错误'))
    homeworkList.value = []
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.courseName = ''
  searchForm.status = ''
  activeTab.value = 'all'
  loadHomeworkList()
}

const resetStudentSearch = () => {
  searchForm.courseName = ''
  studentActiveTab.value = 'all'
  studentFilter.value = 'all'
  loadHomeworkList()
}

const handleTabChange = (tab) => {
  searchForm.status = tab === 'all' ? '' : tab
  loadHomeworkList()
}

const handleStudentTabChange = (tab) => {
  studentFilter.value = tab
  loadHomeworkList()
}

const viewDetail = (id) => {
  router.push(`/homework/${id}`)
}

const submitHomework = (id) => {
  // 跳转到提交页面，并传递作业 ID 作为查询参数
  router.push({
    path: '/submission',
    query: { homeworkId: id }
  })
}

const viewSubmission = (id) => {
  // 跳转到提交页面，传递作业 ID 作为查询参数，并添加查看详情的标志
  router.push({
    path: '/submission',
    query: { homeworkId: id, viewDetail: 'true' }
  })
}

const editHomework = (id) => {
  router.push(`/homework/${id}/edit`)
}

// 判断作业是否已截止
const isOverdue = (deadline) => {
  if (!deadline) return false
  return new Date(deadline) < new Date()
}

const deleteHomework = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该作业吗？', '提示', {
      type: 'warning'
    })
    
    await deleteHomeworkApi(id)
    ElMessage.success('删除成功')
    await loadHomeworkList()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败:', e)
      ElMessage.error('删除失败')
    }
  }
}

const startBatchDelete = () => {
  batchOperationType.value = 'delete'
  batchDeleteMode.value = true
  selectAll.value = false
  homeworkList.value.forEach(h => h.selected = false)
}

const cancelBatchOperation = () => {
  batchDeleteMode.value = false
  selectAll.value = false
  homeworkList.value.forEach(h => h.selected = false)
}

const handleSelectAllChange = (value) => {
  homeworkList.value.forEach(h => {
    h.selected = value
  })
}

const confirmBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedCount.value} 项作业吗？`,
      '提示',
      {
        type: 'warning'
      }
    )
    
    const selectedIds = homeworkList.value.filter(h => h.selected).map(h => h.id)
    for (const id of selectedIds) {
      await deleteHomeworkApi(id)
    }
    
    ElMessage.success('批量删除成功')
    cancelBatchOperation()
    await loadHomeworkList()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量删除失败:', e)
      ElMessage.error('批量删除失败')
    }
  }
}

onMounted(() => {
  loadHomeworkList()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/custom.css';

.homework-list-page {
  max-width: 1200px;
  margin: 0 auto;
}

.homework-card {
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
        align-items: center;
        
        .el-button {
          // 统一所有按钮的大小
          padding: 8px 15px;
          font-size: 14px;
        }
      }
    }
  }
}

.filter-section {
  margin-top: 15px;
  
  .search-bar {
    margin-top: 15px;
    display: flex;
    align-items: center;
  }
}

.homework-list {
  margin-top: 20px;
}

.homework-items {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.homework-item {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;
  border-left: 4px solid #4CAF50;
  display: flex;
  gap: 15px;
  position: relative;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
  }
  
  &.batch-delete-mode {
    padding-left: 50px;
    border-left-color: #f44336;
  }
  
  .item-checkbox {
    position: absolute;
    left: 15px;
    top: 20px;
    display: flex;
    align-items: center;
  }
  
  .item-content {
    flex: 1;
    
    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 15px;
      
      .item-title {
        color: #4CAF50;
        margin: 0;
        font-size: 18px;
        font-weight: bold;
        flex: 1;
      }
      
      .item-meta {
        display: flex;
        align-items: center;
        gap: 12px;
        
        .course-name {
          font-size: 14px;
          color: #666;
          padding: 2px 8px;
          background: #f5f5f5;
          border-radius: 4px;
        }
      }
    }
    
    .item-body {
      margin-bottom: 15px;
      
      .description {
        background: #f9f9f9;
        padding: 12px;
        border-radius: 6px;
        margin-bottom: 12px;
        white-space: pre-wrap;
        line-height: 1.6;
        color: #333;
        font-size: 14px;
        max-height: 100px;
        overflow-y: auto;
      }
      
      .meta-info {
        display: flex;
        gap: 20px;
        flex-wrap: wrap;
        
        .meta-item {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 13px;
          color: #666;
          
          .el-icon {
            color: #4CAF50;
          }
          
          .status-unsubmitted {
            color: #909399;
            font-weight: 500;
          }
          
          .status-submitted {
            color: #409EFF;
            font-weight: 500;
          }
          
          .status-graded {
            color: #67C23A;
            font-weight: 500;
          }
        }
      }
    }
    
    .item-actions {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }
  }
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
  
  &.delete-mode {
    border: 2px solid #f44336;
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
    }
  }
}

:deep(.el-tabs__header) {
  margin-bottom: 15px;
}

:deep(.el-form-item) {
  margin-bottom: 0;
}
</style>
