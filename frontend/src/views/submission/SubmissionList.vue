<template>
  <div class="submission-list-container">
    <el-card>
      <template #header>
        <span>我的提交</span>
      </template>
      
      <el-table :data="submissionList" style="width: 100%" v-loading="loading">
        <el-table-column prop="homeworkTitle" label="作业标题" />
        <el-table-column prop="courseName" label="课程" width="150" />
        <el-table-column prop="submittedAt" label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.submittedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ row.status === 'graded' ? '已批改' : '待批改' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分数" width="80" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row.id)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSubmissionsByStudent } from '@/api/submission'

const router = useRouter()
const submissionList = ref([])
const loading = ref(false)

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const getStatusTag = (status) => {
  return status === 'graded' ? 'success' : 'warning'
}

const loadSubmissions = async () => {
  loading.value = true
  try {
    const res = await getSubmissionsByStudent()
    submissionList.value = res.data || []
  } catch (e) {
    console.error('加载提交列表失败:', e)
  } finally {
    loading.value = false
  }
}

const viewDetail = (id) => {
  // 跳转到提交详情页面（查看自己的提交）
  router.push({
    path: '/submission-detail/' + id
  })
}

onMounted(() => {
  loadSubmissions()
})
</script>

<style lang="scss" scoped>
.submission-list-container {
  padding: 20px;
}
</style>
