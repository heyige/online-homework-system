<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" :class="userStore.role === 'ADMIN' ? 'users' : 'homework'">
              <el-icon><component :is="userStore.role === 'ADMIN' ? 'UserFilled' : 'Document'" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ userStore.role === 'ADMIN' ? adminStats.userCount : stats.homeworkCount }}</div>
              <div class="stat-label">{{ userStore.role === 'ADMIN' ? '用户总数' : '作业总数' }}</div>
              <div v-if="userStore.role === 'ADMIN'" class="stat-sub">
                教师 {{ adminStats.teacherCount }} · 学生 {{ adminStats.studentCount }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon submission">
              <el-icon><component :is="userStore.role === 'ADMIN' ? 'Document' : 'Upload'" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ userStore.role === 'ADMIN' ? stats.homeworkCount : stats.submissionCount }}</div>
              <div class="stat-label">{{ userStore.role === 'ADMIN' ? '作业总数' : (userStore.role === 'TEACHER' ? '收到提交' : '提交次数') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon graded">
              <el-icon><component :is="userStore.role === 'ADMIN' ? 'DocumentCopy' : 'Edit'" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ userStore.role === 'ADMIN' ? adminStats.pendingCount : stats.gradedCount }}</div>
              <div class="stat-label">{{ userStore.role === 'ADMIN' ? '待审批申请' : '已批改' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon message">
              <el-icon><Bell /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.unreadCount }}</div>
              <div class="stat-label">未读消息</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="userStore.role === 'ADMIN'" :gutter="16" class="admin-quick-actions">
      <el-col :span="24">
        <el-card>
          <div class="quick-actions">
            <span class="quick-title">快捷操作</span>
            <el-button type="primary" @click="$router.push('/users')">
              <el-icon><UserFilled /></el-icon> 用户管理
            </el-button>
            <el-button type="warning" @click="$router.push('/profile-requests')">
              <el-icon><DocumentCopy /></el-icon> 申请审批
              <el-badge v-if="adminStats.pendingCount > 0" :value="adminStats.pendingCount" class="action-badge" />
            </el-button>
            <el-button @click="$router.push('/messages')">
              <el-icon><Bell /></el-icon> 消息通知
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="userStore.role === 'TEACHER'" :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card v-loading="statsLoading">
          <template #header>
            <div class="card-header">
              <span>作业统计</span>
              <el-select
                v-model="selectedHomeworkId"
                placeholder="选择作业"
                clearable
                filterable
                style="width: 320px"
                @change="loadHomeworkStatistics"
              >
                <el-option
                  v-for="hw in teacherHomeworkList"
                  :key="hw.id"
                  :label="`${hw.title}${hw.courseName ? '（' + hw.courseName + '）' : ''}`"
                  :value="hw.id"
                />
              </el-select>
            </div>
          </template>

          <el-empty v-if="!selectedHomeworkId" description="请选择作业查看统计情况" />

          <template v-else-if="homeworkStats">
            <el-descriptions :column="4" border class="homework-summary">
              <el-descriptions-item label="作业标题">{{ homeworkStats.homeworkTitle || '-' }}</el-descriptions-item>
              <el-descriptions-item label="课程">{{ homeworkStats.courseName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="截止时间">{{ formatDate(homeworkStats.deadline) || '-' }}</el-descriptions-item>
              <el-descriptions-item label="满分">{{ homeworkStats.homeworkMaxScore ?? '-' }}</el-descriptions-item>
            </el-descriptions>

            <el-row :gutter="16" class="mini-stats">
              <el-col :span="4">
                <div class="mini-stat-item">
                  <div class="mini-stat-value">{{ homeworkStats.assignedStudentCount || 0 }}</div>
                  <div class="mini-stat-label">分配学生</div>
                </div>
              </el-col>
              <el-col :span="4">
                <div class="mini-stat-item">
                  <div class="mini-stat-value">{{ homeworkStats.submittedCount || 0 }}</div>
                  <div class="mini-stat-label">已提交</div>
                </div>
              </el-col>
              <el-col :span="4">
                <div class="mini-stat-item">
                  <div class="mini-stat-value warning">{{ homeworkStats.unsubmittedCount || 0 }}</div>
                  <div class="mini-stat-label">未提交</div>
                </div>
              </el-col>
              <el-col :span="4">
                <div class="mini-stat-item">
                  <div class="mini-stat-value success">{{ homeworkStats.gradedCount || 0 }}</div>
                  <div class="mini-stat-label">已批改</div>
                </div>
              </el-col>
              <el-col :span="4">
                <div class="mini-stat-item">
                  <div class="mini-stat-value primary">{{ homeworkStats.pendingGradeCount || 0 }}</div>
                  <div class="mini-stat-label">待批改</div>
                </div>
              </el-col>
              <el-col :span="4">
                <div class="mini-stat-item">
                  <div class="mini-stat-value">{{ homeworkStats.completionRate || 0 }}%</div>
                  <div class="mini-stat-label">提交率</div>
                </div>
              </el-col>
            </el-row>

            <el-row :gutter="16" class="mini-stats score-stats">
              <el-col :span="8">
                <div class="mini-stat-item">
                  <div class="mini-stat-value">{{ homeworkStats.averageScore || 0 }}</div>
                  <div class="mini-stat-label">平均分</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="mini-stat-item">
                  <div class="mini-stat-value success">{{ homeworkStats.maxScore || 0 }}</div>
                  <div class="mini-stat-label">最高分</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="mini-stat-item">
                  <div class="mini-stat-value">{{ homeworkStats.minScore || 0 }}</div>
                  <div class="mini-stat-label">最低分</div>
                </div>
              </el-col>
            </el-row>

            <el-table
              :data="homeworkStats.studentDetails || []"
              stripe
              style="width: 100%"
              empty-text="暂无分配学生"
            >
              <el-table-column prop="studentName" label="学生" min-width="120" />
              <el-table-column prop="status" label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="getStudentStatusType(row.statusCode)" effect="light">
                    {{ row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="提交时间" min-width="170">
                <template #default="{ row }">
                  {{ row.submittedAt ? formatDate(row.submittedAt) : '-' }}
                </template>
              </el-table-column>
              <el-table-column label="得分" width="100">
                <template #default="{ row }">
                  <span v-if="row.score != null">{{ row.score }}</span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="row.submissionId"
                    type="primary"
                    link
                    @click="$router.push(`/grade/${row.submissionId}`)"
                  >
                    {{ row.statusCode === 'graded' ? '查看' : '批改' }}
                  </el-button>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="userStore.role === 'ADMIN' ? 12 : 12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ userStore.role === 'ADMIN' ? '待审批申请' : '最近作业' }}</span>
              <el-button
                v-if="userStore.role === 'ADMIN'"
                text
                type="primary"
                @click="$router.push('/profile-requests')"
              >
                前往审批
              </el-button>
              <el-button
                v-else-if="userStore.role !== 'STUDENT'"
                text
                type="primary"
                @click="$router.push('/homework')"
              >
                查看全部
              </el-button>
              <el-button
                v-else
                text
                type="primary"
                @click="$router.push('/homework')"
              >
                查看全部
              </el-button>
            </div>
          </template>

          <!-- 管理员：待审批列表 -->
          <template v-if="userStore.role === 'ADMIN'">
            <el-empty v-if="pendingRequests.length === 0" description="暂无待审批申请" />
            <div v-else class="admin-request-list">
              <div
                v-for="item in pendingRequests"
                :key="item.id"
                class="admin-request-item"
              >
                <div class="request-main">
                  <div class="request-title">{{ item.name || item.username }}</div>
                  <div class="request-meta">
                    <span>用户名：{{ item.username }}</span>
                    <span v-if="item.email">邮箱：{{ item.email }}</span>
                    <span>{{ formatDate(item.createdAt) }}</span>
                  </div>
                </div>
                <el-button type="primary" size="small" @click="$router.push('/profile-requests')">
                  去审批
                </el-button>
              </div>
            </div>
          </template>

          <!-- 学生：卡片列表 -->
          <template v-else-if="userStore.role === 'STUDENT'">
            <el-empty v-if="recentHomework.length === 0" description="暂无作业" />
            <div v-else class="student-homework-list">
              <div
                v-for="homework in recentHomework"
                :key="homework.id"
                class="student-homework-item"
              >
                <div class="item-main">
                  <div class="item-title-row">
                    <span class="item-title">{{ homework.title }}</span>
                    <el-tag size="small" type="info">{{ homework.courseName }}</el-tag>
                  </div>
                  <div class="item-meta">
                    <span><el-icon><User /></el-icon> {{ homework.teacherName }}</span>
                    <span :class="{ overdue: homework.isOverdue }">
                      <el-icon><Clock /></el-icon> {{ homework.deadlineText }}
                    </span>
                    <span v-if="homework.maxScore != null">
                      <el-icon><Coin /></el-icon> 满分 {{ homework.maxScore }}
                    </span>
                  </div>
                </div>
                <div class="item-side">
                  <div class="item-status">
                    <el-tag :type="getSubmissionStatusType(homework.submissionStatus)" effect="light" size="small">
                      {{ homework.submissionStatus }}
                    </el-tag>
                    <span v-if="homework.score != null" class="item-score">{{ homework.score }} 分</span>
                  </div>
                  <el-button
                    v-if="!homework.submissionStatus || homework.submissionStatus === '未提交'"
                    :type="homework.isOverdue ? 'info' : 'success'"
                    size="small"
                    :disabled="homework.isOverdue"
                    @click="goSubmitHomework(homework.id)"
                  >
                    {{ homework.isOverdue ? '已截止' : '去提交' }}
                  </el-button>
                  <el-button
                    v-else-if="homework.submissionStatus === '已提交'"
                    type="primary"
                    size="small"
                    @click="goViewSubmission(homework.id)"
                  >
                    查看提交
                  </el-button>
                  <el-button
                    v-else
                    type="success"
                    size="small"
                    @click="goViewSubmission(homework.id)"
                  >
                    查看结果
                  </el-button>
                </div>
              </div>
            </div>
          </template>

          <!-- 教师：表格 -->
          <el-table v-else-if="userStore.role === 'TEACHER'" :data="recentHomework" style="width: 100%" empty-text="暂无作业">
            <el-table-column prop="title" label="作业标题" show-overflow-tooltip />
            <el-table-column prop="courseName" label="课程" width="100" />
            <el-table-column prop="deadline" label="截止时间" width="170" />
          </el-table>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card v-loading="messagesLoading">
          <template #header>
            <div class="card-header message-card-header">
              <span>最近消息</span>
              <div class="message-header-actions">
                <el-select
                  v-if="userStore.role === 'ADMIN'"
                  v-model="selectedMessageUserId"
                  placeholder="选择用户"
                  filterable
                  clearable
                  style="width: 240px"
                  @change="loadAdminRecentMessages"
                >
                  <el-option
                    v-for="user in adminUserOptions"
                    :key="user.id"
                    :label="user.label"
                    :value="user.id"
                  />
                </el-select>
                <el-button text type="primary" @click="$router.push('/messages')">查看全部</el-button>
              </div>
            </div>
          </template>

          <el-empty
            v-if="userStore.role === 'ADMIN' && !selectedMessageUserId"
            description="请选择用户查看最近消息"
          />
          <el-empty v-else-if="recentMessages.length === 0" description="暂无消息" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="message in recentMessages"
              :key="message.id"
              :timestamp="formatDate(message.createdAt)"
              placement="top"
            >
              <el-card shadow="never" class="message-timeline-card">
                <div class="message-item-header">
                  <h4>{{ message.title }}</h4>
                  <el-tag
                    v-if="userStore.role === 'ADMIN'"
                    :type="message.isRead ? 'info' : 'danger'"
                    size="small"
                    effect="light"
                  >
                    {{ message.isRead ? '已读' : '未读' }}
                  </el-tag>
                </div>
                <p>{{ message.content }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="userStore.role === 'ADMIN'" :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>全平台作业概况</span>
              <span class="header-tip">共 {{ stats.homeworkCount }} 份作业</span>
            </div>
          </template>
          <el-table :data="recentHomework" style="width: 100%" empty-text="暂无作业">
            <el-table-column prop="title" label="作业标题" min-width="140" show-overflow-tooltip />
            <el-table-column prop="courseName" label="课程" width="100" />
            <el-table-column prop="teacherName" label="发布教师" width="110" />
            <el-table-column prop="statusLabel" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getHomeworkStatusType(row.status)" size="small">{{ row.statusLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deadline" label="截止时间" width="170" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getHomeworkByStudent, getHomeworkByTeacher, getHomeworkList } from '@/api/homework'
import { getSubmissionsByStudent, getAllSubmissionsForTeacher, getHomeworkStatistics } from '@/api/submission'
import { getUnreadMessages, getMessagesByReceiver } from '@/api/message'
import { getAllUsers, getUsersByRole } from '@/api/user'
import { getPendingProfileRequests } from '@/api/profileRequest'

const userStore = useUserStore()
const router = useRouter()

const stats = reactive({
  homeworkCount: 0,
  submissionCount: 0,
  gradedCount: 0,
  unreadCount: 0,
  userCount: 0
})

const adminStats = reactive({
  userCount: 0,
  teacherCount: 0,
  studentCount: 0,
  pendingCount: 0
})

const recentHomework = ref([])
const recentMessages = ref([])
const pendingRequests = ref([])
const adminUserOptions = ref([])
const selectedMessageUserId = ref(null)
const messagesLoading = ref(false)
const teacherHomeworkList = ref([])
const selectedHomeworkId = ref(null)
const homeworkStats = ref(null)
const statsLoading = ref(false)

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

const getHomeworkStatusLabel = (status) => {
  const labelMap = {
    published: '已发布',
    draft: '草稿',
    closed: '已关闭',
    wait: '待发布',
    expired: '已过期',
    withdrawn: '已撤回'
  }
  return labelMap[status] || status || '-'
}

const getHomeworkStatusType = (status) => {
  const typeMap = {
    published: 'success',
    draft: 'info',
    closed: 'warning',
    wait: 'warning',
    expired: 'info',
    withdrawn: 'danger'
  }
  return typeMap[status] || 'info'
}

const buildTeacherNameMap = (teachers) => {
  const map = {}
  ;(teachers || []).forEach(teacher => {
    map[teacher.id] = teacher.name || teacher.username || '-'
  })
  return map
}

const mapAdminHomework = (item, teacherMap) => ({
  id: item.id,
  title: item.title || '-',
  courseName: item.courseName || '-',
  teacherName: teacherMap[item.teacherId] || '-',
  status: item.status,
  statusLabel: getHomeworkStatusLabel(item.status),
  deadline: item.deadline ? formatDate(item.deadline) : '-'
})

const getRoleLabel = (role) => {
  const roleMap = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  }
  return roleMap[(role || '').toUpperCase()] || role || '-'
}

const loadAdminRecentMessages = async () => {
  if (!selectedMessageUserId.value) {
    recentMessages.value = []
    return
  }

  messagesLoading.value = true
  try {
    const res = await getMessagesByReceiver(selectedMessageUserId.value, 10)
    recentMessages.value = res.data || []
  } catch (e) {
    console.error('加载用户消息失败:', e)
    recentMessages.value = []
  } finally {
    messagesLoading.value = false
  }
}

const loadAdminStats = async () => {
  const [usersRes, teacherRes, studentRes, homeworkRes, pendingRes, teachersRes, allUsersRes] = await Promise.all([
    getAllUsers({ page: 1, limit: 1 }),
    getAllUsers({ page: 1, limit: 1, role: 'TEACHER' }),
    getAllUsers({ page: 1, limit: 1, role: 'STUDENT' }),
    getHomeworkList(),
    getPendingProfileRequests(),
    getUsersByRole('teacher'),
    getAllUsers({ page: 1, limit: 500 })
  ])

  adminStats.userCount = usersRes.data?.totalElements || 0
  stats.userCount = adminStats.userCount
  adminStats.teacherCount = teacherRes.data?.totalElements || 0
  adminStats.studentCount = studentRes.data?.totalElements || 0
  stats.homeworkCount = homeworkRes.data?.length || 0
  adminStats.pendingCount = pendingRes.data?.length || 0
  pendingRequests.value = (pendingRes.data || []).slice(0, 5)

  const teacherMap = buildTeacherNameMap(teachersRes.data)
  recentHomework.value = (homeworkRes.data || [])
    .sort((a, b) => {
      const timeA = a.deadline ? new Date(a.deadline).getTime() : 0
      const timeB = b.deadline ? new Date(b.deadline).getTime() : 0
      return timeB - timeA
    })
    .slice(0, 10)
    .map(item => mapAdminHomework(item, teacherMap))

  adminUserOptions.value = (allUsersRes.data?.content || []).map(user => ({
    id: user.id,
    label: `${user.name || user.username}（${user.username} · ${getRoleLabel(user.role)}）`
  }))

  if (!selectedMessageUserId.value) {
    selectedMessageUserId.value = userStore.user?.id ?? userStore.user?.userId ?? null
  }
  await loadAdminRecentMessages()
}

const getStudentStatusType = (statusCode) => {
  const typeMap = {
    unsubmitted: 'info',
    submitted: 'warning',
    graded: 'success'
  }
  return typeMap[statusCode] || 'info'
}

const getSubmissionStatusType = (status) => {
  const typeMap = {
    '未提交': 'info',
    '已提交': 'warning',
    '已批改': 'success'
  }
  return typeMap[status] || 'info'
}

const goSubmitHomework = (id) => {
  router.push({ path: '/submission', query: { homeworkId: id } })
}

const goViewSubmission = (id) => {
  router.push({ path: '/submission', query: { homeworkId: id, viewDetail: 'true' } })
}

const mapStudentHomework = (item) => {
  const deadline = item.homework?.deadline
  return {
    id: item.homework?.id,
    title: item.homework?.title || '-',
    courseName: item.homework?.courseName || '-',
    teacherName: item.teacherName || '-',
    deadline,
    deadlineText: deadline ? formatDate(deadline) : '-',
    maxScore: item.homework?.maxScore,
    submissionStatus: item.submissionStatus || '未提交',
    score: item.score,
    isOverdue: deadline ? new Date(deadline) < new Date() : false
  }
}

const loadHomeworkStatistics = async () => {
  if (!selectedHomeworkId.value) {
    homeworkStats.value = null
    return
  }

  statsLoading.value = true
  try {
    const res = await getHomeworkStatistics(selectedHomeworkId.value)
    homeworkStats.value = res.data || null
  } catch (e) {
    console.error('加载作业统计失败:', e)
    homeworkStats.value = null
  } finally {
    statsLoading.value = false
  }
}

const loadStats = async () => {
  try {
    if (userStore.role === 'STUDENT') {
      const homeworkRes = await getHomeworkByStudent()
      recentHomework.value = (homeworkRes.data || [])
        .filter(h => h.homework && h.homework.status === 'published')
        .sort((a, b) => {
          const timeA = a.homework?.deadline ? new Date(a.homework.deadline).getTime() : Number.MAX_SAFE_INTEGER
          const timeB = b.homework?.deadline ? new Date(b.homework.deadline).getTime() : Number.MAX_SAFE_INTEGER
          return timeA - timeB
        })
        .slice(0, 5)
        .map(mapStudentHomework)
      stats.homeworkCount = (homeworkRes.data || []).filter(h => h.homework && h.homework.status === 'published').length
      
      const submissionRes = await getSubmissionsByStudent()
      stats.submissionCount = submissionRes.data?.length || 0
      stats.gradedCount = submissionRes.data?.filter(s => s.status === 'graded').length || 0
    } else if (userStore.role === 'TEACHER') {
      const homeworkRes = await getHomeworkByTeacher()
      teacherHomeworkList.value = homeworkRes.data || []
      recentHomework.value = teacherHomeworkList.value.slice(0, 5).map(item => ({
        title: item.title || '-',
        courseName: item.courseName || '-',
        deadline: item.deadline ? formatDate(item.deadline) : '-'
      }))
      stats.homeworkCount = teacherHomeworkList.value.length

      const submissionRes = await getAllSubmissionsForTeacher()
      stats.submissionCount = submissionRes.data?.length || 0
      stats.gradedCount = submissionRes.data?.filter(s => s.status === 'graded').length || 0

      if (teacherHomeworkList.value.length > 0) {
        selectedHomeworkId.value = teacherHomeworkList.value[0].id
        await loadHomeworkStatistics()
      }
    } else if (userStore.role === 'ADMIN') {
      await loadAdminStats()
    }
    
    const messageRes = await getUnreadMessages()
    stats.unreadCount = messageRes.data?.length || 0

    if (userStore.role !== 'ADMIN') {
      recentMessages.value = messageRes.data?.slice(0, 5) || []
    }
  } catch (e) {
    console.error('加载统计数据失败:', e)
  }
}

loadStats()
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 20px;
}

.stat-card {
  :deep(.el-card__body) {
    padding: 20px;
  }
  
  .stat-content {
    display: flex;
    align-items: center;
    
    .stat-icon {
      width: 60px;
      height: 60px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 30px;
      color: #fff;
      margin-right: 15px;
      
      &.homework {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }

      &.users {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }
      
      &.submission {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      }
      
      &.graded {
        background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
      }
      
      &.message {
        background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
      }
    }
    
    .stat-info {
      flex: 1;
      
      .stat-value {
        font-size: 24px;
        font-weight: bold;
        color: #333;
      }
      
      .stat-label {
        font-size: 14px;
        color: #666;
        margin-top: 5px;
      }

      .stat-sub {
        margin-top: 4px;
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

.admin-quick-actions {
  margin-top: 20px;
}

.quick-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .quick-title {
    font-weight: 600;
    color: #303133;
    margin-right: 8px;
  }

  .action-badge {
    margin-left: 6px;
  }
}

.header-tip {
  font-size: 13px;
  color: #909399;
}

.admin-request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.admin-request-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;

  .request-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 6px;
  }

  .request-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    font-size: 13px;
    color: #909399;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-card-header {
  gap: 12px;
  flex-wrap: wrap;
}

.message-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.message-timeline-card {
  h4 {
    margin: 0;
    font-size: 15px;
    color: #303133;
  }

  p {
    margin: 8px 0 0;
    color: #606266;
    line-height: 1.6;
    white-space: pre-wrap;
  }
}

.message-item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.homework-summary {
  margin-bottom: 20px;
}

.mini-stats {
  margin-bottom: 16px;
}

.score-stats {
  margin-bottom: 20px;
}

.mini-stat-item {
  padding: 16px;
  background: #f8f9fb;
  border-radius: 8px;
  text-align: center;

  .mini-stat-value {
    font-size: 22px;
    font-weight: bold;
    color: #303133;

    &.success {
      color: #67c23a;
    }

    &.warning {
      color: #e6a23c;
    }

    &.primary {
      color: #409eff;
    }
  }

  .mini-stat-label {
    margin-top: 6px;
    font-size: 13px;
    color: #909399;
  }
}

.student-homework-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.student-homework-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: box-shadow 0.2s, border-color 0.2s;

  &:hover {
    border-color: #c6e2ff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.08);
  }

  .item-main {
    flex: 1;
    min-width: 0;
  }

  .item-title-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    .item-title {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .item-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    font-size: 13px;
    color: #909399;

    span {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }

    .overdue {
      color: #f56c6c;
    }
  }

  .item-side {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 8px;
    flex-shrink: 0;
  }

  .item-status {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .item-score {
    font-size: 14px;
    font-weight: 600;
    color: #67c23a;
  }
}
</style>
