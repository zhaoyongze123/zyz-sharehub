<template>
  <div class="page-shell h-screen flex flex-col pt-[60px] bg-gray-50 overflow-hidden">
    <!-- Header Bar -->
    <header class="h-[60px] bg-white border-b px-6 flex items-center justify-between shrink-0 shadow-sm z-10 w-full">
      <div class="flex items-center gap-4">
        <h1 class="text-xl font-bold m-0">简历工作台</h1>
        <div class="text-sm text-gray-500 mt-1 flex items-center gap-2">
          <span>当前版本:</span>
          <select v-model="currentVersionId" @change="switchVersion" class="border rounded px-2 py-1 text-sm bg-gray-50">
            <option v-for="v in versions" :key="v.id" :value="v.id">{{ v.name }}</option>
          </select>
          <button @click="createNewVersion" class="text-blue-500 hover:text-blue-700 ml-1 text-xs">新建</button>
        </div>
      </div>
      
      <div class="flex items-center gap-4">
        <!-- Save Status -->
        <div class="text-sm flex items-center gap-1 min-w-[80px] justify-end">
          <span v-if="saving" class="text-gray-500 text-xs">保存中...</span>
          <span v-else-if="saveTime" class="text-green-600 text-xs">已保存 {{ saveTime }}</span>
        </div>

        <div class="flex items-center gap-2 text-sm border-r pr-4">
          <span class="text-gray-500">模板:</span>
          <select v-model="currentTemplate" class="border rounded px-2 py-1">
            <option value="classic">经典模板</option>
            <option value="modern" disabled>现代模板(开发中)</option>
          </select>
        </div>

        <BaseButton variant="secondary" @click="resetToDefault" class="text-sm">重置</BaseButton>
        <BaseButton @click="exportPdf" :disabled="exporting" class="text-sm">
          {{ exporting ? '生成中...' : '导出 PDF' }}
        </BaseButton>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 flex overflow-hidden">
      <!-- Column 1: Module Outline & Drag Sorting -->
      <aside class="w-[260px] bg-white border-r flex flex-col h-full shrink-0">
        <div class="p-4 border-b bg-gray-50 font-medium">模块管理</div>
        <div class="p-2 overflow-y-auto flex-1">
          <draggable 
            v-model="modules" 
            item-key="id"
            handle=".drag-handle"
            ghost-class="opacity-50"
            class="space-y-2"
          >
            <template #item="{ element }">
              <div 
                class="flex items-center gap-2 p-2 rounded cursor-pointer transition-colors border"
                :class="activeModuleId === element.id ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-blue-300'"
                @click="activeModuleId = element.id"
              >
                <div class="drag-handle cursor-grab text-gray-400 hover:text-gray-600 px-1">⋮⋮</div>
                <div class="flex-1 text-sm select-none">{{ element.title }}</div>
                <!-- Visibility Toggle -->
                <button 
                  @click.stop="element.visible = !element.visible"
                  class="text-xs p-1 rounded hover:bg-gray-200 text-gray-500"
                  :class="{'text-blue-500': element.visible}"
                  title="显示/隐藏"
                >
                  {{ element.visible ? '👁️' : '🙈' }}
                </button>
              </div>
            </template>
          </draggable>
        </div>
      </aside>

      <!-- Column 2: Module Editor -->
      <section class="w-[360px] bg-white border-r flex flex-col h-full shrink-0">
        <div class="p-4 border-b bg-gray-50 font-medium flex justify-between items-center">
          <span>编辑：{{ activeModuleTitle }}</span>
        </div>
        <div class="p-4 overflow-y-auto flex-1 config-panel" v-if="activeModule">
          
          <!-- Base Info Editor -->
          <div v-if="activeModule.type === 'basic'" class="space-y-4">
            <BaseInput v-model="activeModule.data.name" label="姓名" />
            <BaseInput v-model="activeModule.data.intent" label="求职意向" />
            <div class="grid grid-cols-2 gap-2">
              <BaseInput v-model="activeModule.data.phone" label="手机号码" />
              <BaseInput v-model="activeModule.data.email" label="邮箱" />
              <BaseInput v-model="activeModule.data.age" label="年龄" />
              <BaseInput v-model="activeModule.data.gender" label="性别" />
              <BaseInput v-model="activeModule.data.city" label="求职城市" />
              <BaseInput v-model="activeModule.data.currentCity" label="当前城市" />
              <BaseInput v-model="activeModule.data.experience" label="工作经验" />
              <BaseInput v-model="activeModule.data.salary" label="期望薪资" />
            </div>
            <BaseInput v-model="activeModule.data.availability" label="到岗时间" />
            <div class="text-xs text-gray-500 pt-2 border-t mt-4">提示: 基础信息通常固定在顶部显示。</div>
          </div>

          <!-- Education Editor -->
          <div v-if="activeModule.type === 'education'" class="space-y-4">
            <div v-for="(item, idx) in activeModule.data.items" :key="idx" class="p-3 border rounded relative bg-gray-50">
              <button @click="removeItem(activeModule.data.items, idx)" class="absolute top-2 right-2 text-red-500 text-xs">删除</button>
              <BaseInput v-model="item.school" label="学校名称" class="mb-2" />
              <div class="grid grid-cols-2 gap-2 mb-2">
                <BaseInput v-model="item.major" label="专业" />
                <BaseInput v-model="item.degree" label="学历" />
              </div>
              <BaseInput v-model="item.time" label="时间 (如: 2018.09 - 2022.06)" class="mb-2" />
              <BaseInput v-model="item.gpa" label="专业成绩/GPA" class="mb-2" />
              <div>
                <label class="block text-sm font-medium mb-1 text-gray-700">主修课程</label>
                <textarea v-model="item.courses" class="w-full border rounded p-2 text-sm" rows="3"></textarea>
              </div>
            </div>
            <BaseButton variant="secondary" @click="addEdu" class="w-full text-sm">+ 添加教育经历</BaseButton>
          </div>

          <!-- Experience/Project Editor -->
          <div v-if="activeModule.type === 'experience' || activeModule.type === 'project'" class="space-y-4">
            <div v-for="(item, idx) in activeModule.data.items" :key="idx" class="p-3 border rounded relative bg-gray-50">
              <button @click="removeItem(activeModule.data.items, idx)" class="absolute top-2 right-2 text-red-500 text-xs">删除</button>
              <BaseInput v-model="item.company" :label="activeModule.type === 'experience' ? '公司名称' : '项目名称'" class="mb-2" />
              <BaseInput v-model="item.position" :label="activeModule.type === 'experience' ? '职位' : '角色'" class="mb-2" />
              <BaseInput v-model="item.time" label="时间" class="mb-2" />
              <div>
                <label class="block text-sm font-medium mb-1 text-gray-700">描述 (每行一段)</label>
                <textarea 
                  :value="item.descriptions.join('\n')" 
                  @input="e => updateDescs(item, e)" 
                  class="w-full border rounded p-2 text-sm leading-relaxed" 
                  rows="5"
                ></textarea>
              </div>
            </div>
            <BaseButton variant="secondary" @click="addExp(activeModule.type)" class="w-full text-sm">+ 添加{{ activeModule.type === 'experience' ? '工作' : '项目' }}经历</BaseButton>
          </div>

          <!-- Skills Editor -->
          <div v-if="activeModule.type === 'skills'" class="space-y-4">
            <div>
              <label class="block text-sm font-medium mb-1 text-gray-700">语言能力</label>
              <textarea v-model="activeModule.data.languageText" class="w-full border rounded p-2 text-sm" rows="2"></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1 text-gray-700">计算机能力</label>
              <textarea v-model="activeModule.data.computerText" class="w-full border rounded p-2 text-sm" rows="2"></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1 text-gray-700">团队能力</label>
              <textarea v-model="activeModule.data.teamText" class="w-full border rounded p-2 text-sm" rows="2"></textarea>
            </div>
            
            <div class="border-t pt-4 mt-4">
              <label class="block text-sm font-medium mb-2 text-gray-700">技能进度条设置</label>
              <div v-for="(bar, idx) in activeModule.data.bars" :key="idx" class="flex gap-2 items-center mb-2">
                <input v-model="bar.name" placeholder="技能名称" class="border rounded px-2 py-1 text-sm w-20" />
                <input v-model="bar.level" placeholder="等级评语" class="border rounded px-2 py-1 text-sm w-20" />
                <input v-model="bar.percent" type="range" min="1" max="100" class="flex-1" />
                <span class="text-xs w-8">{{ bar.percent }}%</span>
                <button @click="removeItem(activeModule.data.bars, idx)" class="text-red-500 pb-1">×</button>
              </div>
              <BaseButton variant="secondary" @click="addSkillBar" class="w-full text-xs mt-2">+ 添加进度条</BaseButton>
            </div>
          </div>

          <!-- Text List Editor (Honors / Evaluation) -->
          <div v-if="activeModule.type === 'textList'" class="space-y-4">
            <div>
              <label class="block text-sm font-medium mb-1 text-gray-700">内容 (每行一段)</label>
              <textarea 
                :value="activeModule.data.items.join('\n')" 
                @input="e => updateTextList(e)" 
                class="w-full border rounded p-2 text-sm leading-relaxed" 
                rows="8"
              ></textarea>
            </div>
          </div>
          
          <!-- Text Paragraph Editor -->
          <div v-if="activeModule.type === 'text'" class="space-y-4">
            <div>
              <label class="block text-sm font-medium mb-1 text-gray-700">正文内容</label>
              <textarea 
                v-model="activeModule.data.content" 
                class="w-full border rounded p-2 text-sm leading-relaxed whitespace-pre-wrap" 
                rows="8"
              ></textarea>
            </div>
          </div>

        </div>
        <div v-else class="p-4 text-center text-gray-400 mt-10">
          请选择左侧模块进行编辑
        </div>
      </section>

      <!-- Column 3: Real-time PDF Preview -->
      <section class="flex-1 bg-gray-200 overflow-y-auto flex justify-center p-8">
        <ResumeClassicTemplate :modules="modules" :template="currentTemplate" ref="resumeTemplateRef" />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import draggable from 'vuedraggable'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import ResumeClassicTemplate from '@/components/business/ResumeClassicTemplate.vue'
import html2pdf from 'html2pdf.js'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

// === State Management ===
const resumeTemplateRef = ref<any>(null)
const exporting = ref(false)
const saving = ref(false)
const saveTime = ref('')
const currentTemplate = ref('classic')

const defaultModules = [
  {
    id: 'basic', type: 'basic', title: '基础信息', visible: true,
    data: {
      name: '全民简历', intent: '行政专员', city: '上海', salary: '6000/月', availability: '一个月内到岗',
      age: '33', gender: '男', currentCity: '上海', experience: '7年经验', phone: '15688888886', email: 'qmjianli@qq.com',
      avatar: ''
    }
  },
  {
    id: 'education', type: 'education', title: '教育背景', visible: true,
    data: {
      items: [
        {
          school: '全民简历师范大学', major: '工商管理', degree: '本科', time: '2012-09 ~ 2016-07',
          gpa: 'GPA 3.66/4 (专业前5%)',
          courses: '基础会计学、货币银行学、统计学、经济法概论、财务会计学、管理学原理、组织行为学、市场营销学、国际贸易理论、国际贸易实务、人力资源开发与管理、财务管理学、企业经营战略概论、质量管理学、西方经济学等等。'
        }
      ]
    }
  },
  {
    id: 'experience', type: 'experience', title: '工作经历', visible: true,
    data: {
      items: [
        {
          company: '全民简历科技有限公司', position: '行政专员', time: '2018-09 ~ 至今',
          descriptions: [
            '负责本部的行政人事管理和日常事务，协助总经办搞好各部门之间的综合协调，落实公司规章制度，沟通内外联系，保证上情下达和下情上报。',
            '编制公司人事管理制度，规避各项人事风险。'
          ]
        },
        {
          company: '上海斧掌网络科技有限公司', position: '行政专员', time: '2016-09 ~ 2018-08',
          descriptions: [
            '负责中心简单财务管理，资产管控；',
            '负责公司总部的来访客户接待工作，负责引导和介绍公司的分布情况；'
          ]
        }
      ]
    }
  },
  {
    id: 'project', type: 'project', title: '项目经历', visible: false,
    data: {
      items: [
        {
          company: '开源微服务网关平台', position: '核心开发者', time: '2021-01 ~ 2021-12',
          descriptions: [
            '主导基于 Spring Cloud Gateway 的微服务架构设计。',
            '实现高并发场景下的限流降级，系统吞吐量提升 300%。'
          ]
        }
      ]
    }
  },
  {
    id: 'skills', type: 'skills', title: '技能特长', visible: true,
    data: {
      languageText: '大学英语6级证书，荣获全国大学生英语竞赛一等奖，能够熟练进行交流、读写。',
      computerText: '计算机二级证书，熟练操作windows平台上的各类应用软件，如Word、Excel。',
      teamText: '具有丰富的团队组建与扩充经验和项目管理与协调经验。',
      bars: [
        { name: '计算机', level: '精通', percent: 90 },
        { name: '英语', level: '良好', percent: 70 }
      ]
    }
  },
  {
    id: 'honors', type: 'textList', title: '荣誉证书', visible: true,
    data: {
      items: [
        '英语四级，听说读写能力良好，能流利用英语进行日常交流；',
        '通过全国计算机二级考试，熟练运用office等常用的办公软件。'
      ]
    }
  },
  {
    id: 'evaluation', type: 'text', title: '自我评价', visible: true,
    data: {
      content: '（此处可填写自我评价内容）具备较强的责任心和抗压能力，乐于接受挑战。执行能力强，能够快速适应新环境，团队协作意识良好。'
    }
  }
]

const versions = ref<{id: string, name: string, data: any}[]>([])
const currentVersionId = ref('v1')
const modules = ref<any>(JSON.parse(JSON.stringify(defaultModules)))

const activeModuleId = ref('basic')
const activeModule = computed(() => modules.value.find((m: any) => m.id === activeModuleId.value))
const activeModuleTitle = computed(() => activeModule.value?.title || '')

// === Helper Methods ===
const removeItem = (arr: any[], idx: number) => arr.splice(idx, 1)

const updateDescs = (item: any, e: any) => {
  item.descriptions = e.target.value.split('\n').filter((l: string) => l.trim())
}
const updateTextList = (e: any) => {
  if (activeModule.value) {
    activeModule.value.data.items = e.target.value.split('\n').filter((l: string) => l.trim())
  }
}
const addEdu = () => activeModule.value.data.items.push({ school: '', major: '', degree: '本科', time: '', courses: '', gpa: '' })
const addExp = (type: string) => activeModule.value.data.items.push({ company: '', position: '', time: '', descriptions: [''] })
const addSkillBar = () => activeModule.value.data.bars.push({ name: '新技能', level: '一般', percent: 50 })

// === Persist & Multi-version Logic ===
let saveTimer: any = null
const autoSave = () => {
  saving.value = true
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    // Save to current version
    const idx = versions.value.findIndex(v => v.id === currentVersionId.value)
    if (idx !== -1) {
      versions.value[idx].data = JSON.parse(JSON.stringify(modules.value))
      localStorage.setItem('resume_versions', JSON.stringify(versions.value))
      
      saving.value = false
      const d = new Date()
      saveTime.value = `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}:${d.getSeconds().toString().padStart(2,'0')}`
    }
  }, 1000) // Debounce 1s
}

watch(modules, () => {
  autoSave()
}, { deep: true })

onMounted(() => {
  const saved = localStorage.getItem('resume_versions')
  if (saved) {
    versions.value = JSON.parse(saved)
    currentVersionId.value = versions.value[0].id
    modules.value = JSON.parse(JSON.stringify(versions.value[0].data))
  } else {
    versions.value = [{ id: 'v1', name: '默认简历', data: JSON.parse(JSON.stringify(defaultModules)) }]
    localStorage.setItem('resume_versions', JSON.stringify(versions.value))
  }
})

const switchVersion = () => {
  const target = versions.value.find(v => v.id === currentVersionId.value)
  if (target) modules.value = JSON.parse(JSON.stringify(target.data))
}

const createNewVersion = () => {
  const id = 'v' + Date.now()
  const newVer = { id, name: `复制自 ${versions.value.find(v => v.id === currentVersionId.value)?.name}`, data: JSON.parse(JSON.stringify(modules.value)) }
  versions.value.push(newVer)
  currentVersionId.value = id
  localStorage.setItem('resume_versions', JSON.stringify(versions.value))
}

const resetToDefault = () => {
  if(confirm('确认将当前版本重置为默认模板数据吗？')) {
    modules.value = JSON.parse(JSON.stringify(defaultModules))
  }
}

// === Export PDF ===
async function exportPdf() {
  if (!resumeTemplateRef.value?.resumeRef) return

  exporting.value = true
  appStore.showToast('导出任务开始', '正在生成高质量 PDF...')

  const element = resumeTemplateRef.value.resumeRef
  var opt = {
    margin:       0,
    filename:     `${modules.value.find((m:any) => m.id === 'basic')?.data.name || '我的'}-简历.pdf`,
    image:        { type: 'jpeg', quality: 0.98 },
    html2canvas:  { scale: 2, useCORS: true },
    jsPDF:        { unit: 'px', format: 'a4', orientation: 'portrait', hotfixes: ["px_scaling"] }
  };

  try {
    await html2pdf().set(opt).from(element).save()
    appStore.showToast('导出完成', '已成功下载 PDF 文件')
  } catch (error: any) {
    appStore.showToast('导出失败', error.message || '生成 PDF 时发生错误', 'error')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped lang="scss">
textarea {
  resize: vertical;
}

.config-panel {
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background: #d1d5db;
    border-radius: 4px;
  }
}

[type=range] {
  -webkit-appearance: none;
  background: transparent;
}
[type=range]::-webkit-slider-thumb {
  -webkit-appearance: none;
  height: 12px;
  width: 12px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  margin-top: -4px;
}
[type=range]::-webkit-slider-runnable-track {
  width: 100%;
  height: 4px;
  cursor: pointer;
  background: #e5e7eb;
  border-radius: 2px;
}
</style>
