<template>
  <div class="admin-view">
    <div class="section-heading">
      <h1>用户管理</h1>
      <p>支持用户状态管理、真实登录标识查看与封禁/解封。</p>
    </div>

    <section v-if="loading" class="glass-panel panel panel-state">
      <BaseSkeleton height="12rem" />
    </section>

    <BaseErrorState
      v-else-if="error"
      title="用户列表加载失败"
      description="暂时无法读取真实用户列表，请稍后重试。"
    />

    <section v-else-if="userItems.length" class="glass-panel panel">
      <BaseTable>
        <thead>
          <tr>
            <th>昵称</th>
            <th>登录标识</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in userItems" :key="item.id">
            <td>{{ item.nickname }}</td>
            <td>{{ item.login }}</td>
            <td>{{ item.status }}</td>
            <td>
              <BaseButton
                size="sm"
                variant="secondary"
                :disabled="submittingUserId === item.id"
                @click="toggleUser(item)"
              >
                {{ item.status === '已封禁' ? '解封' : '封禁' }}
              </BaseButton>
            </td>
          </tr>
        </tbody>
      </BaseTable>
    </section>

    <BaseEmpty
      v-else
      title="暂无用户数据"
      description="当前真实用户列表为空。"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { banAdminUser, fetchAdminUsers, type AdminUserItem, unbanAdminUser } from '@/api/admin'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const loading = ref(true)
const error = ref(false)
const userItems = ref<AdminUserItem[]>([])
const submittingUserId = ref<number | null>(null)

async function loadUsers() {
  loading.value = true
  error.value = false

  try {
    const response = await fetchAdminUsers(1, 20)
    userItems.value = response.items
  } catch (loadError) {
    error.value = true
    userItems.value = []
    console.error(loadError)
  } finally {
    loading.value = false
  }
}

async function toggleUser(user: AdminUserItem) {
  submittingUserId.value = user.id

  try {
    if (user.status === '已封禁') {
      await unbanAdminUser(user.id)
      appStore.showToast('用户已解封', `${user.nickname} 已恢复为正常状态`)
    } else {
      await banAdminUser(user.id)
      appStore.showToast('用户已封禁', `${user.nickname} 已被标记为封禁`)
    }
    await loadUsers()
  } catch (actionError) {
    console.error(actionError)
  } finally {
    submittingUserId.value = null
  }
}

onMounted(() => {
  void loadUsers()
})
</script>

<style scoped lang="scss">
.admin-view {
  display: grid;
  gap: var(--space-6);
}

.panel {
  padding: var(--space-4);
}

.panel-state {
  min-height: 14rem;
}
</style>
