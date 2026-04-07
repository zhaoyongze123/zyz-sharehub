<template>
  <div class="note-workbench">
    <!-- 未初始化界面 -->
    <div v-if="!store.initialized" class="init-overlay">
      <div class="init-card">
        <div class="i-carbon-folder-open init-icon"></div>
        <h2>初始化本地笔记库</h2>
        <p class="desc">您的笔记将完全保存在本地浏览器中，不会上传到任何云端服务器。</p>
        <div class="storage-options">
          <div class="storage-mode active">
            <div class="i-carbon-data-base mode-icon"></div>
            <div class="mode-info">
              <h4>浏览器缓存模式 (推荐)</h4>
              <span>使用 IndexedDB/LocalStorage 进行存储，无需额外授权。</span>
            </div>
            <div class="i-carbon-checkmark-outline check-icon"></div>
          </div>
        </div>
        <button class="btn-primary start-btn" @click="startInitialize">开始创建本地笔记库</button>
      </div>
    </div>

    <!-- 工作台界面 (Obsidian 风格) -->
    <template v-else>
      <!-- 最左侧图标功能丝带 (App Ribbon)：切换视图按钮 -->
      <nav class="app-ribbon">
        <div class="ribbon-top">
          <button class="ribbon-btn" :class="{active: viewMode === 'files'}" @click="viewMode = 'files'" title="文件树视图">
            <div class="i-carbon-folder"></div>
          </button>
          <button class="ribbon-btn" :class="{active: viewMode === 'search'}" @click="viewMode = 'search'" title="全文搜索">
            <div class="i-carbon-search"></div>
          </button>
          <button class="ribbon-btn" :class="{active: viewMode === 'tags'}" @click="viewMode = 'tags'" title="标签筛选">
            <div class="i-carbon-tag"></div>
          </button>
        </div>
        <div class="ribbon-bottom">
          <button class="ribbon-btn" title="本地库设置" @click="$router.push('/notes/settings')">
            <div class="i-carbon-settings"></div>
          </button>
        </div>
      </nav>

      <!-- 侧边导航栏 -->
      <aside class="sidebar-left" v-show="isSidebarOpen">
        <!-- 工作区名称和操作 -->
        <div class="sidebar-header">
          <span class="workspace-name dropdown-trigger" title="工作区名称">ShareHub Vault <div class="i-carbon-chevron-down text-xs ml-1"></div></span>
          <div class="actions">
            <!-- 新建按钮 -->
            <button title="新建笔记" @click="createNewNote"><div class="i-carbon-document-add"></div></button>
            <button title="新建文件夹" @click="promptCreateFolder"><div class="i-carbon-folder-add"></div></button>
          </div>
        </div>

        <!-- 当前视图模式对应的内容 -->
        <div class="sidebar-content">
          <!-- 1. 文件树视图 (包含文件夹树和笔记列表) -->
          <div v-show="viewMode === 'files'" class="view-pane files-view">
            <div class="folder-tree">
              <div 
                v-for="folder in store.folders" 
                :key="folder.id" 
                class="tree-folder"
              >
                <!-- 文件夹节点 -->
                <div class="folder-title" @click="toggleFolder(folder.id)">
                  <div class="i-carbon-chevron-down outline-chevron transition-transform duration-200" :class="{ '-rotate-90': collapsedFolders.includes(folder.id) }"></div>
                  <div class="i-carbon-folder mr-1.5 opacity-70"></div>
                  <span class="name">{{ folder.name }}</span>
                  <button class="icon-btn delete-btn" @click.stop="deleteFolder(folder.id)" title="删除文件夹">
                    <div class="i-carbon-trash-can"></div>
                  </button>
                </div>
                <!-- 文件夹下的笔记列表 -->
                <div class="folder-items" v-show="!collapsedFolders.includes(folder.id)">
                  <div 
                    v-for="note in getNotesByFolder(folder.id)" 
                    :key="note.id" 
                    class="tree-note"
                    :class="{ active: store.activeNoteId === note.id }"
                    @click="openNote(note.id)"
                  >
                    <div class="i-carbon-document text-xs opacity-50"></div>
                    <span class="name">{{ note.title || '未命名笔记' }}</span>
                    <button class="icon-btn delete-btn" @click.stop="deleteNote(note.id)" title="删除笔记">
                      <div class="i-carbon-trash-can"></div>
                    </button>
                  </div>
                </div>
              </div>
              
              <!-- 根目录下的独立笔记列表 -->
              <div class="folder-items no-indent pt-1">
                 <div 
                    v-for="note in getNotesByFolder(null)" 
                    :key="note.id" 
                    class="tree-note"
                    :class="{ active: store.activeNoteId === note.id }"
                    @click="openNote(note.id)"
                  >
                    <div class="i-carbon-document text-xs opacity-50"></div>
                    <span class="name">{{ note.title || '未命名笔记' }}</span>
                    <button class="icon-btn delete-btn" @click.stop="deleteNote(note.id)" title="删除笔记">
                      <div class="i-carbon-trash-can"></div>
                    </button>
                  </div>
              </div>
            </div>
          </div>

          <!-- 2. 搜索视图 (包含搜索框) -->
          <div v-show="viewMode === 'search'" class="view-pane search-view">
            <div class="search-box">
              <input type="text" v-model="searchQuery" placeholder="搜索笔记..." />
              <div class="i-carbon-search search-icon"></div>
              <div class="i-carbon-close clear-icon" v-show="searchQuery" @click="searchQuery = ''"></div>
            </div>
            <div class="search-results">
              <div class="tree-note" v-for="note in filteredAllNotes" :key="note.id" @click="openNote(note.id)" :class="{ active: store.activeNoteId === note.id }">
                <div class="i-carbon-document"></div>
                <span class="name">{{ note.title || '未命名笔记' }}</span>
              </div>
              <div v-if="filteredAllNotes.length === 0" class="empty-hint">没找到相关笔记。</div>
            </div>
          </div>

          <!-- 3. 标签筛选视图 -->
          <div v-show="viewMode === 'tags'" class="view-pane tags-view">
             <div class="tags-header text-xs text-gray-500 font-semibold mb-2 px-3">标签筛选</div>
             <div class="tags-list px-3 flex flex-wrap gap-2">
                <span 
                  v-for="tag in allTags" 
                  :key="tag" 
                  class="tag-pill" 
                  :class="{active: activeFilterTag === tag}"
                  @click="activeFilterTag = activeFilterTag === tag ? null : tag"
                >
                  <div class="i-carbon-hashtag"></div> {{ tag }}
                </span>
             </div>
             <div v-if="allTags.length === 0" class="empty-hint">尚未添加任何标签。</div>
             
             <div class="tags-header text-xs text-gray-500 font-semibold mt-4 mb-2 px-3" v-if="activeFilterTag">具有 "{{ activeFilterTag }}" 的笔记</div>
             <div class="folder-items no-indent" v-if="activeFilterTag">
               <div class="tree-note" v-for="note in notesByTag" :key="note.id" @click="openNote(note.id)" :class="{ active: store.activeNoteId === note.id }">
                  <div class="i-carbon-document"></div>
                  <span class="name">{{ note.title || '未命名笔记' }}</span>
               </div>
             </div>
          </div>
        </div>
      </aside>

      <!-- 中间编辑器区域 -->
      <main class="editor-main">
        <template v-if="activeNote">
          <div class="editor-toolbar">
            <button class="toggle-sidebar-btn" @click="isSidebarOpen = !isSidebarOpen" title="收起/展开面板">
              <div class="i-carbon-side-panel-open" v-if="isSidebarOpen"></div>
              <div class="i-carbon-side-panel-close" v-else></div>
            </button>
            <input 
              v-model="activeNote.title" 
              class="title-input" 
              placeholder="未命名" 
              @input="triggerAutoSave"
            />
            <div class="save-status">
              <div v-if="saveStatus === 'saving'" class="status saving"><div class="i-carbon-progress-5 spin"></div> 保存中...</div>
              <div v-else-if="saveStatus === 'saved'" class="status success"><div class="i-carbon-checkmark"></div> 已保存</div>
              <div v-else-if="saveStatus === 'error'" class="status error"><div class="i-carbon-warning"></div> 存储失败</div>
            </div>
            <!-- 右侧大纲切换 -->
            <button class="toggle-sidebar-btn ml-auto" @click="isRightSidebarOpen = !isRightSidebarOpen" title="右侧大纲">
              <div class="i-carbon-list"></div>
            </button>
          </div>

          <div class="tags-bar">
            <div class="i-carbon-tag"></div>
            <div class="tags-list">
              <span v-for="(tag, index) in activeNote.tags || []" :key="index" class="tag">
                {{ tag }}
                <div class="i-carbon-close tag-close" @click="removeTag(index)"></div>
              </span>
              <input 
                v-model="newTag" 
                class="tag-add-input" 
                placeholder="添加标签..." 
                @keyup.enter="addTag"
              />
            </div>
          </div>

          <!-- 真正的编辑器部分 -->
          <div class="editor-content-area">
            <textarea 
              v-model="activeNote.content" 
              class="markdown-editor" 
              placeholder="开始编写您的 Markdown 笔记... (可以输入大纲标题)"
              @input="triggerAutoSave"
            ></textarea>
          </div>
        </template>
        <div v-else class="empty-editor">
          <!-- 切换视图按钮当没有侧栏打开时可以在这里打开侧栏 -->
           <button class="toggle-sidebar-btn empty-state-toggle" v-if="!isSidebarOpen" @click="isSidebarOpen = true" title="展开侧边栏">
              <div class="i-carbon-side-panel-close"></div> 展开工作区
            </button>
          <div class="i-carbon-document-blank empty-icon"></div>
          <h3>无打开的笔记</h3>
          <p>分享站本地工作区：数据完全离线保存</p>
          <button class="btn-primary" @click="createNewNote">新建笔记</button>
        </div>
      </main>

      <!-- 右栏：大纲面板 -->
      <aside class="sidebar-right" v-show="isRightSidebarOpen && activeNote">
        <div class="outline-header">
          <span>大纲</span>
          <button class="close-outline" @click="isRightSidebarOpen = false"><div class="i-carbon-close"></div></button>
        </div>
        <div class="outline-content">
          <ul class="toc-list" v-if="outline.length > 0">
            <li v-for="(head, index) in outline" :key="index" :class="'toc-h' + head.level">
              {{ head.text }}
            </li>
          </ul>
          <div class="empty-toc" v-else>
            笔记中尚未添加标题
          </div>
        </div>
      </aside>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useLocalNoteStore } from '@/stores/localNote'
import { useRouter } from 'vue-router'

const router = useRouter()
const store = useLocalNoteStore()

// 控制布局展开与收起 (切换视图按钮功能)
const isSidebarOpen = ref(true)
const isRightSidebarOpen = ref(true)

// 当前左侧面板的视图模式 (文件树 files / 搜索框 search / 标签筛选 tags)
const viewMode = ref<'files'|'search'|'tags'>('files')
const searchQuery = ref('')
const activeFilterTag = ref<string | null>(null)

// 文件夹折叠状态
const collapsedFolders = ref<string[]>([])

const saveStatus = ref<'idle' | 'saving' | 'saved' | 'error'>('idle')
let saveTimeout: any = null

const activeNote = computed(() => store.activeNote)
const newTag = ref('')

const startInitialize = () => {
  store.initialize()
}

// 文件夹层级的笔记获取 (文件夹树)
const getNotesByFolder = (folderId: string | null) => {
  let notes = store.notes.filter(n => n.folderId === folderId)
  return notes
}

// 全部笔记的搜索过滤 (搜索框视图)
const filteredAllNotes = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return []
  return store.notes.filter(n => n.title.toLowerCase().includes(q) || n.content.toLowerCase().includes(q))
})

// 所有的标签 (标签筛选)
const allTags = computed(() => {
  const tagSet = new Set<string>()
  store.notes.forEach(n => {
    if (n.tags) {
      n.tags.forEach(t => tagSet.add(t))
    }
  })
  return Array.from(tagSet)
})

// 当前选中标签下的笔记
const notesByTag = computed(() => {
  if (!activeFilterTag.value) return []
  return store.notes.filter(n => n.tags && n.tags.includes(activeFilterTag.value!))
})

// 新建按钮
const createNewNote = () => {
  const folderId = store.folders.length > 0 ? store.folders[0].id : null
  store.createNote(folderId, 'New Note')
  if (!isSidebarOpen.value) {
      isSidebarOpen.value = true
  }
}

const openNote = (id: string) => {
  store.activeNoteId = id
  saveStatus.value = 'idle'
}

const toggleFolder = (id: string) => {
  const idx = collapsedFolders.value.indexOf(id)
  if (idx === -1) {
    collapsedFolders.value.push(id)
  } else {
    collapsedFolders.value.splice(idx, 1)
  }
}

const deleteNote = (id: string) => {
  if (confirm('确定要移至废纸篓？本地删除无法恢复！')) {
    store.deleteNote(id)
  }
}

const promptCreateFolder = () => {
  const name = prompt('输入新文件夹名称')
  if (name && name.trim()) {
    store.createFolder(name.trim())
  }
}

const deleteFolder = (id: string) => {
  if (confirm('确定要删除文件夹吗？其下的笔记将被移至根目录。')) {
    store.deleteFolder(id)
  }
}

const triggerAutoSave = () => {
  if (!store.autoSave) return
  
  saveStatus.value = 'saving'
  if (saveTimeout) clearTimeout(saveTimeout)
  
  saveTimeout = setTimeout(() => {
    try {
      if (activeNote.value) {
        store.updateNote(activeNote.value.id, { 
          title: activeNote.value.title,
          content: activeNote.value.content,
          tags: activeNote.value.tags 
        })
      }
      saveStatus.value = 'saved'
      setTimeout(() => { if(saveStatus.value === 'saved') saveStatus.value = 'idle'}, 3000)
    } catch(e) {
      saveStatus.value = 'error'
    }
  }, 1000)
}

const addTag = () => {
  if (!activeNote.value) return
  const tag = newTag.value.trim()
  if (tag && !activeNote.value.tags) activeNote.value.tags = []
  if (tag && !activeNote.value.tags.includes(tag)) {
    activeNote.value.tags.push(tag)
    triggerAutoSave()
  }
  newTag.value = ''
}

const removeTag = (index: number) => {
  if (!activeNote.value || !activeNote.value.tags) return
  activeNote.value.tags.splice(index, 1)
  triggerAutoSave()
}

// 大纲提取
const outline = computed(() => {
  if (!activeNote.value) return []
  const lines = activeNote.value.content.split('\n')
  const result: any[] = []
  for (const line of lines) {
    const match = line.match(/^(#{1,6})\s+(.+)/)
    if (match) {
      result.push({
        level: match[1].length,
        text: match[2].trim()
      })
    }
  }
  return result
})
</script>

<style scoped>
.note-workbench {
  display: flex;
  height: calc(100vh - 60px); 
  background-color: transparent;
  background: var(--c-bg, #ffffff);
  color: #1f2937;
  overflow: hidden;
  width: 100%;
}

/* 初始化状态 (省略大部分保持原样) */
.init-overlay {
  display: flex; width: 100%; height: 100%; align-items: center; justify-content: center; background-color: #f9fafb;
}
.init-card { width: 600px; padding: 40px; background: white; border-radius: 12px; box-shadow: 0 8px 24px rgba(0,0,0,0.1); text-align: center; }
.init-icon { font-size: 48px; color: #8b5cf6; margin: 0 auto 16px auto; }
.storage-mode.active { border-color: #8b5cf6; background-color: #f5f3ff; }
.mode-icon, .check-icon { margin-right: 16px; color: #8b5cf6; font-size: 32px; }
.check-icon { position: absolute; right: 16px; font-size: 24px; }
.start-btn { width: 100%; padding: 12px; font-size: 16px; background: #8b5cf6; color: white; border-radius: 6px; cursor: pointer; border: none; margin-top: 16px; }

/* 最左侧工具丝带 App Ribbon */
.app-ribbon {
  width: 48px;
  background-color: #f3f4f6;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  flex-shrink: 0;
}
.ribbon-top, .ribbon-bottom { display: flex; flex-direction: column; gap: 16px; }
.ribbon-btn {
  width: 32px; height: 32px;
  border-radius: 6px;
  border: none; background: transparent;
  display: flex; align-items: center; justify-content: center;
  color: #6b7280; font-size: 20px; cursor: pointer;
  transition: all 0.2s;
}
.ribbon-btn:hover { color: #111827; background-color: #e5e7eb; }
.ribbon-btn.active { color: #8b5cf6; background-color: #ede9fe; }

/* 左侧栏 Sidebar Left */
.sidebar-left {
  width: 280px;
  background-color: #f9fafb;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.sidebar-header {
  height: 48px;
  padding: 0 16px;
  display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}
.workspace-name {
  font-weight: 600; font-size: 14px; color: #374151;
  display: flex; align-items: center; cursor: pointer;
}
.workspace-name:hover { background-color: #e5e7eb; border-radius: 4px; padding: 2px 4px; margin-left: -4px; }
.sidebar-header .actions button { cursor: pointer; color: #6b7280; background: none; border: none; padding: 4px; border-radius: 4px; margin-left: 2px; }
.sidebar-header .actions button:hover { background-color: #e5e7eb; color: #111827; }

.sidebar-content {
  flex: 1; overflow-y: auto; padding: 8px 0;
}
.view-pane { display: flex; flex-direction: column; }

/* 文件树视图 */
.tree-folder { display: flex; flex-direction: column; margin-bottom: 2px; }
.folder-title {
  display: flex; align-items: center; padding: 4px 16px; font-size: 13px; font-weight: 500;
  color: #4b5563; cursor: pointer; border-radius: 4px; margin: 0 8px;
}
.folder-title:hover { background-color: #f3f4f6; color: #111827; }
.outline-chevron { font-size: 14px; margin-right: 4px; opacity: 0.6; }
.folder-items { padding-left: 18px; display: flex; flex-direction: column; }
.folder-items.no-indent { padding-left: 8px; }

/* 笔记节点样式 */
.tree-note {
  display: flex; align-items: center; padding: 4px 12px; font-size: 13px;
  cursor: pointer; border-radius: 6px; margin: 1px 8px; gap: 6px;
  color: #4b5563; transition: background 0.1s;
}
.tree-note .name { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; flex: 1; }
.tree-note:hover { background-color: #f3f4f6; }
.tree-note.active { background-color: #e5e7eb; color: #8b5cf6; font-weight: 500; }
.tree-note.active .i-carbon-document { opacity: 1; color: #8b5cf6; }

/* 操作按钮悬浮显示 */
.icon-btn { background: none; border: none; cursor: pointer; color: #9ca3af; opacity: 0; padding: 2px; transition: opacity 0.2s; margin-left: auto; }
.folder-title:hover .icon-btn, .tree-note:hover .icon-btn { opacity: 1; }
.icon-btn:hover { color: #ef4444; }

/* 搜索视图 */
.search-box {
  margin: 8px 12px 16px 12px; position: relative;
}
.search-box input {
  width: 100%; padding: 6px 28px 6px 28px; border: 1px solid #d1d5db; border-radius: 4px; font-size: 13px;
  background-color: white; outline: none; transition: border-color 0.2s;
}
.search-box input:focus { border-color: #8b5cf6; box-shadow: 0 0 0 2px rgba(139, 92, 246, 0.1); }
.search-icon { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); color: #9ca3af; font-size: 14px; }
.clear-icon { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); color: #9ca3af; font-size: 14px; cursor: pointer; }

.empty-hint { padding: 20px; text-align: center; color: #9ca3af; font-size: 13px; }

/* 标签视图 */
.tag-pill {
  padding: 2px 8px; background-color: #e5e7eb; border-radius: 12px; font-size: 12px;
  color: #4b5563; cursor: pointer; display: inline-flex; align-items: center; gap: 4px;
}
.tag-pill:hover { background-color: #d1d5db; }
.tag-pill.active { background-color: #8b5cf6; color: white; }

/* 中间编辑器 Main Editor */
.editor-main {
  flex: 1; display: flex; flex-direction: column; background: white; min-width: 0;
}
.editor-toolbar {
  height: 48px; border-bottom: 1px solid #f3f4f6;
  display: flex; align-items: center; padding: 0 16px; gap: 12px;
}
.toggle-sidebar-btn { background: none; border: none; cursor: pointer; color: #6b7280; font-size: 20px; padding: 4px; display: flex; align-items: center; border-radius: 4px; }
.toggle-sidebar-btn:hover { background-color: #f3f4f6; color: #111827; }
.empty-state-toggle { font-size: 14px; gap: 8px; margin-bottom: 16px; background-color: #f3f4f6; border: 1px solid #e5e7eb; padding: 8px 16px;}
.title-input {
  font-size: 20px; font-weight: 700; border: none; background: transparent; outline: none; flex: 1; color: #111827;
}
.save-status { font-size: 12px; }
.status { display: flex; align-items: center; gap: 4px; }
.status.saving { color: #9ca3af; } .status.success { color: #8b5cf6; } .status.error { color: #ef4444; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }

/* 编辑区内样式 */
.editor-content-area { flex: 1; padding: 24px 48px; overflow-y: auto; }
.markdown-editor {
  width: 100%; height: 100%; border: none; resize: none; outline: none; background: transparent;
  font-family: inherit; font-size: 16px; line-height: 1.7; color: #374151; margin-top: 16px;
}

/* 标签栏（文章内） */
.tags-bar { padding: 4px 48px 0; display: flex; align-items: center; gap: 12px; margin-top: 16px; }
.tags-bar .i-carbon-tag { color: #9ca3af; font-size: 16px; }
.tags-list { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.tag {
  padding: 1px 8px; background: rgba(139, 92, 246, 0.1); color: #8b5cf6; border-radius: 12px;
  font-size: 12px; display: flex; align-items: center; gap: 4px;
}
.tag-close { cursor: pointer; opacity: 0.6; }
.tag-close:hover { opacity: 1; }
.tag-add-input { border: none; outline: none; background: transparent; font-size: 13px; width: 100px; color: #6b7280; }

.empty-editor { flex: 1; display: flex; flex-direction: column; justify-content: center; align-items: center; color: #9ca3af; }
.empty-icon { font-size: 64px; margin-bottom: 24px; opacity: 0.3; }
.empty-editor h3 { color: #374151; margin-bottom: 8px; }
.empty-editor p { margin-bottom: 24px; font-size: 14px; }
.btn-primary { padding: 8px 16px; background: #8b5cf6; color: white; border: none; border-radius: 6px; cursor: pointer; }
.btn-primary:hover { background: #7c3aed; }

/* 右侧大纲栏 Sidebar Right */
.sidebar-right {
  width: 240px; background-color: #f9fafb; border-left: 1px solid #e5e7eb; display: flex; flex-direction: column; flex-shrink: 0;
}
.outline-header {
  height: 48px; padding: 0 16px; font-weight: 500; font-size: 14px; color: #374151;
  display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid rgba(0,0,0,0.05);
}
.close-outline { background: none; border: none; cursor: pointer; color: #9ca3af; padding: 4px; }
.close-outline:hover { color: #111827; background-color: #e5e7eb; border-radius: 4px; }

.outline-content { padding: 16px; flex: 1; overflow-y: auto; }
.toc-list { list-style: none; padding: 0; margin: 0; }
.toc-list li {
  font-size: 13px; padding: 4px 8px; margin-bottom: 2px; color: #4b5563; cursor: pointer;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; border-radius: 4px;
}
.toc-list li:hover { background-color: #e5e7eb; color: #8b5cf6; }
.toc-h1 { padding-left: 0 !important; font-weight: 600; }
.toc-h2 { padding-left: 12px !important; }
.toc-h3 { padding-left: 24px !important; }
.toc-h4 { padding-left: 36px !important; }
</style>