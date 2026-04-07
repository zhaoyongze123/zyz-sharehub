import { defineStore } from 'pinia'

export interface LocalFolder {
  id: string
  name: string
  parentId: string | null
  createdAt: number
  updatedAt: number
}

export interface LocalNote {
  id: string
  title: string
  content: string
  folderId: string | null
  tags: string[]
  createdAt: number
  updatedAt: number
}

export interface LocalStorageState {
  initialized: boolean
  folders: LocalFolder[]
  notes: LocalNote[]
  activeNoteId: string | null
  autoSave: boolean
}

export const useLocalNoteStore = defineStore('localNote', {
  state: (): LocalStorageState => {
    const savedState = localStorage.getItem('local_notes_db')
    if (savedState) {
      try {
        const parsed = JSON.parse(savedState)
        return {
          initialized: parsed.initialized || false,
          folders: parsed.folders || [],
          notes: parsed.notes || [],
          activeNoteId: parsed.activeNoteId || null,
          autoSave: parsed.autoSave !== undefined ? parsed.autoSave : true
        }
      } catch (e) {
        console.error('Failed to parse local notes db', e)
      }
    }
    return {
      initialized: false,
      folders: [],
      notes: [],
      activeNoteId: null,
      autoSave: true
    }
  },
  actions: {
    saveToDb() {
      try {
        localStorage.setItem('local_notes_db', JSON.stringify(this.$state))
      } catch (e) {
        console.error('Storage full or unavailable', e)
        throw new Error('Local storage failed')
      }
    },
    initialize() {
      this.initialized = true
      // Create a default folder
      if (this.folders.length === 0) {
        const newId = 'f_' + Date.now()
        this.folders.push({
          id: newId,
          name: '默认笔记本',
          parentId: null,
          createdAt: Date.now(),
          updatedAt: Date.now()
        })
        const noteId = 'n_' + Date.now()
        this.notes.push({
          id: noteId,
          title: '欢迎使用本地笔记',
          content: '这是一篇基于本地存储的笔记。\n\n## 特性\n- 完全本地化，数据不上传云端\n- 自动保存\n- 实时 Markdown 预览\n- 文件夹分类与标签管理',
          folderId: newId,
          tags: ['入门'],
          createdAt: Date.now(),
          updatedAt: Date.now()
        })
        this.activeNoteId = noteId
      }
      this.saveToDb()
    },
    createFolder(name: string, parentId: string | null = null) {
      const folder: LocalFolder = {
        id: 'f_' + Date.now(),
        name,
        parentId,
        createdAt: Date.now(),
        updatedAt: Date.now()
      }
      this.folders.push(folder)
      if (this.autoSave) this.saveToDb()
      return folder
    },
    deleteFolder(id: string) {
      this.folders = this.folders.filter(f => f.id !== id)
      // notes in this folder -> moved to root (null) or deleted? 
      // Let's move them to root
      this.notes.forEach(n => {
        if (n.folderId === id) n.folderId = null
      })
      if (this.autoSave) this.saveToDb()
    },
    renameFolder(id: string, newName: string) {
      const folder = this.folders.find(f => f.id === id)
      if (folder) {
        folder.name = newName
        folder.updatedAt = Date.now()
        if (this.autoSave) this.saveToDb()
      }
    },
    createNote(folderId: string | null = null, title = '未命名笔记') {
      const note: LocalNote = {
        id: 'n_' + Date.now(),
        title,
        content: '',
        folderId,
        tags: [],
        createdAt: Date.now(),
        updatedAt: Date.now()
      }
      this.notes.push(note)
      this.activeNoteId = note.id
      if (this.autoSave) this.saveToDb()
      return note
    },
    updateNote(id: string, updates: Partial<LocalNote>) {
      const note = this.notes.find(n => n.id === id)
      if (note) {
        Object.assign(note, updates)
        note.updatedAt = Date.now()
        if (this.autoSave) this.saveToDb()
      }
    },
    deleteNote(id: string) {
      this.notes = this.notes.filter(n => n.id !== id)
      if (this.activeNoteId === id) this.activeNoteId = null
      if (this.autoSave) this.saveToDb()
    },
    clearAll() {
      this.folders = []
      this.notes = []
      this.activeNoteId = null
      this.initialized = false
      this.saveToDb()
    },
    importData(data: string) {
      try {
        const parsed = JSON.parse(data)
        if (parsed.folders && parsed.notes) {
          this.folders = parsed.folders
          this.notes = parsed.notes
          this.initialized = true
          this.saveToDb()
          return true
        }
      } catch (e) {
        return false
      }
      return false
    },
    exportData() {
      return JSON.stringify({
        folders: this.folders,
        notes: this.notes,
        version: '1.0'
      }, null, 2)
    }
  },
  getters: {
    activeNote: (state) => state.notes.find(n => n.id === state.activeNoteId) || null,
    folderTree: (state) => {
      // simple 1-level tree for now or mapped
      const rootFolders = state.folders.filter(f => !f.parentId)
      return rootFolders.map(f => ({
        ...f,
        children: state.folders.filter(sub => sub.parentId === f.id)
      }))
    }
  }
})
