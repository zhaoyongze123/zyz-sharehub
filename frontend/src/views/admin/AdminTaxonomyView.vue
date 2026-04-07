<template>
  <div class="admin-view">
    <div class="section-heading">
      <h1>标签分类管理</h1>
      <p>支持标签与分类的增删改和排序。</p>
    </div>
    <section class="glass-panel panel">
      <BaseTable>
        <thead>
          <tr>
            <th>名称</th>
            <th>类型</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in taxonomyItems" :key="item.id">
            <td>{{ item.name }}</td>
            <td>{{ item.type }}</td>
            <td>
              <BaseButton size="sm" variant="secondary" @click="editItem(item.name)">编辑</BaseButton>
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
import { taxonomyItems } from '@/mock/admin'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

function editItem(name: string) {
  appStore.showToast('已进入编辑态', `${name} 已准备修改`)
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
