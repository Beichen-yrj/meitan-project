<template>
  <div class="draft-page-bar">
    <el-button class="draft-page-bar__back" type="primary" @click="router.push('/home')">
      <el-icon><ArrowLeft /></el-icon>
      返回主页
    </el-button>
    <h1 class="draft-page-bar__title">{{ title }}</h1>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

defineProps({
  title: {
    type: String,
    required: true,
  },
})

const router = useRouter()
</script>
