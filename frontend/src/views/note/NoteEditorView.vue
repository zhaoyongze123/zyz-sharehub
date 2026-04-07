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
          <div class="storage-mode disabled" title="由于浏览器安全限制，暂时无法直接读写本地文件夹">
            <div class="i-carbon-folder mode-icon"></div>
            <div class="mode-info">
              <h4>本地文件夹模式</h4>
              <span>直接在您选择的磁盘文件夹中读写 .md 文件。(开发中)</span>
            </div>
          </div>
        </div>

        <div class="risk-warning">
          <div class="i-carbon-warning-alt"></div>
          <div class="risk-text">
            <strong>防丢提示：</strong> 清除浏览器缓存会导致数据丢失，请务必定期在设置页中<span class="highlight">备份/导出</span>您的数据。
          </div>
        </div>

        <button class="btn-primary start-btn" @click="startInitialize">
          开始创建本地笔记库
        </button>
      </div>
    </div>

    <!-- 工作台界面 -->
    <template v-else>
      <!-- 左栏：文件树与侧边导航 -->
      <aside class="sidebar-left">
        <div class="sidebar-header">
          <span class="title">本地笔记</span>
          <div class="actions">
            <button title="本地库设置" @click="$router.push('/notes/settings')">
              <div class="i-carbon-settings"></div>
            </button>
            <button title="新建笔记" @click="createNewNote">
              <div class="i-carbon-document-add"></div>
            </button>
            <button title="新建文件夹" @click="promptCreateFolder">
              <div class="i-carbon-folder-add"></div>
            </button>
          </div>
        </div>

        <div class="search-box">
          <div class="i-carbon-search search-icon"></div>
          <input type="text" v-model="searchQuery" placeholder="搜索笔记..." />
        </div>

        <nav class="nav-links">
          <button class="nav-item" :class="{ active: viewMode === 'recent' }" @click="viewMode = 'recent'">
            <div class="i-carbon-recently-viewed"></div> 最近打开
          </button>
          <button class="nav-item" :class="{ active: viewMode === 'favorites' }" @click="viewMode = 'favorites'">
            <div class="i-carbon-star"></div> 收藏笔记
          </button>
          <button class="nav-item" :class="{ active: viewMode === 'tags' }" @click="viewMode = 'tags'">
            <div class="i-carbon-tag"></div> 标签筛选
          </button>
        </nav>

        <div class="folder-tree">
          <div class="tree-header" @click="viewMode = 'folders'">
            <div class="i-carbon-chevron-down outline-chevron"></div>
            <span>文件夹</span>
          </div>
          <!-- 简化版文件树结构 -->
          <div class="tree-content" v-show="viewMode === 'folders' || viewMode === 'recent' || viewMode === 'favorites'">
            <div 
              v-for="folder in store.folders" 
              :key="folder.id" 
              class="tree-folder"
            >
              <div class="folder-title">
                <div class="i-carbon-folder"></div>
                <span class="name">{{ folder.name }}</span>
                <button class="icon-btn delete-btn" @click.stop="deleteFolder(folder.id)" title="删除文件夹">
                  <div class="i-carbon-trash-can"></div>
                </button>
              </div>
              <div class="folder-items">
                <div 
                  v-for="note in getNotesByFolder(folder.id)" 
                  :key="note.id" 
                  class="tree-note"
                  :class="{ active: store.activeNoteId === note.id }"
                  @click="openNote(note.id)"
                >
                  <div class="i-carbon-document"></div>
                  <span class="name">{{ note.title || '未命名笔记' }}</span>
                  <button class="icon-btn delete-btn" @click.stop="deleteNote(note.id)" title="删除笔记">
                    <div class="i-carbon-trash-can"></div>
                  </button>
                </div>
              </div>
            </div>
            
            <!-- 根目录下的独立笔记 -->
            <div class="folder-items no-indent">
               <div 
                  v-for="note in getNotesByFolder(null)" 
                  :key="note.id" 
                  class="tree-note"
                  :class="{ active: store.activeNoteId === note.id }"
                  @click="openNote(note.id)"
                >
                  <div class="i-carbon-document"></div>
                  <span class="name">{{ note.title || '未命名笔记' }}</span>
                  <button class="icon-btn delete-btn" @click.stop="deleteNote(note.id)" title="删除笔记">
                    <div class="i-carbon-trash-can"></div>
                  </button>
                </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 中栏：编辑器 -->
      <main class="editor-main">
        <template v-if="activeNote">
          <div class="editor-toolbar">
            <input 
              v-model="activeNote.title" 
              class="title-input" 
              placeholder="未命名笔记" 
              @input="triggerAutoSave"
            />
            <div class="save-status">
              <div v-if="saveStatus === 'saving'" class="status saving">
                <div class="i-carbon-progress-5 spin"></div> 保存中...
              </div>
              <div v-else-if="saveStatus === 'saved'" class="status success">
                <div class="i-carbon-checkmark"></div> 已保存到本地
              </div>
              <div v-else-if="saveStatus === 'error'" class="status error">
                <div class="i-carbon-warning"></div> 存储失败: 空间不足
              </div>
            </div>
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
              placeholder="开始编写您的 Markdown 笔记..."
              @input="triggerAutoSave"
            ></textarea>
          </div>
        </template>
        <div v-else class="empty-editor">
          <div class="i-carbon-document-blank empty-icon"></div>
          <h3>选择或创建一篇笔记</h3>
          <p>所有数据都安全地保存在本地</p>
          <button class="btn-primary" @click="createNewNote">新建笔记</button>
        </div>
      </main>

      <!-- 右栏：大纲和预览 -->
      <aside class="sidebar-right">
        <div class="outline-header">
          <span>内容大纲 / 预览</span>
        </div>
        <div class="outline-content" v-if="activeNote">
          <div class="preview-mode">
            实时预览不可用，将按照纯文本显示。
            你可以随意组织大纲结构...
          </div>
          <!-- 简单的大纲提取 -->
          <ul class="toc-list" v-if="outline.length > 0">
            <li v-for="(head, index) in outline" :key="index" :class="'toc-h' + head.level">
              {{ head.text }}
            </li>
          </ul>
          <div class="empty-toc" v-else>
            暂无标题大纲
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
const viewMode = ref('folders')
const searchQuery = ref('')

const saveStatus = ref<'idle' | 'saving' | 'saved' | 'error'>('idle')
let saveTimeout: any = null

const activeNote = computed(() => store.activeNote)
const newTag = ref('')

const startInitialize = () => {
  store.initialize()
}

const getNotesByFolder = (folderId: string | null) => {
  let notes = store.notes.filter(n => n.folderId === folderId)
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    notes = notes.filter(n => n.title.toLowerCase().includes(q) || n.content.toLowerCase().includes(q))
  }
  return notes
}

const createNewNote = () => {
  const folderId = store.folders.length > 0 ? store.folders[0].id : null
  store.createNote(folderId, '新建笔记')
}

const openNote = (id: string) => {
  store.activeNoteId = id
  saveStatus.value = 'idle'
}

const deleteNote = (id: string) => {
  if (confirm('确定要永久删除这篇笔记吗？本地删除后无法恢复！')) {
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
      setTimeout(() => { if(saveStatus.value === 'saved') saveStatus.value = 'idle'}, 2000)
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

// 大纲提取 (简单解析 markdown 标题)
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
  height: calc(100vh - 60px); /* 假设有顶栏 */
  background-color: transparent;
  color: #111;
  overflow: hidden;
  width: 100%;
}

/* 初始化弹窗 */
.init-overlay {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  background-color: #f9fafb;
}
.init-card {
  width: 600px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
  text-align: center;
}
.init-icon {
  font-size: 48px;
  color: #3b82f6;
  margin-bottom: 16px;
  margin-left: auto;
  margin-right: auto;
}
.init-card h2 {
  margin-bottom: 8px;
  font-size: 24px;
}
.init-card .desc {
  color: #6b7280;
  margin-bottom: 32px;
}
.storage-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 32px;
  text-align: left;
}
.storage-mode {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}
.storage-mode.active {
  border-color: #3b82f6;
  background-color: #f0f9ff;
}
.storage-mode.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.mode-icon {
  font-size: 32px;
  margin-right: 16px;
  color: #3b82f6;
}
.mode-info h4 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
}
.mode-info span {
  font-size: 14px;
  color: #6b7280;
}
.check-icon {
  position: absolute;
  right: 16px;
  font-size: 24px;
  color: #3b82f6;
}

.risk-warning {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  background: #fefce8;
  border-radius: 8px;
  margin-bottom: 32px;
  text-align: left;
  border-left: 4px solid #facc15;
}
.i-carbon-warning-alt {
  color: #facc15;
  font-size: 20px;
  margin-right: 12px;
  margin-top: 2px;
}
.risk-text {
  font-size: 14px;
  line-height: 1.5;
}
.highlight {
  color: #ef4444;
  font-weight: 500;
}

.start-btn {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  background: #2563eb;
  color: white;
  border-radius: 6px;
  cursor: pointer;
  border: none;
}
.start-btn:hover {
  background: #1d4ed8;
}

/* 工作台布局 */
.sidebar-left {
  width: 260px;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  background: white;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e5e7eb;
}
.sidebar-header .title {
  font-weight: 600;
  font-size: 16px;
}
.sidebar-header .actions {
  display: flex;
  gap: 8px;
}
.sidebar-header .actions button {
  background: none;
  border: none;
  cursor: pointer;
  color: #6b7280;
  font-size: 18px;
  padding: 4px;
  border-radius: 4px;
}
.sidebar-header .actions button:hover {
  background: #f3f4f6;
  color: #1f2937;
}

.search-box {
  padding: 12px 16px;
  position: relative;
}
.search-box input {
  width: 100%;
  padding: 8px 12px 8px 32px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
}
.search-box .search-icon {
  position: absolute;
  left: 24px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
}

.nav-links {
  padding: 0 8px;
  margin-bottom: 16px;
}
.nav-item {
  width: 100%;
  text-align: left;
  padding: 8px 12px;
  border: none;
  background: none;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #374151;
}
.nav-item:hover {
  background: #f3f4f6;
}
.nav-item.active {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 500;
}

.folder-tree {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}
.tree-header {
  padding: 8px 12px;
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  text-transform: uppercase;
}
.tree-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tree-folder {
  display: flex;
  flex-direction: column;
}
.folder-title {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  cursor: pointer;
  border-radius: 6px;
}
.folder-title:hover {
  background: #f3f4f6;
}
.folder-title .icon-btn {
  margin-left: auto;
}
.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #9ca3af;
  opacity: 0;
  transition: opacity 0.2s;
  padding: 2px;
}
.folder-title:hover .icon-btn, .tree-note:hover .icon-btn {
  opacity: 1;
}
.icon-btn:hover {
  color: #ef4444;
}
.folder-items {
  padding-left: 24px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.folder-items.no-indent {
  padding-left: 0;
}
.tree-note {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  border-radius: 6px;
  gap: 8px;
  color: #4b5563;
}
.tree-note .name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}
.tree-note:hover {
  background: #f3f4f6;
}
.tree-note.active {
  background: #2563eb;
  color: white;
}
.tree-note.active .icon-btn {
  color: rgba(255,255,255,0.7);
}
.tree-note.active .icon-btn:hover {
  color: white;
}

/* 中间编辑器 */
.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
}
.editor-toolbar {
  padding: 16px 24px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title-input {
  font-size: 24px;
  font-weight: 600;
  border: none;
  background: transparent;
  outline: none;
  flex: 1;
  margin-right: 16px;
  color: #111827;
}
.save-status {
  font-size: 13px;
}
.status {
  display: flex;
  align-items: center;
  gap: 6px;
}
.status.saving { color: #9ca3af; }
.status.success { color: #10b981; }
.status.error { color: #ef4444; }

.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  100% { transform: rotate(360deg); }
}

.tags-bar {
  padding: 8px 24px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 12px;
}
.tags-bar .i-carbon-tag {
  color: #9ca3af;
}
.tags-list {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.tag {
  padding: 2px 8px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #4b5563;
}
.tag-close {
  cursor: pointer;
  opacity: 0.6;
}
.tag-close:hover {
  opacity: 1;
}
.tag-add-input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  width: 100px;
}

.editor-content-area {
  flex: 1;
  padding: 24px;
  overflow: hidden;
}
.markdown-editor {
  width: 100%;
  height: 100%;
  border: none;
  resize: none;
  outline: none;
  background: transparent;
  font-family: monospace;
  font-size: 15px;
  line-height: 1.6;
  color: #1f2937;
}

.empty-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #9ca3af;
}
.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}
.empty-editor h3 {
  color: #1f2937;
  margin-bottom: 8px;
}
.empty-editor p {
  margin-bottom: 24px;
}

.btn-primary {
  padding: 8px 16px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.btn-primary:hover {
  background: #1d4ed8;
}

/* 右栏：大纲 */
.sidebar-right {
  width: 240px;
  border-left: 1px solid #e5e7eb;
  background: #f9fafb;
  display: flex;
  flex-direction: column;
}
.outline-header {
  padding: 16px;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
  font-size: 14px;
  color: #374151;
}
.outline-content {
  padding: 16px;
  flex: 1;
  overflow-y: auto;
}
.preview-mode {
  background: white;
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
  margin-bottom: 24px;
  border: 1px solid #e5e7eb;
}
.toc-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.toc-list li {
  font-size: 13px;
  padding: 4px 0;
  color: #4b5563;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.toc-list li:hover {
  color: #2563eb;
}
.toc-h1 { padding-left: 0 !important; font-weight: 600; }
.toc-h2 { padding-left: 12px !important; }
.toc-h3 { padding-left: 24px !important; }
.toc-h4 { padding-left: 36px !important; }
.empty-toc {
  font-size: 13px;
  color: #9ca3af;
  text-align: center;
  margin-top: 40px;
}
</style>