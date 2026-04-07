import re

with open('frontend/src/views/note/NoteEditorView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add drag & drop events to folders
folder_title_new = """<div class="folder-title" @click="toggleFolder(folder.id)"
                  @dragover.prevent
                  @drop="onDropFolder($event, folder.id)"
                  :class="{ 'drag-over': dragTargetFolder === folder.id }"
                  @dragenter.prevent="dragTargetFolder = folder.id"
                  @dragleave="dragTargetFolder = null"
                >"""
content = re.sub(r'<div class="folder-title" @click="toggleFolder\(folder\.id\)">', folder_title_new, content)

# 2. Add drag & drop events to tree-notes inside folder
tree_note_folder_old = """<div 
                    v-for="note in getNotesByFolder(folder.id)" 
                    :key="note.id" 
                    class="tree-note"
                    :class="{ active: store.activeNoteId === note.id }"
                    @click="openNote(note.id)"
                  >"""
tree_note_folder_new = """<div 
                    v-for="note in getNotesByFolder(folder.id)" 
                    :key="note.id" 
                    class="tree-note"
                    :class="{ active: store.activeNoteId === note.id, 'drag-over-note': dragTargetNote === note.id }"
                    @click="openNote(note.id)"
                    draggable="true"
                    @dragstart="onDragStart($event, note.id)"
                    @dragover.prevent
                    @drop.stop="onDropNote($event, note.id, folder.id)"
                    @dragenter.prevent="dragTargetNote = note.id"
                    @dragleave="dragTargetNote = null"
                  >"""
content = content.replace(tree_note_folder_old, tree_note_folder_new)

# 3. Add drag & drop to root-level tree-notes
tree_note_root_old = """<div 
                    v-for="note in getNotesByFolder(null)" 
                    :key="note.id" 
                    class="tree-note"
                    :class="{ active: store.activeNoteId === note.id }"
                    @click="openNote(note.id)"
                  >"""
tree_note_root_new = """<div 
                    v-for="note in getNotesByFolder(null)" 
                    :key="note.id" 
                    class="tree-note"
                    :class="{ active: store.activeNoteId === note.id, 'drag-over-note': dragTargetNote === note.id }"
                    @click="openNote(note.id)"
                    draggable="true"
                    @dragstart="onDragStart($event, note.id)"
                    @dragover.prevent
                    @drop.stop="onDropNote($event, note.id, null)"
                    @dragenter.prevent="dragTargetNote = note.id"
                    @dragleave="dragTargetNote = null"
                  >"""
content = content.replace(tree_note_root_old, tree_note_root_new)

# 4. Add drag drop root zone
root_items_old = """<div class="folder-items no-indent pt-1">"""
root_items_new = """<div class="folder-items no-indent pt-1"
                @dragover.prevent
                @drop="onDropFolder($event, null)"
                :class="{ 'drag-over': dragTargetFolder === null }"
                @dragenter.prevent="dragTargetFolder = null"
                @dragleave="dragTargetFolder = 'leave'"
              >"""
content = content.replace(root_items_old, root_items_new)


# 5. Add Editor toolbar Delete Note button
toolbar_old = """<!-- 编辑/阅读模式切换 -->
            <div class="mode-toggle bg-gray-100 p-1 flex rounded-md ml-auto mr-4 text-sm font-medium">"""
toolbar_new = """<button class="toggle-sidebar-btn ml-auto hover:text-red-500 mr-2" @click="deleteNote(activeNote.id)" title="删除当前笔记">
              <div class="i-carbon-trash-can"></div>
            </button>
            <!-- 编辑/阅读模式切换 -->
            <div class="mode-toggle bg-gray-100 p-1 flex rounded-md mr-4 text-sm font-medium">"""
content = content.replace(toolbar_old, toolbar_new)

# 6. Add script variables
script_vars = """// 当前选中标签下的笔记
const notesByTag = computed(() => {
  if (!activeFilterTag.value) return []
  return store.notes.filter(n => n.tags && n.tags.includes(activeFilterTag.value!))
})

// 拖拽相关状态
const draggedNoteId = ref<string | null>(null)
const dragTargetFolder = ref<string | null | 'leave'>(null)
const dragTargetNote = ref<string | null>(null)

const onDragStart = (e: DragEvent, id: string) => {
  draggedNoteId.value = id
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', id)
  }
}

const onDropFolder = (e: DragEvent, folderId: string | null) => {
  dragTargetFolder.value = null
  if (!draggedNoteId.value) return
  // 如果直接放置在文件夹上，我们把它移到该文件夹的最后
  store.reorderNote(draggedNoteId.value, '', folderId)
  draggedNoteId.value = null
}

const onDropNote = (e: DragEvent, targetNoteId: string, folderId: string | null) => {
  dragTargetNote.value = null
  if (!draggedNoteId.value) return
  
  if (draggedNoteId.value !== targetNoteId) {
    store.reorderNote(draggedNoteId.value, targetNoteId, folderId)
  }
  
  draggedNoteId.value = null
}
"""
content = content.replace("// 当前选中标签下的笔记\nconst notesByTag = computed(() => {\n  if (!activeFilterTag.value) return []\n  return store.notes.filter(n => n.tags && n.tags.includes(activeFilterTag.value!))\n})", script_vars)

# 7. Add scoped styles for drag & drop
style_new = """/* 文件树视图 */
.tree-folder { display: flex; flex-direction: column; margin-bottom: 2px; }
.folder-title {
  display: flex; align-items: center; padding: 4px 16px; font-size: 13px; font-weight: 500;
  color: #4b5563; cursor: pointer; border-radius: 4px; margin: 0 8px; transition: background 0.2s;
}
.folder-title.drag-over { background-color: #e0e7ff; outline: 1px dashed #8b5cf6; }
.folder-items.no-indent.drag-over { background-color: #f5f3ff; min-height: 20px; }
.tree-note.drag-over-note { border-top: 2px solid #8b5cf6; }"""

content = content.replace("/* 文件树视图 */\n.tree-folder { display: flex; flex-direction: column; margin-bottom: 2px; }\n.folder-title {\n  display: flex; align-items: center; padding: 4px 16px; font-size: 13px; font-weight: 500;\n  color: #4b5563; cursor: pointer; border-radius: 4px; margin: 0 8px;\n}", style_new)


with open('frontend/src/views/note/NoteEditorView.vue', 'w', encoding='utf-8') as f:
    f.write(content)
