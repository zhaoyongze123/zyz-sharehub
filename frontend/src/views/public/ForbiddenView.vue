<template>
  <div class="page-shell">
    <BaseErrorState title="当前账号无权限访问" description="请切换管理员账号，或返回公开页面继续浏览。">
      <BaseButton @click="router.push('/login?redirect=/admin')">切换账号</BaseButton>
    </BaseErrorState>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'

const router = useRouter()
</script>
