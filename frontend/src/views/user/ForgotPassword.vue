<template>
  <div class="forgot-password-container">
    <div class="forgot-password-box">
      <div class="forgot-password-header">
        <h2>忘记密码</h2>
        <p v-if="step === 1">验证身份</p>
        <p v-else-if="step === 2">重置密码</p>
      </div>
      
      <!-- 第一步：验证邮箱/手机号 -->
      <el-form
        v-if="step === 1"
        ref="verifyFormRef"
        :model="verifyForm"
        :rules="verifyRules"
        class="reset-form"
      >
        <el-form-item prop="identifier">
          <el-input
            v-model="verifyForm.identifier"
            placeholder="请输入邮箱或手机号"
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="reset-button"
            @click="verifyIdentifier"
          >
            下一步
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 第二步：验证码验证和密码重置 -->
      <el-form
        v-else-if="step === 2"
        ref="resetFormRef"
        :model="resetForm"
        :rules="resetRules"
        class="reset-form"
      >
        <el-form-item prop="code">
          <el-input
            v-model="resetForm.code"
            placeholder="请输入验证码"
            prefix-icon="Key"
            size="large"
          >
            <template #append>
              <el-button 
                @click="sendCode" 
                :disabled="countdown > 0"
              >
                {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item prop="newPassword">
          <el-input
            v-model="resetForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="resetForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="reset-button"
            @click="handleReset"
          >
            重置密码
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="reset-footer">
        <router-link to="/login">返回登录</router-link>
        <el-button 
          v-if="step === 2" 
          type="text" 
          @click="step = 1"
        >
          上一步
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { generateVerificationCode, resetPassword, verifyCode } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()

const step = ref(1) // 1: 验证身份, 2: 重置密码
const verifyFormRef = ref(null)
const resetFormRef = ref(null)
const loading = ref(false)
const countdown = ref(0)

const verifyForm = reactive({
  identifier: ''
})

const resetForm = reactive({
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const verifyRules = {
  identifier: [
    { required: true, message: '请输入邮箱或手机号', trigger: 'blur' }
  ]
}

const resetRules = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 4, message: '密码长度不能少于 4 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: (rule, value, callback) => {
        if (value !== resetForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, trigger: 'blur' }
  ]
}

const verifyIdentifier = () => {
  if (!verifyFormRef.value) return
  
  verifyFormRef.value.validate((valid) => {
    if (!valid) return
    
    loading.value = true
    
    generateVerificationCode(verifyForm.identifier)
      .then(() => {
        ElMessage.success('验证码已发送到控制台')
        step.value = 2
      })
      .catch((e) => {
        console.error('验证失败:', e)
      })
      .finally(() => {
        loading.value = false
      })
  })
}

const sendCode = async () => {
  try {
    await generateVerificationCode(verifyForm.identifier)
    ElMessage.success('验证码已重新发送到控制台')
    countdown.value = 60
    
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (e) {
    console.error('发送验证码失败:', e)
  }
}

const handleReset = async () => {
  if (!resetFormRef.value) return
  
  await resetFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    
    try {
      // 先验证验证码
      await verifyCode(verifyForm.identifier, resetForm.code)
      // 验证码验证通过后重置密码
      await resetPassword(verifyForm.identifier, resetForm.code, resetForm.newPassword)
      ElMessage.success('密码重置成功')
      router.push('/login')
    } catch (e) {
      console.error('重置密码失败:', e)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.forgot-password-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px 0;
}

.forgot-password-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  
  .forgot-password-header {
    text-align: center;
    margin-bottom: 30px;
    
    h2 {
      font-size: 24px;
      color: #333;
      margin-bottom: 10px;
    }
    
    p {
      font-size: 14px;
      color: #666;
    }
  }
  
  .reset-form {
    .reset-button {
      width: 100%;
    }
    
    :deep(.el-form-item) {
      margin-bottom: 20px;
    }
  }
  
  .reset-footer {
    text-align: center;
    margin-top: 20px;
    
    a {
      color: #409EFF;
      text-decoration: none;
      
      &:hover {
        text-decoration: underline;
      }
    }
  }
}
</style>
