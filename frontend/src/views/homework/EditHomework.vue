<template>
  <div class="edit-homework-container">
    <el-card>
      <template #header>
        <span>编辑作业</span>
      </template>
      
      <el-form
        ref="homeworkFormRef"
        :model="homeworkForm"
        :rules="homeworkRules"
        label-width="100px"
      >
        <el-form-item label="作业标题" prop="title">
          <el-input v-model="homeworkForm.title" placeholder="请输入作业标题" />
        </el-form-item>
        
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="homeworkForm.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        
        <el-form-item label="作业描述" prop="description">
          <el-input
            v-model="homeworkForm.description"
            type="textarea"
            :rows="10"
            placeholder="请输入作业描述"
          />
        </el-form-item>
        
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker
            v-model="homeworkForm.deadline"
            type="datetime"
            placeholder="选择截止日期"
            style="width: 100%"
            default-time="00:00:00"
          />
        </el-form-item>
        
        <el-form-item label="满分" prop="maxScore">
          <el-input-number v-model="homeworkForm.maxScore" :min="0" :max="100" />
        </el-form-item>
        
        <el-form-item label="允许修改">
          <el-switch v-model="homeworkForm.allowModification" />
        </el-form-item>
        
        <el-form-item label="作业状态" prop="status">
          <el-select v-model="homeworkForm.status" placeholder="请选择作业状态" style="width: 100%">
            <el-option label="待发布" value="wait" />
            <el-option label="已发布" value="published" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="发布给学生">
          <el-select
            v-model="selectedStudents"
            multiple
            placeholder="选择学生（不选则发布给所有学生）"
            style="width: 100%"
          >
            <el-option
              v-for="student in students"
              :key="student.id"
              :label="student.name"
              :value="student.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">保存修改</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { updateHomework, getHomeworkById, getStudentIdsByHomeworkId, updateHomeworkStudents } from '@/api/homework'
import { getUsersByRole } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const homeworkId = route.params.id

const homeworkFormRef = ref(null)
const students = ref([])
const selectedStudents = ref([])

const homeworkForm = reactive({
  title: '',
  courseName: '',
  description: '',
  deadline: null,
  maxScore: 100,
  allowModification: true,
  status: 'published'
})

const homeworkRules = {
  title: [
    { required: true, message: '请输入作业标题', trigger: 'blur' }
  ],
  courseName: [
    { required: true, message: '请输入课程名称', trigger: 'blur' }
  ],
  deadline: [
    { required: true, message: '请选择截止时间', trigger: 'change' }
  ]
}

const loadHomework = async () => {
  try {
    const res = await getHomeworkById(homeworkId)
    if (res.code === 200) {
      const homework = res.data
      homeworkForm.title = homework.title
      homeworkForm.courseName = homework.courseName
      homeworkForm.description = homework.description
      homeworkForm.deadline = new Date(homework.deadline)
      homeworkForm.maxScore = homework.maxScore
      homeworkForm.allowModification = homework.allowModification
      homeworkForm.status = homework.status
    }
  } catch (e) {
    console.error('加载作业详情失败:', e)
  }
}

const loadStudents = async () => {
  try {
    const res = await getUsersByRole('student')
    students.value = res.data || []
  } catch (e) {
    console.error('加载学生列表失败:', e)
  }
}

const loadSelectedStudents = async () => {
  try {
    const res = await getStudentIdsByHomeworkId(homeworkId)
    if (res.code === 200) {
      const ids = res.data || []
      const allStudentIds = students.value.map(s => s.id)
      const isAllStudents = allStudentIds.length > 0
        && ids.length === allStudentIds.length
        && allStudentIds.every(id => ids.includes(id))
      // 已发布给全部学生时，编辑页保持为空，与「不选则发布给所有学生」一致
      selectedStudents.value = isAllStudents ? [] : ids
    }
  } catch (e) {
    console.error('加载已选择学生失败:', e)
  }
}

const handleSubmit = async () => {
  if (!homeworkFormRef.value) return
  
  await homeworkFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    try {
      // 先更新作业基本信息
      await updateHomework(homeworkId, homeworkForm)
      
      // 然后更新学生关联关系
      await updateHomeworkStudents(homeworkId, selectedStudents.value)
      
      ElMessage.success('作业更新成功')
      router.push('/homework')
    } catch (e) {
      console.error('更新作业失败:', e)
      ElMessage.error('更新作业失败')
    }
  })
}

onMounted(async () => {
  await loadHomework()
  await loadStudents()
  await loadSelectedStudents()
})
</script>

<style lang="scss" scoped>
.edit-homework-container {
  padding: 20px;
}
</style>