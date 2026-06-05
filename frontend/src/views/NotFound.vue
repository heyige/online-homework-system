<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <h1 class="error-code">404</h1>
      <h2 class="error-title">页面未找到</h2>
      <p class="error-description">抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.not-found-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f2f5;
}

.not-found-content {
  text-align: center;
  
  .error-code {
    font-size: 120px;
    font-weight: bold;
    color: #409EFF;
    line-height: 1;
    margin-bottom: 20px;
  }
  
  .error-title {
    font-size: 32px;
    color: #333;
    margin-bottom: 10px;
  }
  
  .error-description {
    font-size: 16px;
    color: #666;
    margin-bottom: 30px;
  }
}
</style>
