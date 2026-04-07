<template>
  <div class="admin-view">
    <div class="section-heading">
      <h1>用户管理</h1>
      <p>支持用户搜索、状态管理、角色查看与封禁/解封。</p>
    </div>
    <section class="glass-panel panel">
      <BaseTable>
        <thead>
          <tr>
            <th>昵称</th>
            <th>角色</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in adminUsers" :key="item.id">
            <td>{{ item.nickname }}</td>
            <td>{{ item.role }}</td>
            <td>{{ item.status }}</td>
            <td>
              <BaseButton size="sm" variant="secondary" @click="toggleUser(item.nickname)">
                {{ item.status === '已封禁' ? '解封' : '封禁' }}
              </BaseButton>
            </td>
          </tr>
        </tbody>
      </BaseTable>
    </section>
  </div>
</template>

<script setup lang="ts">
import BaseButton from '@/components/base/BaseButton.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import { adminUsers } from '@/mock/admin'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

function toggleUser(name: string) {
  appStore.showToast('用户状态已更新', `${name} 的状态已切换`)
}
</script>

<style scoped lang="scss">
.admin-view {
  display: grid;
  gap: var(--space-6);
}

.panel {
  padding: var(--space-4);
}
</style>
