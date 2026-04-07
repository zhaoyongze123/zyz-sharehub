<template>
  <div class="page-shell">
    <BaseErrorState title="页面不存在" description="你访问的路径没有对应页面，可能已经被移动或删除。">
      <BaseButton @click="router.push('/')">返回首页</BaseButton>
    </BaseErrorState>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'

const router = useRouter()
</script>
