<template>
  <div class="h-screen flex flex-col pt-[60px] bg-gray-50 overflow-hidden w-full relative">
    <div class="max-w-4xl w-full mx-auto p-8">
      <div class="flex items-center gap-4 mb-8">
        <button @click="router.back()" class="text-gray-500 hover:text-gray-700 bg-white shadow-sm border px-3 py-1.5 rounded-lg text-sm transition">
          ← 返回工作台
        </button>
        <h1 class="text-2xl font-bold m-0 text-gray-800">本地库设置</h1>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Storage Information -->
        <div class="bg-white border rounded-xl p-6 shadow-sm">
          <h2 class="font-bold text-lg mb-4 flex items-center gap-2">
            <span>💾</span> 存储信息
          </h2>
          <div class="space-y-3 text-sm text-gray-600">
            <div class="flex justify-between border-b pb-2">
              <span>当前存储方式</span>
              <span class="font-medium text-gray-800">浏览器 LocalStorage</span>
            </div>
            <div class="flex justify-between border-b pb-2">
              <span>本地库容量限制</span>
              <span class="font-medium text-gray-800">约 5MB (视浏览器而定)</span>
            </div>
            <div class="flex justify-between border-b pb-2">
              <span>当前笔记数量</span>
              <span class="font-medium text-gray-800">{{ localNote.notes.length }} 篇</span>
            </div>
            <div class="flex justify-between pb-2">
              <span>当前文件夹数</span>
              <span class="font-medium text-gray-800">{{ localNote.folders.length }} 个</span>
            </div>
          </div>
          
          <div class="mt-6 bg-blue-50 border border-blue-100 p-3 rounded-lg text-blue-800 text-xs">
            <strong>本地备份提醒：</strong><br/>
            因为没有云存储，卸载浏览器或清理缓存会导致笔记全部丢失。请定期使用右侧的"导出"功能备份数据到电脑本地文件夹中。
          </div>
        </div>

        <!-- Preferences -->
        <div class="bg-white border rounded-xl p-6 shadow-sm">
          <h2 class="font-bold text-lg mb-4 flex items-center gap-2">
            <span>⚙️</span> 偏好设置
          </h2>
          <div class="space-y-4">
            <label class="flex justify-between items-center cursor-pointer p-3 border rounded-lg hover:bg-gray-50 transition">
              <div>
                <div class="font-medium text-gray-800 text-sm">自动保存</div>
                <div class="text-xs text-gray-500 mt-0.5">停止打字1秒后自动保存到本地</div>
              </div>
              <input type="checkbox" v-model="localNote.autoSave" @change="localNote.saveToDb()" class="w-4 h-4 cursor-pointer" />
            </label>
          </div>
        </div>

        <!-- Data Management -->
        <div class="bg-white border rounded-xl p-6 shadow-sm md:col-span-2">
          <h2 class="font-bold text-lg mb-4 flex items-center gap-2">
            <span>🔄</span> 数据管理
          </h2>
          
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            
            <!-- Export -->
            <div class="border rounded-lg p-5 text-center flex flex-col items-center hover:border-blue-300 transition">
              <div class="text-3xl mb-2">📤</div>
              <h3 class="font-medium text-sm text-gray-800 mb-1">导出全部笔记</h3>
              <p class="text-xs text-gray-500 mb-4 h-8">将所有笔记和分类打包为 JSON 格式文件下载</p>
              <button @click="handleExport" class="w-full bg-blue-50 text-blue-600 hover:bg-blue-100 font-medium py-2 rounded text-sm transition">
                下载备份 JSON
              </button>
            </div>

            <!-- Import -->
            <div class="border rounded-lg p-5 text-center flex flex-col items-center hover:border-green-300 transition">
              <div class="text-3xl mb-2">📥</div>
              <h3 class="font-medium text-sm text-gray-800 mb-1">导入备份数据</h3>
              <p class="text-xs text-gray-500 mb-4 h-8">从 JSON 文件恢复（将覆盖当前本地库所有内容）</p>
              <label class="w-full bg-green-50 text-green-600 hover:bg-green-100 font-medium py-2 rounded text-sm transition cursor-pointer">
                选择 JSON 文件
                <input type="file" accept=".json" class="hidden" @change="handleImport" />
              </label>
            </div>

            <!-- Clear -->
            <div class="border border-red-200 bg-red-50/30 rounded-lg p-5 text-center flex flex-col items-center">
              <div class="text-3xl mb-2">🗑️</div>
              <h3 class="font-medium text-sm text-red-800 mb-1">清空本地库</h3>
              <p class="text-xs text-red-600/70 mb-4 h-8">删除所有文件夹和笔记记录，此操作不可逆</p>
              <button @click="handleClear" class="w-full border border-red-300 bg-white text-red-600 hover:bg-red-50 font-medium py-2 rounded text-sm transition">
                彻底清空数据
              </button>
            </div>

          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useLocalNoteStore } from '@/stores/localNote'

const router = useRouter()
const localNote = useLocalNoteStore()

const handleExport = () => {
  const data = localNote.exportData()
  const blob = new Blob([data], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `sharehub-notes-${new Date().toISOString().slice(0,10)}.json`
  a.click()
  URL.revokeObjectURL(url)
}

const handleImport = (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  if (confirm('警告：导入备份数据将会覆盖您当前所有本地笔记！确定继续吗？')) {
    const reader = new FileReader()
    reader.onload = (evt) => {
      const content = evt.target?.result as string
      if (localNote.importData(content)) {
        alert('✅ 笔记数据导入成功！')
        router.push('/editor/note')
      } else {
        alert('❌ 数据格式错误或损坏，导入失败。')
      }
    }
    reader.readAsText(file)
  }
}

const handleClear = () => {
  if (confirm('🚨 危险操作 🚨\n确定要清空所有本地笔记数据吗？此操作无法恢复！')) {
    localNote.clearAll()
    alert('本地笔记库已清空。')
    router.push('/editor/note')
  }
}
</script>
