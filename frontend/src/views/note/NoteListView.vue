<template>
  <div class="forum-container">
    <aside class="forum-sidebar">
      <nav class="forum-nav-group">
        <button class="forum-nav-item" :class="{ active: currentNav === 'topics' }" @click="switchNav('topics')">
          <div class="i-carbon-share-knowledge nav-icon"></div>
          <span>分享广场</span>
          <span class="nav-dot" v-if="currentNav === 'topics'"></span>
        </button>
        <button class="forum-nav-item" :class="{ active: currentNav === 'my-shares' }" @click="switchNav('my-shares')">
          <div class="i-carbon-document-add nav-icon"></div>
          <span>我的分享</span>
        </button>
        <button class="forum-nav-item" :class="{ active: currentNav === 'bookmarks' }" @click="switchNav('bookmarks')">
          <div class="i-carbon-bookmark nav-icon"></div>
          <span>我的收藏</span>
        </button>
        <button class="forum-nav-item" :class="{ active: currentNav === 'history' }" @click="switchNav('history')">
          <div class="i-carbon-recently-viewed nav-icon"></div>
          <span>浏览历史</span>
        </button>
      </nav>

      <div class="forum-nav-group">
        <div class="group-header" @click="toggleCategories = !toggleCategories">
          <span>AI 资源类别</span>
          <div class="i-carbon-chevron-down chevron" :style="{ transform: toggleCategories ? 'rotate(180deg)' : '' }"></div>
        </div>
        <div v-show="toggleCategories">
          <button
            v-for="cat in categories"
            :key="cat.name"
            class="forum-nav-item"
            :class="{ active: activeCategory === cat.name }"
            @click="toggleCategory(cat.name)">
            <span class="color-dot" :class="cat.color"></span>
            <span>{{ cat.name }}</span>
          </button>
        </div>
      </div>

      <div class="forum-nav-group">
        <div class="group-header ai-hot-header">
          <div class="ai-hot-title">
            <div class="i-carbon-fire" style="color: #ef4444;"></div>
            <span>AI 热点速递</span>
          </div>
        </div>
        <div class="news-list">
          <a href="javascript:void(0)" class="news-item" @click="openExternal('https://openai.com')">
            <span class="news-bullet"></span>
            <span>OpenAI 发布 o1 推理模型，数学能力突破</span>
          </a>
          <a href="javascript:void(0)" class="news-item" @click="openExternal('https://anthropic.com')">
            <span class="news-bullet"></span>
            <span>Claude 3.5 Sonnet 更新，交互引革新</span>
          </a>
          <a href="javascript:void(0)" class="news-item" @click="openExternal('https://cursor.sh')">
            <span class="news-bullet"></span>
            <span>Cursor 融资 $60M，AI 驱动的 IDE 爆发</span>
          </a>
        </div>
      </div>

      <div class="sidebar-action">
        <button class="publish-btn" @click="showPublishModal = true">
          <div class="i-carbon-add publish-icon"></div>
          发布新分享
        </button>
      </div>
    </aside>

    <main class="forum-main">
      <div class="forum-intro-banner" v-if="currentNav === 'topics'">
        专注 AI 技术分享，打造极客知识库。管理员发布的公告支持全领域透出与置顶，普通用户只展示自己的真实内容链路。
      </div>
      <div class="forum-intro-banner" v-else-if="currentNav === 'my-shares'">
        这里展示你发布过的全部笔记广场内容，可直接进入详情或删除自己的内容。
      </div>
      <div class="forum-intro-banner" v-else-if="currentNav === 'bookmarks'">
        你收藏的优质 AI 资源都在这里，方便随时回看。
      </div>

      <div class="forum-toolbar">
        <div class="toolbar-tabs">
          <div class="dropdown-wrapper">
            <button class="filter-dropdown" @click="showDropdown = !showDropdown">
              {{ activeCategory || '全部领域' }}
              <div class="i-carbon-chevron-down icon-xs"></div>
            </button>
            <div class="dropdown-menu" v-if="showDropdown">
              <div class="dropdown-item" @click="toggleCategory(''); showDropdown = false">全部领域</div>
              <div class="dropdown-item" v-for="cat in categories" :key="cat.name" @click="toggleCategory(cat.name); showDropdown = false">
                {{ cat.name }}
              </div>
            </div>
          </div>

          <button
            v-for="tab in filterTabs"
            :key="tab.label"
            class="tab-link"
            :class="{ active: activeTab === tab.id }"
            @click="activeTab = tab.id">
            {{ tab.label }}
          </button>
        </div>
      </div>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div class="topic-list-header">
        <div class="header-col title-col">AI 分享内容</div>
        <div class="header-col author-col">分享者</div>
        <div class="header-col time-col">发布时间</div>
      </div>

      <div class="topic-list">
        <article
          class="topic-row"
          v-for="topic in displayedTopics"
          :key="topic.id"
          :class="{ 'has-read': topic.hasRead, pinned: topic.isPinned }">
          <div class="topic-main">
            <div class="title-with-link">
              <div v-if="topic.isPinned" class="i-carbon-pin pin-icon"></div>
              <h3 class="topic-title" @click="readTopic(topic)">{{ topic.title }}</h3>
              <span class="title-category" :class="topic.categoryColor">{{ topic.categoryLabel }}</span>
            </div>
            <p class="topic-excerpt" v-if="topic.excerpt">{{ topic.excerpt }}</p>
            <div class="topic-meta">
              <span v-if="topic.isOfficial" class="meta-tag">
                <span class="dot tag-gray"></span>
                平台公告
              </span>
              <span v-if="topic.isOfficial" class="badge blue">官方</span>
              <span v-if="topic.category" class="meta-tag clickable" @click="toggleCategory(topic.category)">
                <span class="dot" :class="topic.categoryColor"></span>
                {{ topic.category }}
              </span>
              <span v-else class="meta-tag subdued">
                <span class="dot tag-gray"></span>
                所有领域
              </span>
              <span class="badge gray" v-for="tag in topic.tags" :key="tag.label">
                {{ tag.label }}
              </span>
              <button class="likes-btn" :class="{ liked: topic.isBookmarked }" @click="toggleBookmark(topic)">
                <div :class="topic.isBookmarked ? 'i-carbon-favorite-filled' : 'i-carbon-favorite'" class="icon-xs"></div>
                {{ topic.likes }} 收藏
              </button>
              <button v-if="topic.canDelete" class="danger-link" @click="handleDeleteTopic(topic)">
                删除
              </button>
            </div>
          </div>
          <div class="topic-author">
            <img :src="topic.author.avatar" class="user-avatar" v-if="topic.author.avatar" />
            <div class="user-avatar author-initial bg-gray" v-else>{{ topic.author.name.charAt(0).toUpperCase() }}</div>
            <span class="author-name">{{ topic.author.name }}</span>
          </div>
          <div class="topic-time">{{ topic.time }}</div>
        </article>

        <div v-if="!loading && displayedTopics.length === 0" class="empty-state">
          <div class="i-carbon-not-found empty-icon"></div>
          <p>没有找到符合条件的 AI 分享内容。</p>
        </div>
      </div>
    </main>

    <div class="modal-overlay" v-if="showPublishModal" @click.self="showPublishModal = false">
      <div class="publish-editor-modal">
        <div class="modal-header">
          <h3>发布新分享（支持 Markdown）</h3>
          <button class="close-btn" @click="showPublishModal = false"><div class="i-carbon-close"></div></button>
        </div>

        <div class="editor-toolbar">
          <input v-model="newDraft.title" type="text" placeholder="给你的分享起个响亮的标题..." class="title-input" />

          <div class="form-row metadata-row">
            <select v-model="newDraft.category" class="form-select">
              <option value="">{{ authStore.isAdmin ? '所有领域（管理员可不选）' : '选择所属领域' }}</option>
              <option v-for="cat in categories" :key="cat.name" :value="cat.name">{{ cat.name }}</option>
            </select>

            <div class="tags-input-wrapper">
              <div class="tag-chip" v-for="(tag, index) in newDraft.tags" :key="index">
                {{ tag }}
                <div class="i-carbon-close tag-close" @click="removeTag(index)"></div>
              </div>
              <input v-model="tagInput" type="text" class="tag-input-field" placeholder="输入标签按回车添加..." @keydown.enter.prevent="addTag" />
            </div>

            <label class="checkbox-label">
              <input type="checkbox" v-model="newDraft.hasLink">
              外链标志
            </label>

            <label v-if="authStore.isAdmin" class="checkbox-label admin-checkbox">
              <input type="checkbox" v-model="newDraft.isPinned">
              置顶公告
            </label>
          </div>

          <p v-if="authStore.isAdmin" class="admin-hint">管理员可不选领域，发布后将按“所有领域”展示。</p>
        </div>

        <div class="editor-panes">
          <div class="pane editor-pane">
            <textarea v-model="newDraft.content" placeholder="使用 Markdown 记录你的技术思考、分享前沿工具或开源项目..." class="md-textarea"></textarea>
          </div>
          <div class="pane preview-pane">
            <div class="preview-content markdown-body" v-html="renderedMarkdown || '<p class=\'placeholder-text\'>Markdown 预览区域...</p>'"></div>
          </div>
        </div>

        <div class="modal-footer">
          <div class="word-count" v-if="newDraft.content">字数: {{ newDraft.content.length }}</div>
          <div class="footer-actions">
            <button class="btn-cancel" @click="showPublishModal = false">取消</button>
            <button class="btn-submit" @click="submitPublish" :disabled="publishDisabled">确认发布</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import {
  createNote,
  deleteNote,
  favoriteNote,
  fetchCommunityNotes,
  fetchMyFavoriteNotes,
  fetchMyNoteHistory,
  fetchNotes,
  unfavoriteNote,
  type NoteDTO,
  type RelatedNoteItem
} from '@/api/notes'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()
const currentNav = ref('topics')
const activeTab = ref('latest')
const activeCategory = ref('')
const toggleCategories = ref(true)
const showDropdown = ref(false)
const showPublishModal = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const notes = ref<NoteDTO[]>([])
const favoriteNotes = ref<RelatedNoteItem[]>([])
const historyNotes = ref<RelatedNoteItem[]>([])
const favoriteNoteIds = ref<number[]>([])
const currentUserKey = computed(() => window.localStorage.getItem('ShareHub.userKey') || '')
const tagInput = ref('')

const newDraft = reactive({
  title: '',
  content: '',
  category: '',
  tags: [] as string[],
  hasLink: false,
  isPinned: false
})

const categories = [
  { name: '大模型前沿', color: 'bg-blue' },
  { name: 'AI 应用与 Agent', color: 'bg-red' },
  { name: '提示词工程', color: 'bg-green' },
  { name: 'AI 工具与效能', color: 'bg-teal' },
  { name: '学术与论文', color: 'bg-yellow' }
]

const filterTabs = [
  { id: 'latest', label: '最新 AI 分享' },
  { id: 'featured', label: '精选合集' },
  { id: 'popular', label: '最多收藏' }
]

const publishDisabled = computed(() => {
  if (authStore.isAdmin) {
    return !newDraft.title.trim() || !newDraft.content.trim()
  }
  return !newDraft.title.trim() || !newDraft.category.trim() || !newDraft.content.trim()
})

const renderedMarkdown = computed(() => {
  if (!newDraft.content) return ''
  return DOMPurify.sanitize(marked(newDraft.content) as string)
})

function mapNoteToTopic(note: NoteDTO) {
  const status = note.status || 'DRAFT'
  const category = note.category?.trim() || ''
  const isMine = Boolean(currentUserKey.value) && note.ownerKey === currentUserKey.value
  const canDelete = isMine || authStore.isAdmin
  return {
    id: note.id,
    title: note.title || '未命名笔记',
    excerpt: note.contentMd?.slice(0, 120) || '',
    category,
    categoryLabel: category || '所有领域',
    categoryColor: resolveCategoryColor(category),
    tags: category ? [{ label: category }] : [{ label: '全站可见' }],
    author: {
      name: note.ownerName?.trim() || note.ownerKey || '未知作者',
      avatar: note.ownerAvatarUrl || ''
    },
    likes: favoriteNoteIds.value.includes(note.id) ? 1 : 0,
    isBookmarked: favoriteNoteIds.value.includes(note.id),
    hasRead: false,
    isMine,
    canDelete,
    time: formatNoteTime(note.createdAt || note.updatedAt, note.isPinned),
    isFeatured: status === 'PUBLISHED',
    isOfficial: Boolean(note.isOfficial),
    isPinned: Boolean(note.isPinned)
  }
}

function mapRelatedNoteToTopic(note: RelatedNoteItem, options: { hasRead?: boolean; isMine?: boolean } = {}) {
  const status = note.status || 'DRAFT'
  const category = note.category?.trim() || ''
  const isMine = options.isMine ?? (Boolean(currentUserKey.value) && note.ownerKey === currentUserKey.value)
  const canDelete = currentNav.value === 'my-shares' ? isMine : authStore.isAdmin
  return {
    id: note.id,
    title: note.title || '未命名笔记',
    excerpt: note.summary || '',
    category,
    categoryLabel: category || '所有领域',
    categoryColor: resolveCategoryColor(category),
    tags: (note.tags || []).map((tag) => ({ label: tag })),
    author: {
      name: note.ownerName?.trim() || note.ownerKey || '未知作者',
      avatar: note.ownerAvatarUrl || ''
    },
    likes: note.favorites ?? 0,
    isBookmarked: note.favorited ?? false,
    hasRead: options.hasRead ?? false,
    isMine,
    canDelete,
    time: note.updatedAt || '未知时间',
    isFeatured: status === 'PUBLISHED',
    isOfficial: false,
    isPinned: false
  }
}

const displayedTopics = computed(() => {
  let list

  if (currentNav.value === 'bookmarks') {
    list = favoriteNotes.value.map((note) => mapRelatedNoteToTopic(note))
  } else if (currentNav.value === 'history') {
    list = historyNotes.value.map((note) => mapRelatedNoteToTopic(note, { hasRead: true }))
  } else {
    list = notes.value.map(mapNoteToTopic)
    if (currentNav.value === 'my-shares') {
      list = list.filter((t) => t.isMine)
    }
  }

  if (activeCategory.value) {
    list = list.filter((t) => t.category === activeCategory.value)
  }

  if (activeTab.value === 'featured') {
    list = list.filter((t) => t.isFeatured)
  } else if (activeTab.value === 'popular') {
    list = [...list].sort((a, b) => (b.likes || 0) - (a.likes || 0))
  }

  return list
})

async function syncFavoriteIds() {
  try {
    const data = await fetchMyFavoriteNotes({ page: 1, pageSize: 1000 })
    favoriteNoteIds.value = (data.list || []).map((item) => item.id)
  } catch {
    favoriteNoteIds.value = []
  }
}

async function fetchList() {
  loading.value = true
  errorMessage.value = ''
  try {
    await syncFavoriteIds()

    if (currentNav.value === 'bookmarks') {
      const data = await fetchMyFavoriteNotes({ page: pageNum.value, pageSize: pageSize.value })
      favoriteNotes.value = data.list || []
      historyNotes.value = []
      notes.value = []
      total.value = data.total ?? favoriteNotes.value.length
      return
    }

    if (currentNav.value === 'history') {
      const data = await fetchMyNoteHistory({ page: pageNum.value, pageSize: pageSize.value })
      historyNotes.value = data.list || []
      favoriteNotes.value = []
      notes.value = []
      total.value = data.total ?? historyNotes.value.length
      return
    }

    const data = currentNav.value === 'topics'
      ? await fetchCommunityNotes({ page: pageNum.value, pageSize: pageSize.value })
      : await fetchNotes({ page: pageNum.value, pageSize: pageSize.value, status: activeTab.value === 'featured' ? 'PUBLISHED' : undefined })
    notes.value = data.list || []
    favoriteNotes.value = []
    historyNotes.value = []
    total.value = data.total ?? notes.value.length
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.msg || '笔记列表获取失败'
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
watch([activeTab, currentNav], () => { void fetchList() })

function switchNav(nav: string) {
  currentNav.value = nav
  activeCategory.value = ''
}

function toggleCategory(name: string) {
  activeCategory.value = activeCategory.value === name ? '' : name
}

function resolveCategoryColor(category: string) {
  return categories.find((item) => item.name === category)?.color || 'bg-gray'
}

function formatNoteTime(value?: string | null, pinned?: boolean) {
  if (pinned) {
    return '置顶公告'
  }
  if (!value) {
    return '未知时间'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '未知时间'
  }
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function updateRelatedCollection(list: RelatedNoteItem[], noteId: number, favorited: boolean, favorites: number) {
  const target = list.find((item) => item.id === noteId)
  if (!target) return
  target.favorited = favorited
  target.favorites = favorites
}

async function toggleBookmark(topic: any) {
  if (!topic?.id) return
  const noteId = Number(topic.id)
  const favorited = Boolean(topic.isBookmarked)
  const result = favorited ? await unfavoriteNote(noteId) : await favoriteNote(noteId)
  const nextFavorited = !favorited
  const nextFavorites = result.favorites ?? topic.likes ?? 0

  if (nextFavorited) {
    if (!favoriteNoteIds.value.includes(noteId)) {
      favoriteNoteIds.value = [...favoriteNoteIds.value, noteId]
    }
  } else {
    favoriteNoteIds.value = favoriteNoteIds.value.filter((id) => id !== noteId)
  }

  updateRelatedCollection(favoriteNotes.value, noteId, nextFavorited, nextFavorites)
  updateRelatedCollection(historyNotes.value, noteId, nextFavorited, nextFavorites)

  if (!nextFavorited && currentNav.value === 'bookmarks') {
    favoriteNotes.value = favoriteNotes.value.filter((item) => item.id !== noteId)
    total.value = Math.max(0, total.value - 1)
  }
}

async function handleDeleteTopic(topic: any) {
  if (!topic?.id) return
  const confirmed = window.confirm(`确认删除《${topic.title}》？删除后不可恢复。`)
  if (!confirmed) return
  try {
    await deleteNote(Number(topic.id))
    appStore.showToast('删除成功', '内容已从笔记广场移除')
    await fetchList()
  } catch (e: any) {
    appStore.showToast('删除失败', e?.response?.data?.msg ?? '请稍后再试', 'error')
  }
}

function readTopic(topic: any) {
  router.push({ name: 'note-detail', params: { id: topic?.id ?? 0 } })
}

function openExternal(url: string) {
  window.open(url, '_blank')
}

function addTag() {
  const val = tagInput.value.trim()
  if (val && !newDraft.tags.includes(val) && newDraft.tags.length < 5) {
    newDraft.tags.push(val)
  }
  tagInput.value = ''
}

function removeTag(index: number) {
  newDraft.tags.splice(index, 1)
}

async function submitPublish() {
  try {
    await createNote({
      title: newDraft.title.trim(),
      contentMd: newDraft.content.trim(),
      status: 'PUBLISHED',
      visibility: 'PUBLIC',
      category: newDraft.category.trim() ? newDraft.category.trim() : null,
      isPinned: authStore.isAdmin ? newDraft.isPinned : false
    })
    await fetchList()
    appStore.showToast('发布成功', '笔记已保存到云端')
  } catch (e: any) {
    appStore.showToast('发布失败', e?.response?.data?.msg ?? '请稍后再试', 'error')
    return
  }

  newDraft.title = ''
  newDraft.content = ''
  newDraft.category = ''
  newDraft.tags = []
  newDraft.hasLink = false
  newDraft.isPinned = false
  tagInput.value = ''
  showPublishModal.value = false

  currentNav.value = 'my-shares'
  activeTab.value = 'latest'
  activeCategory.value = ''
}
</script>

<style scoped lang="scss">
.forum-container {
  display: flex;
  margin: -32px;
  min-height: calc(100vh - 60px);
  background: white;
  position: relative;
}
.forum-sidebar { width: 220px; background: #ffffff; border-right: 1px solid #f3f4f6; padding: 16px 0; display: flex; flex-direction: column; gap: 16px; flex-shrink: 0; overflow-y: auto; }
.forum-nav-group { display: flex; flex-direction: column; gap: 2px; }
.group-header { display: flex; justify-content: space-between; align-items: center; padding: 8px 16px 8px 32px; font-size: 13px; color: #6b7280; cursor: pointer; transition: color 0.15s; }
.group-header:hover .chevron { color: #111827; }
.chevron { font-size: 12px; transition: transform 0.2s; }
.forum-nav-item { display: flex; align-items: center; gap: 12px; padding: 8px 16px 8px 32px; border: none; background: transparent; color: #374151; font-size: 14px; text-align: left; cursor: pointer; position: relative; transition: all 0.15s; }
.forum-nav-item:hover { background: #f9fafb; color: #111827; }
.forum-nav-item.active { background: #e0f2fe; color: #0369a1; font-weight: 500; border-radius: 0 16px 16px 0; margin-right: 8px; }
.nav-icon { font-size: 16px; color: #6b7280; transition: color 0.15s; }
.forum-nav-item.active .nav-icon { color: #0369a1; }
.nav-dot { width: 6px; height: 6px; border-radius: 50%; background: #3b82f6; margin-left: auto; }
.color-dot { width: 10px; height: 10px; border-radius: 2px; }
.bg-blue { background: #3b82f6; }
.bg-red { background: #ef4444; }
.bg-green { background: #10b981; }
.bg-teal { background: #14b8a6; }
.bg-gray { background: #9ca3af; }
.bg-yellow { background: #f59e0b; }
.ai-hot-header { cursor: default; }
.ai-hot-title { display: flex; align-items: center; gap: 6px; font-weight: 600; color: #111827; }
.news-list { display: flex; flex-direction: column; gap: 12px; padding: 12px 16px 12px 32px; }
.news-item { display: flex; align-items: flex-start; gap: 8px; font-size: 13px; color: #4b5563; text-decoration: none; line-height: 1.4; }
.news-bullet { width: 4px; height: 4px; background: #d1d5db; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
.sidebar-action { margin-top: auto; padding: 16px; }
.publish-btn { width: 100%; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 10px; border-radius: 6px; background: #2563eb; border: none; color: white; font-size: 14px; font-weight: 500; cursor: pointer; }
.publish-icon { font-size: 16px; }
.forum-main { flex: 1; padding: 24px; max-width: 1100px; display: flex; flex-direction: column; }
.forum-intro-banner, .error-banner { background: #eff6ff; border: 1px solid #bfdbfe; padding: 12px 16px; border-radius: 4px; color: #1e40af; font-size: 14px; margin-bottom: 20px; }
.error-banner { background: #fef2f2; border-color: #fecaca; color: #b91c1c; }
.forum-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.toolbar-tabs { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.dropdown-wrapper { position: relative; }
.filter-dropdown { display: flex; align-items: center; gap: 4px; background: white; border: 1px solid #d1d5db; padding: 6px 12px; border-radius: 4px; font-size: 13px; color: #374151; cursor: pointer; }
.dropdown-menu { position: absolute; top: calc(100% + 4px); left: 0; background: white; border: 1px solid #e5e7eb; border-radius: 6px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); padding: 4px; z-index: 10; min-width: 120px; }
.dropdown-item { padding: 8px 12px; font-size: 13px; color: #374151; border-radius: 4px; cursor: pointer; }
.dropdown-item:hover { background: #f3f4f6; }
.icon-xs { font-size: 10px; }
.tab-link { background: transparent; border: none; font-size: 14px; color: #4b5563; cursor: pointer; padding: 6px 0; }
.tab-link.active { color: #2563eb; font-weight: 600; border-bottom: 2px solid #2563eb; }
.topic-list-header { display: flex; padding: 8px 16px; font-size: 12px; color: #6b7280; border-bottom: 1px solid #e5e7eb; }
.header-col { display: flex; align-items: center; }
.title-col { flex: 1; }
.author-col { width: 140px; padding-left: 16px; }
.time-col { width: 100px; text-align: right; justify-content: flex-end; }
.topic-list { flex: 1; display: flex; flex-direction: column; }
.topic-row { display: grid; grid-template-columns: minmax(0, 1fr) 140px 100px; gap: 16px; padding: 18px 16px; border-bottom: 1px solid #eef2f7; align-items: start; }
.topic-row.pinned { background: linear-gradient(90deg, #f8fbff, #ffffff); }
.topic-main { min-width: 0; }
.title-with-link { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 10px; }
.topic-title { margin: 0; font-size: 18px; line-height: 1.35; cursor: pointer; color: #0f172a; }
.topic-title:hover { color: #2563eb; }
.title-category { display: inline-flex; align-items: center; padding: 4px 10px; border-radius: 999px; font-size: 12px; color: #475569; background: #f1f5f9; }
.topic-excerpt { margin: 0 0 12px; color: #64748b; line-height: 1.7; }
.topic-meta { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.meta-tag, .badge { display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; border-radius: 999px; font-size: 12px; background: #f8fafc; color: #475569; }
.meta-tag.clickable { cursor: pointer; }
.meta-tag.subdued { color: #64748b; }
.badge.blue { background: #dbeafe; color: #1d4ed8; }
.badge.gray { background: #f1f5f9; color: #475569; }
.dot { width: 8px; height: 8px; border-radius: 999px; }
.tag-gray { background: #94a3b8; }
.likes-btn, .danger-link { border: none; background: transparent; cursor: pointer; font-size: 12px; }
.likes-btn { display: inline-flex; align-items: center; gap: 6px; color: #475569; }
.likes-btn.liked { color: #dc2626; }
.danger-link { color: #dc2626; font-weight: 600; }
.topic-author { display: flex; align-items: center; gap: 10px; color: #334155; }
.user-avatar { width: 32px; height: 32px; border-radius: 999px; object-fit: cover; display: inline-flex; align-items: center; justify-content: center; }
.author-initial { color: white; font-size: 12px; }
.author-name { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.topic-time { text-align: right; color: #64748b; font-size: 13px; }
.pin-icon { color: #dc2626; }
.empty-state { padding: 48px 16px; text-align: center; color: #64748b; }
.empty-icon { font-size: 32px; margin-bottom: 12px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.45); display: flex; align-items: center; justify-content: center; z-index: 50; padding: 24px; }
.publish-editor-modal { width: min(1120px, 100%); background: white; border-radius: 20px; overflow: hidden; box-shadow: 0 30px 80px rgba(15, 23, 42, 0.24); }
.modal-header, .modal-footer { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #e5e7eb; }
.modal-footer { border-top: 1px solid #e5e7eb; border-bottom: none; }
.close-btn, .btn-cancel, .btn-submit { border: none; cursor: pointer; }
.close-btn { background: transparent; }
.editor-toolbar { padding: 20px 24px 0; }
.title-input, .form-select, .tag-input-field, .md-textarea { width: 100%; }
.title-input { border: none; font-size: 22px; font-weight: 700; margin-bottom: 16px; outline: none; }
.metadata-row { display: grid; grid-template-columns: 240px minmax(0, 1fr) auto auto; gap: 12px; align-items: center; }
.form-select, .tag-input-field { border: 1px solid #dbe3ee; border-radius: 10px; padding: 10px 12px; }
.tags-input-wrapper { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; border: 1px solid #dbe3ee; border-radius: 10px; padding: 8px 10px; }
.tag-chip { display: inline-flex; align-items: center; gap: 6px; background: #eff6ff; color: #1d4ed8; border-radius: 999px; padding: 6px 10px; font-size: 12px; }
.checkbox-label { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; color: #475569; }
.admin-hint { margin: 12px 0 0; color: #1d4ed8; font-size: 12px; }
.editor-panes { display: grid; grid-template-columns: 1fr 1fr; min-height: 420px; }
.pane { padding: 24px; }
.editor-pane { background: #f8fafc; border-right: 1px solid #e5e7eb; }
.md-textarea { min-height: 372px; resize: none; border: none; background: transparent; outline: none; line-height: 1.8; }
.preview-content { max-height: 372px; overflow: auto; }
.word-count { color: #64748b; font-size: 13px; }
.footer-actions { display: flex; gap: 12px; }
.btn-cancel { background: #e2e8f0; color: #0f172a; padding: 10px 16px; border-radius: 10px; }
.btn-submit { background: #2563eb; color: white; padding: 10px 18px; border-radius: 10px; }
.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }
@media (max-width: 1024px) {
  .forum-container { flex-direction: column; margin: -16px; }
  .forum-sidebar { width: auto; border-right: none; border-bottom: 1px solid #e5e7eb; }
  .topic-row { grid-template-columns: 1fr; }
  .topic-time { text-align: left; }
  .metadata-row, .editor-panes { grid-template-columns: 1fr; }
  .editor-pane { border-right: none; border-bottom: 1px solid #e5e7eb; }
}
</style>
