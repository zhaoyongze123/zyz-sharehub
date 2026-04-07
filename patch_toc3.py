with open('frontend/src/views/note/NoteEditorView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update refs in template
textarea_old = """<textarea 
              v-if="editMode === 'edit'\""""
textarea_new = """<textarea 
              ref="editorTextareaRef"
              v-if="editMode === 'edit'\""""
content = content.replace(textarea_old, textarea_new)

preview_old = """<div 
              v-else 
              class="markdown-preview"""
preview_new = """<div 
              ref="previewContainerRef"
              v-else 
              class="markdown-preview"""
content = content.replace(preview_old, preview_new)

# 2. Add click event to toc list
# Find: <li v-for="(head, index) in outline" :key="index" :class="'toc-h' + head.level">
toc_old = """<li v-for="(head, index) in outline" :key="index" :class="'toc-h' + head.level">"""
toc_new = """<li v-for="(head, index) in outline" :key="index" :class="'toc-h' + head.level" @click="scrollToHeading(head, index)">"""
content = content.replace(toc_old, toc_new)

# 3. Handle logic replacement safely
outline_old = """// 大纲提取
const outline = computed(() => {
  if (!activeNote.value) return []
  const lines = activeNote.value.content.split('\\n')
  const result: any[] = []
  for (const line of lines) {
    const match = line.match(/^(#{1,6})\\s+(.+)/)
    if (match) {
      result.push({
        level: match[1].length,
        text: match[2].trim()
      })
    }
  }
  return result
})"""

outline_new = """const editorTextareaRef = ref<HTMLTextAreaElement | null>(null)
const previewContainerRef = ref<HTMLDivElement | null>(null)

// 大纲提取
const outline = computed(() => {
  if (!activeNote.value) return []
  const lines = activeNote.value.content.split('\\n')
  const result: any[] = []
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const match = line.match(/^(#{1,6})\\s+(.+)/)
    if (match) {
      result.push({
        level: match[1].length,
        text: match[2].trim(),
        lineIndex: i
      })
    }
  }
  return result
})

const scrollToHeading = (head: any, index: number) => {
  if (editMode.value === 'read') {
    if (!previewContainerRef.value) return
    const headings = previewContainerRef.value.querySelectorAll('h1, h2, h3, h4, h5, h6')
    const target = headings[index]
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  } else {
    if (!editorTextareaRef.value || !activeNote.value) return
    const ta = editorTextareaRef.value
    
    const style = window.getComputedStyle(ta)
    const lineHeightStr = style.lineHeight
    let lineHeight = 27
    if (lineHeightStr !== 'normal') {
      lineHeight = parseFloat(lineHeightStr)
    } else {
      lineHeight = parseFloat(style.fontSize) * 1.5
    }
    ta.scrollTo({
      top: head.lineIndex * lineHeight,
      behavior: 'smooth'
    })
    
    const lines = activeNote.value.content.split('\\n')
    let charIndex = 0
    for (let i = 0; i < head.lineIndex; i++) {
        charIndex += lines[i].length + 1
    }
    ta.focus()
    ta.setSelectionRange(charIndex, charIndex)
  }
}"""
content = content.replace(outline_old, outline_new)

with open('frontend/src/views/note/NoteEditorView.vue', 'w', encoding='utf-8') as f:
    f.write(content)
