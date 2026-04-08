<template>
  <div class="forum-container">
    <!-- Forum Left Sidebar -->
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
          <button v-for="cat in categories" :key="cat.name" 
                  class="forum-nav-item" 
                  :class="{ active: activeCategory === cat.name }" 
                  @click="toggleCategory(cat.name)">
            <span class="color-dot" :class="cat.color"></span> <span>{{ cat.name }}</span>
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

    <!-- Forum Main Content -->
    <main class="forum-main">
      <div class="forum-intro-banner" v-if="currentNav === 'topics'">
        专注 AI 技术分享，打造极客知识库！<strong>涵盖大模型前沿、Agent 开发与提示词工程</strong>，欢迎分享优质资源。<a href="#" @click.prevent="readTopic(null)" class="banner-link">《AI 社区指南》</a>
      </div>
      <div class="forum-intro-banner" v-else-if="currentNav === 'my-shares'">
        这里展示了您在社区发布的所有干货内容，感谢您的持续开源与奉献！
      </div>
      <div class="forum-intro-banner" v-else-if="currentNav === 'bookmarks'">
        您收藏的优质 AI 资源都在这里啦，方便随时查阅与复习。
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
          
          <button v-for="tab in filterTabs" :key="tab.label" 
                  class="tab-link" 
                  :class="{ active: activeTab === tab.id }" 
                  @click="activeTab = tab.id">
            {{ tab.label }}
          </button>
        </div>
      </div>

      <div class="topic-list-header">
        <div class="header-col title-col">AI 分享内容</div>
        <div class="header-col author-col">分享者</div>
        <div class="header-col time-col">发布时间</div>
      </div>

      <div class="topic-list">
        <!-- Pinned Topic -->
        <article class="topic-row pinned" v-if="currentNav === 'topics' && activeTab === 'latest' && !activeCategory">
          <div class="topic-main">
            <div class="topic-title-wrapper">
              <div class="i-carbon-pin pin-icon"></div>
              <h3 class="topic-title" @click="readTopic(null)">《 ShareHub AI 技术分享规范及内容标准 》</h3>
            </div>
            <div class="topic-meta">
              <span class="meta-tag"><span class="dot tag-gray"></span> 平台公告</span>
              <span class="badge blue">官方</span>
            </div>
            <p class="topic-excerpt">
              为了保证社区内容的高质量，我们提倡大家分享：经过验证的 AI 工具、有深度的大模型研究文章、生产环境可用的 Agent 代码片段...
            </p>
          </div>
          <div class="topic-author">
            <div class="user-avatar author-initial">A</div>
            <span class="author-name">系统管理员</span>
          </div>
          <div class="topic-time">置顶</div>
        </article>

        <div class="list-divider" v-if="currentNav === 'topics' && activeTab === 'latest' && !activeCategory"></div>

        <!-- Normal Topics -->
        <article class="topic-row" v-for="topic in displayedTopics" :key="topic.id" :class="{ 'has-read': topic.hasRead }">
          <div class="topic-main">
            <div class="title-with-link">
               <h3 class="topic-title" @click="readTopic(topic)">{{ topic.title }}</h3>
               <div class="i-carbon-launch external-icon" v-if="topic.hasLink" title="包含外部链接" @click.stop="openExternal('https://github.com/github/copilot-chat')"></div>
            </div>
            <p class="topic-excerpt" v-if="topic.excerpt">{{ topic.excerpt }}</p>
            <div class="topic-meta">
              <span class="meta-tag" @click="toggleCategory(topic.category)" style="cursor:pointer">
                <span class="dot" :class="topic.categoryColor"></span> 
                {{ topic.category }}
              </span>
              <span class="badge gray" v-for="tag in topic.tags" :key="tag.label">
                <div :class="tag.icon" class="badge-icon" v-if="tag.icon"></div>
                {{ tag.label }}
              </span>
              <button class="likes-btn" :class="{ 'liked': topic.isBookmarked }" @click="toggleBookmark(topic)">
                <div :class="topic.isBookmarked ? 'i-carbon-favorite-filled' : 'i-carbon-favorite'" class="icon-xs"></div> 
                {{ topic.likes }} 收藏
              </button>
            </div>
          </div>
          <div class="topic-author">
            <img :src="topic.author.avatar" class="user-avatar" v-if="topic.author.avatar"/>
            <div class="user-avatar author-initial bg-gray" v-else>{{ topic.author.name.charAt(0).toUpperCase() }}</div>
            <span class="author-name">{{ topic.author.name }}</span>
          </div>
          <div class="topic-time">{{ topic.time }}</div>
        </article>
        
        <div v-if="displayedTopics.length === 0" class="empty-state">
           <div class="i-carbon-not-found empty-icon"></div>
           <p>没有找到符合条件的 AI 分享内容。</p>
        </div>
      </div>
    </main>

    <!-- Markdown Editor Publish Modal -->
    <div class="modal-overlay" v-if="showPublishModal" @click.self="showPublishModal = false">
      <div class="publish-editor-modal">
        <div class="modal-header">
          <h3>发布新分享（支持 Markdown）</h3>
          <button class="close-btn" @click="showPublishModal = false"><div class="i-carbon-close"></div></button>
        </div>
        
        <!-- Editor Toolbar -->
        <div class="editor-toolbar">
          <input type="text" v-model="newDraft.title" placeholder="给你的分享起个响亮的标题..." class="title-input" />
          
          <div class="form-row metadata-row">
            <select v-model="newDraft.category" class="form-select">
              <option value="" disabled>选择所属领域</option>
              <option v-for="cat in categories" :key="cat.name" :value="cat.name">{{ cat.name }}</option>
            </select>

            <div class="tags-input-wrapper">
              <div class="tag-chip" v-for="(tag, index) in newDraft.tags" :key="index">
                {{ tag }}
                <div class="i-carbon-close tag-close" @click="removeTag(index)"></div>
              </div>
              <input type="text" v-model="tagInput" class="tag-input-field" placeholder="输入标签按回车添加..." @keydown.enter.prevent="addTag" />
            </div>

            <label class="checkbox-label">
              <input type="checkbox" v-model="newDraft.hasLink">
              外链标志
            </label>
          </div>
        </div>

        <!-- Editor Body (Split pane) -->
        <div class="editor-panes">
          <div class="pane editor-pane">
            <textarea v-model="newDraft.content" placeholder="使用 Markdown 记录你的技术思考、分享前沿工具或开源项目... " class="md-textarea"></textarea>
          </div>
          <div class="pane preview-pane">
            <div class="preview-content markdown-body" v-html="renderedMarkdown || '<p class=\'placeholder-text\'>Markdown 预览区域...</p>'"></div>
          </div>
        </div>

        <!-- Footer -->
        <div class="modal-footer">
          <div class="word-count" v-if="newDraft.content">字数: {{ newDraft.content.length }}</div>
          <div class="footer-actions">
            <button class="btn-cancel" @click="showPublishModal = false">取消</button>
            <button class="btn-submit" @click="submitPublish" :disabled="!newDraft.title || !newDraft.category || !newDraft.content">确认发布</button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchNoteList, createNote, type NoteItemDto } from '@/api/notes'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const appStore = useAppStore()
const currentNav = ref('topics')
const activeTab = ref('latest')
const activeCategory = ref('')
const toggleCategories = ref(true)
const showDropdown = ref(false)
const showPublishModal = ref(false)

const tagInput = ref('')

const currentUser = { name: '当前用户', avatar: 'https://avatars.githubusercontent.com/u/99?v=4' }

const newDraft = reactive({
  title: '',
  content: '',
  category: '',
  tags: [] as string[],
  hasLink: false
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
  { id: 'popular', label: '最多收藏' },
]

const topics = ref<NoteItemDto[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

const renderedMarkdown = computed(() => {
  if (!newDraft.content) return ''
  // 简易 Markdown 解析器用于预览
  let html = newDraft.content
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    
  // 标题
  html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>')
  html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>')
  html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>')
  
  // 粗体 & 斜体
  html = html.replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>')
  html = html.replace(/\*(.*)\*/gim, '<em>$1</em>')
  
  // 代码块 & 行内代码
  html = html.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
  
  // 引用
  html = html.replace(/^\> (.*$)/gim, '<blockquote>$1</blockquote>')
  
  // 列表
  html = html.replace(/^\- (.*$)/gim, '<ul><li>$1</li></ul>')
  html = html.replace(/<\/ul>\n<ul>/gim, '')
  
  // 换行替换
  html = html.split('\n').map(line => {
    if (line.match(/^(<h|<pre|<ul|<blockquote)/)) return line;
    return line ? `<p>${line}</p>` : '';
  }).join('');
  
  return html
})

const viewTopics = computed(() =>
  topics.value.map((item) => ({
    id: item.id,
    title: item.title,
    excerpt: item.summary,
    hasLink: false,
    category: item.status || '未分类',
    categoryColor: 'bg-teal',
    tags: (item.tags || []).map((t) => ({ label: t, icon: '' })),
    author: { name: '我', avatar: currentUser.avatar },
    likes: (item.tags || []).length,
    isBookmarked: false,
    hasRead: false,
    isMine: true,
    time: formatDate(item.updatedAt || item.createdAt)
  }))
)

const displayedTopics = computed(() => {
  let list = viewTopics.value

  if (currentNav.value === 'bookmarks') {
    list = list.filter((t) => t.isBookmarked)
  } else if (currentNav.value === 'history') {
    list = list.filter((t) => t.hasRead)
  }

  if (activeCategory.value) {
    list = list.filter((t) => t.category === activeCategory.value || t.tags.some((tag) => tag.label === activeCategory.value))
  }

  if (activeTab.value === 'popular') {
    list = [...list].sort((a, b) => b.likes - a.likes)
  }

  return list
})

function switchNav(nav: string) {
  currentNav.value = nav
  activeCategory.value = ''
}

function toggleCategory(name: string) {
  activeCategory.value = activeCategory.value === name ? '' : name
}

function toggleBookmark(topic: any) {
  topic.isBookmarked = !topic.isBookmarked
  topic.likes += topic.isBookmarked ? 1 : -1
}

function readTopic(topic: any) {
  if (topic) {
    topic.hasRead = true
    router.push({ name: 'note-detail', params: { id: topic.id } })
  } else {
    const firstId = topics.value[0]?.id ?? 1
    router.push({ name: 'note-detail', params: { id: firstId } })
  }
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
    const payload = {
      title: newDraft.title,
      content: newDraft.content,
      summary: newDraft.content.slice(0, 120),
      status: 'PUBLISHED',
      tags: newDraft.tags
    }
    await createNote(payload)
    appStore.showToast('发布成功', '笔记已保存到云端')
    await loadTopics()
    resetDraft()
    currentNav.value = 'my-shares'
    activeTab.value = 'latest'
    activeCategory.value = ''
    showPublishModal.value = false
  } catch (e) {
    appStore.showToast('发布失败', '请稍后重试', 'error')
  }
}

function resetDraft() {
  newDraft.title = ''
  newDraft.content = ''
  newDraft.category = ''
  newDraft.tags = []
  newDraft.hasLink = false
  tagInput.value = ''
}

function formatDate(dateStr?: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return dateStr
  return d.toLocaleString()
}

async function loadTopics() {
  loading.value = true
  try {
    const { data } = await fetchNoteList({
      page: pagination.page,
      pageSize: pagination.pageSize,
      status: activeTab.value === 'featured' ? 'PUBLISHED' : undefined
    })
    topics.value = data.data.list || []
    pagination.total = data.data.total
  } catch (e) {
    appStore.showToast('加载笔记失败', '请检查网络或稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

watch([activeTab, activeCategory, currentNav], () => {
  if (activeTab.value === 'latest' || activeTab.value === 'featured') {
    loadTopics()
  }
})

onMounted(() => {
  loadTopics()
})
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
.ai-hot-header:hover .chevron { color: #6b7280; }
.ai-hot-title { display: flex; align-items: center; gap: 6px; font-weight: 600; color: #111827; }
.news-list { display: flex; flex-direction: column; gap: 12px; padding: 12px 16px 12px 32px; }
.news-item { display: flex; align-items: flex-start; gap: 8px; font-size: 13px; color: #4b5563; text-decoration: none; line-height: 1.4; transition: color 0.15s; }
.news-bullet { width: 4px; height: 4px; background: #d1d5db; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
.news-item:hover { color: #2563eb; }
.news-item:hover .news-bullet { background: #2563eb; }

.sidebar-action { margin-top: auto; padding: 16px; }
.publish-btn { width: 100%; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 10px; border-radius: 6px; background: #2563eb; border: none; color: white; font-size: 14px; font-weight: 500; cursor: pointer; transition: background 0.15s; }
.publish-btn:hover { background: #1d4ed8; }
.publish-btn:active { transform: translateY(1px); }
.publish-icon { font-size: 16px; }

.forum-main { flex: 1; padding: 24px; max-width: 1100px; display: flex; flex-direction: column; }
.forum-intro-banner { background: #eff6ff; border: 1px solid #bfdbfe; padding: 12px 16px; border-radius: 4px; color: #1e40af; font-size: 14px; margin-bottom: 20px; transition: all 0.2s; }
.banner-link { color: #2563eb; text-decoration: none; font-weight: 500; }
.forum-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.toolbar-tabs { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.dropdown-wrapper { position: relative; }
.filter-dropdown { display: flex; align-items: center; gap: 4px; background: white; border: 1px solid #d1d5db; padding: 6px 12px; border-radius: 4px; font-size: 13px; color: #374151; cursor: pointer; transition: border 0.15s; }
.filter-dropdown:hover { border-color: #9ca3af; }
.dropdown-menu { position: absolute; top: calc(100% + 4px); left: 0; background: white; border: 1px solid #e5e7eb; border-radius: 6px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); padding: 4px; z-index: 10; min-width: 120px; }
.dropdown-item { padding: 8px 12px; font-size: 13px; color: #374151; border-radius: 4px; cursor: pointer; }
.dropdown-item:hover { background: #f3f4f6; color: #111827; }
.icon-xs { font-size: 10px; }
.tab-link { background: transparent; border: none; font-size: 14px; color: #4b5563; cursor: pointer; padding: 6px 0; font-weight: 400; transition: color 0.15s; }
.tab-link:hover { color: #111827; }
.tab-link.active { color: #2563eb; font-weight: 600; border-bottom: 2px solid #2563eb; }

.topic-list-header { display: flex; padding: 8px 16px; font-size: 12px; color: #6b7280; border-bottom: 1px solid #e5e7eb; }
.header-col { display: flex; align-items: center; }
.title-col { flex: 1; }
.author-col { width: 140px; padding-left: 16px; }
.time-col { width: 100px; text-align: right; justify-content: flex-end; }

.topic-list { flex: 1; display: flex; flex-direction: column; }
.topic-row { display: flex; align-items: flex-start; padding: 16px; border-bottom: 1px solid #f3f4f6; transition: background 0.15s; }
.topic-row:hover { background-color: #fafafa; }
.topic-row.pinned { background-color: #fcfcfd; }
.topic-row.has-read .topic-title { color: #6b7280; }
.list-divider { height: 2px; background: #e5e7eb; margin: 16px 0; border-radius: 2px; }
.topic-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6px; }
.topic-title-wrapper { display: flex; align-items: center; gap: 6px; }
.title-with-link { display: flex; align-items: center; gap: 8px; }
.external-icon { font-size: 14px; color: #9ca3af; cursor: pointer; transition: color 0.15s; }
.external-icon:hover { color: #2563eb; }
.pin-icon { color: #6b7280; font-size: 14px; transform: rotate(45deg); padding-top: 4px; }
.topic-title { font-size: 16px; font-weight: 500; color: #111827; margin: 0; cursor: pointer; line-height: 1.4; transition: color 0.15s; }
.topic-title:hover { color: #2563eb; text-decoration: underline; }
.topic-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 4px; }
.meta-tag { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #4b5563; transition: color 0.15s; }
.meta-tag:hover { color: #111827; }
.dot { width: 8px; height: 8px; border-radius: 2px; }
.tag-gray { background: #9ca3af; }
.badge { font-size: 11px; padding: 2px 6px; border-radius: 4px; display: flex; align-items: center; gap: 4px; }
.badge.blue { background: #e0f2fe; color: #0284c7; }
.badge.gray { background: #f3f4f6; color: #4b5563; }
.badge-icon { font-size: 12px; color: inherit; }
.likes-btn { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #6b7280; background: transparent; border: none; cursor: pointer; padding: 2px 6px; border-radius: 4px; margin-left: auto; transition: all 0.2s; }
.likes-btn:hover { background: #fef3c7; color: #f59e0b; }
.likes-btn.liked { color: #f59e0b; font-weight: 500; background: #fffbeb; }
.likes-btn .icon-xs { font-size: 13px; }
.topic-excerpt { font-size: 14px; color: #4b5563; margin: 4px 0 6px 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.5; }

.topic-author { width: 140px; padding-left: 16px; display: flex; align-items: center; gap: 8px; }
.user-avatar { width: 28px; height: 28px; border-radius: 50%; border: 1px solid #f3f4f6; flex-shrink: 0; object-fit: cover; }
.author-initial { display: flex; align-items: center; justify-content: center; font-size: 12px; color: white; background: #0284c7; }
.author-name { font-size: 13px; color: #4b5563; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.topic-time { width: 100px; text-align: right; font-size: 13px; color: #6b7280; display: flex; align-items: center; justify-content: flex-end; }
.empty-state { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 64px 0; color: #9ca3af; font-size: 14px; min-height: 300px; }
.empty-icon { font-size: 32px; color: #d1d5db; }

/* Publish Editor Modal (Split Pane) */
.modal-overlay { position: absolute; inset: 0; background: rgba(17,24,39,0.6); backdrop-filter: blur(2px); display: flex; align-items: center; justify-content: center; z-index: 100; animation: fadeIn 0.15s ease-out; }
.publish-editor-modal { width: 95vw; max-width: 1200px; height: 85vh; background: white; border-radius: 12px; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04); display: flex; flex-direction: column; overflow: hidden; animation: slideUp 0.2s ease-out; }
.modal-header { padding: 16px 24px; border-bottom: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: center; background: #f9fafb; flex-shrink: 0; }
.modal-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: #111827; }
.close-btn { background: transparent; border: none; font-size: 20px; color: #6b7280; cursor: pointer; border-radius: 4px; display: flex; align-items: center; padding: 4px; transition: background 0.15s; }
.close-btn:hover { background: #e5e7eb; color: #111827; }

.editor-toolbar { padding: 16px 24px; display: flex; flex-direction: column; gap: 12px; border-bottom: 1px solid #e5e7eb; background: #ffffff; flex-shrink: 0; }
.title-input { width: 100%; border: none; font-size: 20px; font-weight: 600; color: #111827; outline: none; padding: 4px 0; }
.title-input::placeholder { color: #9ca3af; font-weight: 400; }
.metadata-row { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; }
.form-select { border: 1px solid #d1d5db; border-radius: 6px; padding: 6px 12px; font-size: 13px; color: #374151; outline: none; background: white; cursor: pointer; min-width: 150px; }

/* Tags Input */
.tags-input-wrapper { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; border: 1px solid #d1d5db; border-radius: 6px; padding: 4px 8px; flex: 1; background: white; min-height: 34px; }
.tag-chip { display: flex; align-items: center; gap: 4px; background: #eff6ff; color: #1d4ed8; font-size: 12px; padding: 2px 8px; border-radius: 12px; font-weight: 500; }
.tag-close { font-size: 12px; cursor: pointer; color: #60a5fa; transition: color 0.15s; }
.tag-close:hover { color: #1d4ed8; }
.tag-input-field { border: none; outline: none; font-size: 13px; color: #374151; min-width: 150px; flex: 1; background: transparent; padding: 2px; }

.checkbox-label { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #4b5563; cursor: pointer; user-select: none; }

.editor-panes { display: flex; flex: 1; min-height: 0; overflow: hidden; background: #fdfdfd; }
.pane { flex: 1; overflow-y: auto; padding: 24px; width: 50%; }
.editor-pane { border-right: 1px solid #e5e7eb; background: #ffffff; }
.preview-pane { background: #f9fafb; border-left: 1px solid #e5e7eb; margin-left: -1px; }

.md-textarea { width: 100%; height: 100%; border: none; resize: none; outline: none; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 14px; line-height: 1.6; color: #374151; background: transparent; }
.md-textarea::placeholder { color: #9ca3af; }

/* Markdown Preview Styles */
.markdown-body { color: #111827; font-size: 15px; line-height: 1.7; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; word-break: break-word; }
.markdown-body :deep(h1) { font-size: 24px; font-weight: 600; margin: 0 0 16px 0; border-bottom: 1px solid #e5e7eb; padding-bottom: 8px; }
.markdown-body :deep(h2) { font-size: 20px; font-weight: 600; margin: 24px 0 16px 0; }
.markdown-body :deep(h3) { font-size: 18px; font-weight: 600; margin: 24px 0 16px 0; }
.markdown-body :deep(p) { margin: 0 0 16px 0; }
.markdown-body :deep(blockquote) { border-left: 4px solid #cbd5e1; padding-left: 16px; color: #6b7280; margin: 0 0 16px 0; background: #f3f4f6; border-radius: 0 4px 4px 0; padding: 12px 16px; }
.markdown-body :deep(pre) { background: #1f2937; color: #f3f4f6; padding: 16px; border-radius: 8px; overflow-x: auto; margin: 0 0 16px 0; font-family: monospace; font-size: 14px; }
.markdown-body :deep(code) { background: #f1f5f9; color: #ef4444; padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 13px; }
.markdown-body :deep(pre code) { background: transparent; color: inherit; padding: 0; }
.markdown-body :deep(ul) { margin: 0 0 16px 0; padding-left: 24px; list-style-type: disc; }
.markdown-body :deep(li) { margin-bottom: 8px; }
.markdown-body :deep(strong) { font-weight: 600; color: #111827; }
.placeholder-text { color: #9ca3af; font-style: italic; text-align: center; margin-top: 40px; }

.modal-footer { padding: 16px 24px; background: white; border-top: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: center; flex-shrink: 0; }
.word-count { font-size: 13px; color: #9ca3af; }
.footer-actions { display: flex; gap: 12px; margin-left: auto; }
.btn-cancel { padding: 8px 20px; background: white; border: 1px solid #d1d5db; border-radius: 6px; font-size: 14px; font-weight: 500; color: #374151; cursor: pointer; transition: all 0.15s; }
.btn-cancel:hover { background: #f9fafb; border-color: #9ca3af; }
.btn-submit { padding: 8px 24px; background: #2563eb; border: none; border-radius: 6px; font-size: 14px; font-weight: 500; color: white; cursor: pointer; transition: background 0.15s; }
.btn-submit:hover { background: #1d4ed8; }
.btn-submit:disabled { background: #93c5fd; cursor: not-allowed; }

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes slideUp { from { opacity: 0; transform: translateY(20px) scale(0.98); } to { opacity: 1; transform: translateY(0) scale(1); } }

@media (max-width: 1024px) { .forum-sidebar { display: none; } }
</style>
