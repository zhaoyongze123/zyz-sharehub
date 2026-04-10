<template>
  <div class="admin-view">
    <div class="section-heading">
      <h1>标签分类管理</h1>
      <p>基于真实资料库生成当前分类与标签概览，编辑能力待后端治理接口补齐后再开放。</p>
    </div>

    <section v-if="loading" class="glass-panel panel panel-state">
      <BaseSkeleton height="12rem" />
    </section>

    <BaseErrorState
      v-else-if="error"
      title="标签分类加载失败"
      description="暂时无法读取真实资料数据，请稍后重试。"
    />

    <section v-else-if="taxonomyItems.length" class="glass-panel panel">
      <div class="panel-summary" data-testid="admin-taxonomy-summary">
        共整理 {{ normalizedItems.length }} 项，已读取 {{ loadedResourceCount }} / {{ resourceTotal }} 条真实资料。
      </div>
      <BaseTable>
        <thead>
          <tr>
            <th>名称</th>
            <th>类型</th>
            <th>关联资料数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in normalizedItems" :key="item.id">
            <td>{{ item.name }}</td>
            <td>{{ item.type }}</td>
            <td>{{ item.count }}</td>
            <td>
              <BaseButton size="sm" variant="secondary" @click="explainItem(item.name, item.type)" data-testid="admin-taxonomy-readonly">
                只读概览
              </BaseButton>
            </td>
          </tr>
        </tbody>
      </BaseTable>
    </section>

    <BaseEmpty
      v-else
      title="暂无标签分类数据"
      description="当前真实资料库中还没有可展示的标签或分类。"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchResources } from '@/api/resources'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseEmpty from '@/components/base/BaseEmpty.vue'
import BaseErrorState from '@/components/base/BaseErrorState.vue'
import BaseSkeleton from '@/components/base/BaseSkeleton.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import { useAppStore } from '@/stores/app'

interface TaxonomyItem {
  id: string
  name: string
  type: '标签' | '分类'
  count: number
}

const appStore = useAppStore()
const loading = ref(true)
const error = ref(false)
const resourceTotal = ref(0)
const loadedResourceCount = ref(0)
const taxonomyItems = ref<TaxonomyItem[]>([])

const EMPTY_ITEM_NAME = '未分类'

const normalizedItems = computed(() => (
  taxonomyItems.value
    .slice()
    .sort((left, right) => {
      if (right.count !== left.count) {
        return right.count - left.count
      }
      return left.name.localeCompare(right.name, 'zh-CN')
    })
))

function buildTaxonomy(resources: Awaited<ReturnType<typeof fetchResources>>['items']) {
  const categoryCounter = new Map<string, number>()
  const tagCounter = new Map<string, number>()

  for (const resource of resources) {
    const category = resource.category.trim() || EMPTY_ITEM_NAME
    categoryCounter.set(category, (categoryCounter.get(category) ?? 0) + 1)

    for (const tag of resource.tags) {
      const normalizedTag = tag.trim()
      if (!normalizedTag) continue
      tagCounter.set(normalizedTag, (tagCounter.get(normalizedTag) ?? 0) + 1)
    }
  }

  const categories = Array.from(categoryCounter.entries()).map(([name, count]) => ({
    id: `category-${name}`,
    name,
    type: '分类' as const,
    count
  }))
  const tags = Array.from(tagCounter.entries()).map(([name, count]) => ({
    id: `tag-${name}`,
    name,
    type: '标签' as const,
    count
  }))

  taxonomyItems.value = [...categories, ...tags]
}

async function loadTaxonomy() {
  loading.value = true
  error.value = false

  try {
    const response = await fetchResources({
      page: 0,
      pageSize: 100,
      sortBy: 'latest'
    })
    resourceTotal.value = response.total
    loadedResourceCount.value = response.items.length
    buildTaxonomy(response.items)
  } catch (loadError) {
    error.value = true
    resourceTotal.value = 0
    loadedResourceCount.value = 0
    taxonomyItems.value = []
    console.error(loadError)
  } finally {
    loading.value = false
  }
}

function explainItem(name: string, type: TaxonomyItem['type']) {
  appStore.showToast('当前为只读概览', `${type}“${name}”已来自真实资料数据，编辑接口待补齐`)
}

onMounted(() => {
  void loadTaxonomy()
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

.panel-summary {
  margin-bottom: var(--space-3);
  color: var(--color-text-secondary);
}
</style>
