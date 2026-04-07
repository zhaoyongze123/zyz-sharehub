<template>
  <div class="page-shell h-screen flex flex-col  bg-gray-100 overflow-hidden font-sans">
    <!-- Header Bar -->
    <header class="h-[60px] bg-white border-b px-6 flex items-center justify-between shrink-0 shadow-sm z-10 w-full">
      <div class="flex items-center gap-4">
        <h1 class="text-xl font-bold m-0 text-gray-800">简历工作台</h1>
        <div class="text-sm text-gray-500 mt-1 flex items-center gap-2">
          <span>版本:</span>
          <select v-model="currentVersionId" @change="switchVersion" class="border-none bg-gray-50 rounded py-1 px-2 font-medium cursor-pointer shadow-sm">
            <option v-for="v in versions" :key="v.id" :value="v.id">{{ v.name }}</option>
          </select>
          <button @click="createNewVersion" class="text-blue-600 hover:text-blue-800 ml-1 font-medium bg-blue-50 px-2 py-1 rounded">新建</button>
        </div>
      </div>
      
      <div class="flex items-center gap-4">
        <div class="text-sm flex items-center gap-1 min-w-[80px] justify-end mr-4">
          <span v-if="saving" class="text-gray-400 text-xs">保存中...</span>
          <span v-else-if="saveTime" class="text-green-500 text-xs">已自动保存 {{ saveTime }}</span>
        </div>
        <div class="flex items-center gap-2 text-sm border-r pr-4">
          <span class="text-gray-500">模板:</span>
          <select v-model="currentTemplate" class="border border-gray-200 rounded px-2 py-1 bg-white cursor-pointer shadow-sm">
            <option value="classic">经典单栏</option>
          </select>
        </div>
        <BaseButton variant="secondary" @click="resetToDefault" class="text-sm">重置</BaseButton>
        <BaseButton @click="exportPdf" :disabled="exporting" class="text-sm shadow-md bg-gray-800 hover:bg-gray-900 border-none text-white">
          {{ exporting ? '生成中...' : '导出 PDF' }}
        </BaseButton>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 flex overflow-hidden relative">
      <!-- Column 1: Module Outline -->
      <aside :class="['bg-white border-r flex flex-col h-full shrink-0 transition-all duration-300 z-20 shadow-lg', outlineCollapsed ? 'w-[40px]' : 'w-[240px]']">
        <div class="p-3 border-b bg-gray-50 flex justify-between items-center h-[52px]">
          <span v-if="!outlineCollapsed" class="font-bold text-gray-700 text-[14px]">配置模块</span>
          <button @click="outlineCollapsed = !outlineCollapsed" class="text-gray-500 hover:text-gray-800 p-1 mx-auto rounded bg-white border shadow-sm flex items-center justify-center w-[24px] h-[24px]" title="展开/收起">
            <i :class="outlineCollapsed ? 'i-carbon-chevron-right' : 'i-carbon-chevron-left'">&gt;</i>
          </button>
        </div>
        <div class="p-3 overflow-y-auto flex-1" v-show="!outlineCollapsed">
          <draggable 
            v-model="modules" 
            item-key="id"
            handle=".drag-handle"
            ghost-class="opacity-50"
            class="space-y-3"
          >
            <template #item="{ element }">
              <div 
                class="flex items-center gap-2 p-2.5 rounded-lg cursor-pointer transition-all border group shadow-sm bg-white"
                :class="activeModuleId === element.id ? 'border-blue-500 ring-1 ring-blue-200' : 'border-gray-200 hover:border-gray-300 hover:shadow-md'"
                @click="switchActive(element.id)"
              >
                <div class="drag-handle cursor-grab text-gray-300 hover:text-gray-500 px-1"><i class="i-carbon-draggable">≡</i></div>
                <div class="flex-1 text-[13px] font-medium text-gray-700 select-none truncate">{{ element.title }}</div>
                <button 
                  @click.stop="element.visible = !element.visible"
                  class="text-xs p-1.5 rounded-md transition-colors"
                  :class="element.visible ? 'text-blue-600 bg-blue-50 hover:bg-blue-100' : 'text-gray-400 bg-gray-100 hover:bg-gray-200'"
                  title="模块显隐"
                >
                  <i :class="element.visible ? 'i-carbon-view' : 'i-carbon-view-off'">{{ element.visible ? '👁️' : '🙈' }}</i>
                </button>
              </div>
            </template>
          </draggable>
        </div>
      </aside>

      <!-- Column 2: Module Editor -->
      <section class="bg-white border-r flex flex-col h-full shrink-0 shadow-[4px_0_24px_rgba(0,0,0,0.04)] z-10 transition-all duration-300 relative" :class="outlineCollapsed ? 'w-[500px]' : 'w-[420px]'">
        <div class="p-4 border-b bg-gray-50/80 flex justify-between items-center h-[52px] shrink-0 border-l relative">
          <!-- Small tab indicating active panel -->
          <div class="absolute -left-[1px] top-0 bottom-0 w-[3px] bg-blue-500"></div>
          <span class="font-bold text-gray-700 text-[14px]">编辑：<span class="text-blue-600">{{ activeModuleTitle }}</span></span>
        </div>
        <div class="p-6 overflow-y-auto flex-1 config-panel bg-white/50 relative" v-if="activeModule">
          
          <!-- Base Info Editor -->
          <div v-if="activeModule.type === 'basic'" class="space-y-6">
            <div class="border border-blue-100 p-4 rounded-xl bg-blue-50/30">
              <label class="block text-[13px] font-bold mb-3 text-gray-700">头像照片</label>
              <div class="flex items-center gap-4">
                <div class="w-16 h-16 bg-white border shadow-sm rounded-lg flex items-center justify-center overflow-hidden relative shrink-0">
                  <img v-if="activeModule.data.avatar" :src="activeModule.data.avatar" class="w-full h-full object-cover"/>
                  <i v-else class="text-gray-300 text-2xl">👤</i>
                </div>
                <div class="flex-1 space-y-2">
                  <div class="flex gap-2">
                    <button @click="triggerAvatarUpload" class="text-[12px] font-semibold bg-blue-50 text-blue-700 px-3 py-1.5 rounded hover:bg-blue-100 border border-blue-100 transition-colors shadow-sm">上传新头像</button>
                    <input type="file" accept="image/*" @change="uploadAvatar" class="hidden" id="avatarFileInput" />
                    
                    <button v-if="activeModule.data.avatar" @click="activeModule.data.avatar = ''" class="text-red-500 text-[12px] bg-white border border-red-100 hover:bg-red-50 px-3 py-1.5 rounded transition-colors shadow-sm">移除</button>
                  </div>
                  <div class="flex gap-2 mt-1">
                    <button @click="activeModule.data.avatarVisible = !(activeModule.data.avatarVisible !== false)" :class="(activeModule.data.avatarVisible !== false) ? 'text-blue-600 bg-white border-blue-200' : 'text-gray-500 bg-gray-50 border-gray-200'" class="text-[12px] px-3 py-1.5 rounded flex items-center gap-1 border shadow-sm transition-colors w-full justify-center">
                      {{ (activeModule.data.avatarVisible !== false) ? '👁️ 当前印刷可见' : '🙈 当前简历中隐藏' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div>
              <div class="text-[13px] font-bold text-gray-800 pb-2 border-b mb-4 flex items-center"><span class="w-1 h-3 bg-gray-800 mr-2 rounded-full"></span> 细节信息</div>
              <div class="grid grid-cols-2 gap-4">
                <div v-for="key in ['name', 'intent', 'phone', 'email', 'age', 'gender', 'city', 'currentCity', 'experience', 'salary', 'availability']" :key="key" class="flex flex-col gap-1 relative group bg-gray-50/50 p-2.5 rounded-lg border border-transparent hover:border-gray-200 transition-colors">
                  <div class="flex justify-between items-center px-0.5">
                     <label class="text-[12px] font-bold text-gray-600 shrink-0">{{ getLabel(key) }}</label>
                     <button @click="activeModule.data[`${key}Visible`] = !(activeModule.data[`${key}Visible`] !== false)" 
                             class="h-6 w-6 flex items-center justify-center rounded border transition-colors shadow-sm bg-white hover:bg-gray-50"
                             :class="activeModule.data[`${key}Visible`] !== false ? 'border-blue-200 text-blue-500' : 'border-gray-200 text-gray-400'"
                             title="开启/隐藏">
                       {{ activeModule.data[`${key}Visible`] !== false ? '👁️' : '🙈' }}
                     </button>
                  </div>
                  <input :id="'input_basic_' + key" v-model="activeModule.data[key]" class="w-full border-gray-300 rounded px-3 py-1.5 text-[13px] bg-white focus:ring-1 focus:ring-blue-500 border outline-none transition-shadow" />
                </div>
              </div>
            </div>
          </div>

          <!-- Education Editor -->
          <div v-if="activeModule.type === 'education'" class="space-y-6">
            <div v-for="(item, idx) in activeModule.data.items" :key="idx" class="p-5 border rounded-xl relative bg-white shadow-sm hover:shadow-md transition-shadow">
              <div class="absolute top-3 right-3 flex gap-2">
                 <button @click="item.visible = !(item.visible !== false)" :class="(item.visible !== false) ? 'text-blue-600 bg-blue-50 border-blue-200' : 'text-gray-500 bg-gray-100 border-gray-200'" class="text-[12px] px-2 py-1 rounded border">
                   {{ (item.visible !== false) ? '👁️ 此条显示' : '🙈 此条隐藏' }}
                 </button>
                 <button @click="removeItem(activeModule.data.items, idx)" class="text-red-600 text-[12px] px-2 py-1 rounded border border-red-100 bg-red-50 hover:bg-red-100">删除</button>
              </div>
              <div class="text-xs font-bold text-gray-400 mb-4 uppercase tracking-wider">Education {{ idx + 1 }}</div>
              
              <div class="grid grid-cols-2 gap-4">
                <div v-for="key in ['school', 'major', 'degree', 'time', 'gpa']" :key="key" class="flex flex-col gap-1 col-span-1" :class="key === 'school' ? 'col-span-2' : ''">
                  <div class="flex justify-between items-center text-xs">
                    <span class="font-medium text-gray-700">{{ getLabel(key) }}</span>
                    <button @click="item[key + 'Visible'] = !(item[key + 'Visible'] !== false)" class="text-xs text-gray-400 hover:text-blue-500 bg-gray-50 border rounded w-[22px] h-[22px]" :class="item[key + 'Visible'] !== false ? 'text-blue-500 border-blue-200' : ''">{{ item[key + 'Visible'] !== false ? '👁️' : '🙈' }}</button>
                  </div>
                  <input :id="`input_edu_${idx}_${key}`" v-model="item[key]" class="w-full border rounded px-3 py-1.5 text-sm bg-gray-50 focus:bg-white focus:ring-1 outline-none transition-shadow" />
                </div>
                <div class="col-span-2 flex flex-col gap-1 mt-2">
                  <div class="flex justify-between items-center text-xs">
                    <span class="font-medium text-gray-700">主修课程</span>
                    <button @click="item.coursesVisible = !(item.coursesVisible !== false)" class="text-xs text-gray-400 hover:text-blue-500 bg-gray-50 border rounded w-[22px] h-[22px]" :class="item.coursesVisible !== false ? 'text-blue-500 border-blue-200' : ''">{{ item.coursesVisible !== false ? '👁️' : '🙈' }}</button>
                  </div>
                  <textarea :id="`input_edu_${idx}_courses`" v-model="item.courses" class="w-full border rounded p-3 text-[13px] bg-gray-50 focus:bg-white focus:ring-1 outline-none leading-relaxed transition-shadow" rows="3"></textarea>
                </div>
              </div>
            </div>
            <BaseButton variant="secondary" @click="addEdu" class="w-full text-sm py-2.5 border-dashed border-2 hover:bg-gray-50">+ 添加教育经历</BaseButton>
          </div>

          <!-- Experience/Project Editor -->
          <div v-if="activeModule.type === 'experience' || activeModule.type === 'project'" class="space-y-6">
            <div v-for="(item, idx) in activeModule.data.items" :key="idx" class="p-5 border rounded-xl relative bg-white shadow-sm hover:shadow-md transition-shadow">
              <div class="absolute top-3 right-3 flex gap-2">
                 <button @click="item.visible = !(item.visible !== false)" :class="(item.visible !== false) ? 'text-blue-600 bg-blue-50 border-blue-200' : 'text-gray-500 bg-gray-100 border-gray-200'" class="text-[12px] px-2 py-1 rounded border">
                   {{ (item.visible !== false) ? '👁️ 显示' : '🙈 隐藏' }}
                 </button>
                 <button @click="removeItem(activeModule.data.items, idx)" class="text-red-600 text-[12px] px-2 py-1 rounded border border-red-100 bg-red-50 hover:bg-red-100">删除</button>
              </div>
              <div class="text-xs font-bold text-gray-400 mb-4 uppercase tracking-wider">{{ activeModule.type === 'experience' ? 'Experience' : 'Project' }} {{ idx + 1 }}</div>
              
              <div class="grid grid-cols-2 gap-4">
                <div v-for="key in ['company', 'position', 'time']" :key="key" class="flex flex-col gap-1 col-span-1" :class="key === 'company' ? 'col-span-2' : ''">
                  <div class="flex justify-between items-center text-xs">
                    <span class="font-medium text-gray-700">{{ activeModule.type === 'experience' ? getLabel(key) : getLabel(key.replace('company','project').replace('position','role')) }}</span>
                    <button @click="item[key + 'Visible'] = !(item[key + 'Visible'] !== false)" class="text-xs text-gray-400 hover:text-blue-500 bg-gray-50 border rounded w-[22px] h-[22px]" :class="item[key + 'Visible'] !== false ? 'text-blue-500 border-blue-200' : ''">{{ item[key + 'Visible'] !== false ? '👁️' : '🙈' }}</button>
                  </div>
                  <input :id="`input_exp_${idx}_${key}`" v-model="item[key]" class="w-full border rounded px-3 py-1.5 text-sm bg-gray-50 focus:bg-white focus:ring-1 outline-none transition-shadow" />
                </div>
                
                <div class="col-span-2 flex flex-col gap-1 mt-2">
                  <div class="flex justify-between items-center text-xs mb-1">
                    <span class="font-medium text-gray-700">经历描述细节 (每行一段)</span>
                  </div>
                  <div v-for="(desc, dIdx) in item.descriptions" :key="dIdx" class="flex gap-2 items-start group">
                    <button @click="desc.visible = !(desc.visible !== false)" class="mt-1 flex-shrink-0 text-xs text-gray-400 hover:text-blue-500 bg-white border rounded w-[24px] h-[24px] shadow-sm transform transition-transform active:scale-95" :class="desc.visible !== false ? 'text-blue-500 border-blue-200' : ''">{{ desc.visible !== false ? '👁️' : '🙈' }}</button>
                    <textarea :id="`input_exp_${idx}_desc_${dIdx}`" v-model="desc.text" class="flex-1 w-full border rounded p-2 text-[13px] bg-gray-50 focus:bg-white focus:ring-1 outline-none leading-relaxed transition-shadow min-h-[40px] resize-none" rows="2"></textarea>
                    <button @click="item.descriptions.splice(dIdx, 1)" class="mt-1 text-gray-300 hover:text-red-500 w-[24px] h-[24px] flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity text-xl focus:opacity-100">×</button>
                  </div>
                  <button @click="item.descriptions.push({ text: '', visible: true })" class="text-[12px] bg-gray-50 hover:bg-gray-100 text-gray-600 rounded py-1.5 mt-1 border border-transparent shadow-sm inline-block self-start px-3 transition-colors">+ 新增一条描述</button>
                </div>
              </div>
            </div>
            <BaseButton variant="secondary" @click="addExp(activeModule.type)" class="w-full text-sm py-2.5 border-dashed border-2 hover:bg-gray-50">+ 添加{{ activeModule.type === 'experience' ? '工作经历' : '项目经验' }}</BaseButton>
          </div>

          <!-- Skills Editor -->
          <div v-if="activeModule.type === 'skills'" class="space-y-6">
            <div class="space-y-4">
              <div v-for="key in ['languageText', 'computerText', 'teamText']" :key="key" class="bg-gray-50 p-4 rounded-xl border relative shadow-sm hover:shadow-md transition-shadow">
                <div class="flex justify-between mb-2 items-center">
                  <label class="block text-[13px] font-bold text-gray-700">{{ getLabel(key) }}</label>
                  <button @click="activeModule.data[`${key}Visible`] = !(activeModule.data[`${key}Visible`] !== false)" 
                          class="h-7 w-7 flex items-center justify-center rounded border transition-colors shadow-sm bg-white"
                          :class="activeModule.data[`${key}Visible`] !== false ? 'border-blue-200 text-blue-500' : 'border-gray-200 text-gray-400'">
                    {{ activeModule.data[`${key}Visible`] !== false ? '👁️' : '🙈' }}
                  </button>
                </div>
                <textarea :id="`input_skills_${key}`" v-model="activeModule.data[key]" class="w-full border rounded p-3 text-[13px] outline-none focus:ring-1 focus:bg-white transition-colors" rows="3"></textarea>
              </div>
            </div>
            
            <div class="border-t pt-6 mt-4">
              <label class="block text-[14px] font-bold mb-4 text-gray-800 flex items-center"><span class="w-1 h-3 bg-gray-800 mr-2 rounded-full"></span> 可视化技能进度条</label>
              <div v-for="(bar, idx) in activeModule.data.bars" :key="idx" class="flex gap-3 mb-3 p-3 border rounded-xl bg-white shadow-sm w-full relative">
                <button @click="bar.visible = !(bar.visible !== false)" 
                        class="h-8 w-8 mt-1 flex-shrink-0 flex items-center justify-center rounded border transition-colors bg-gray-50"
                        :class="bar.visible !== false ? 'border-blue-200 text-blue-500' : 'border-gray-200 text-gray-400'">
                  {{ bar.visible !== false ? '👁️' : '🙈' }}
                </button>
                <div class="flex-1 min-w-0 space-y-2">
                  <div class="flex gap-2">
                    <input :id="`input_skills_bar_${idx}_name`" v-model="bar.name" placeholder="技能名称" class="border rounded px-2.5 py-1 text-[13px] flex-1 min-w-0 bg-gray-50 focus:bg-white transition-shadow" />
                    <input v-model="bar.level" placeholder="精通" class="border rounded px-2.5 py-1 text-[13px] flex-1 min-w-0 bg-gray-50 focus:bg-white transition-shadow" />
                  </div>
                  <div class="flex items-center gap-3">
                     <span class="text-xs font-bold text-gray-600 w-[30px] text-right">{{ bar.percent }}%</span>
                     <input v-model="bar.percent" type="range" min="1" max="100" class="flex-1 custom-range" />
                     <button @click="removeItem(activeModule.data.bars, idx)" class="text-red-400 hover:text-red-600 w-8 flex justify-center text-xl pb-1.5 focus:outline-none">×</button>
                  </div>
                </div>
              </div>
              <BaseButton variant="secondary" @click="addSkillBar" class="w-full text-[13px] mt-2 py-2 border-dashed border-2 hover:bg-gray-50 text-gray-600">+ 增加进度条项</BaseButton>
            </div>
          </div>

          <!-- Text List Editor -->
          <div v-if="activeModule.type === 'textList'" class="space-y-4">
            <label class="block text-[14px] font-bold mb-2 text-gray-800 flex items-center"><span class="w-1 h-3 bg-gray-800 mr-2 rounded-full"></span> 条目文本列表</label>
            <div v-for="(txt, idx) in activeModule.data.items" :key="idx" class="flex gap-2 mb-3 items-start bg-white p-3 rounded-lg border shadow-sm relative group">
               <button @click="txt.visible = !(txt.visible !== false)" 
                       class="mt-1 h-7 w-7 flex-shrink-0 flex items-center justify-center rounded border transition-colors bg-gray-50"
                       :class="txt.visible !== false ? 'border-blue-200 text-blue-500' : 'border-gray-200 text-gray-400'">
                 {{ txt.visible !== false ? '👁️' : '🙈' }}
               </button>
               <textarea :id="`input_textlist_${idx}`" v-model="txt.text" class="flex-1 border rounded p-2 text-[13px] leading-relaxed bg-gray-50 focus:bg-white outline-none transition-shadow min-h-[44px]" rows="2"></textarea>
               <button @click="removeItem(activeModule.data.items, idx)" class="text-gray-300 hover:text-red-500 bg-white border border-transparent hover:border-red-200 shadow-sm h-7 w-7 flex items-center justify-center rounded absolute -right-2 -top-2 opacity-0 group-hover:opacity-100 transition-opacity z-10 hidden sm:flex focus:opacity-100">×</button>
            </div>
            <BaseButton variant="secondary" @click="addTextListItem" class="w-full text-[13px] py-2 border-dashed border-2 hover:bg-gray-50">+ 增加一行</BaseButton>
          </div>
          
          <!-- Text Paragraph Editor -->
          <div v-if="activeModule.type === 'text'" class="space-y-4">
            <label class="block text-[14px] font-bold mb-2 text-gray-800 flex items-center"><span class="w-1 h-3 bg-gray-800 mr-2 rounded-full"></span> 自由段落排版</label>
            <textarea 
              :id="`input_text_content`"
              v-model="activeModule.data.content" 
              class="w-full border shadow-inner rounded-xl p-4 text-[13px] leading-relaxed whitespace-pre-wrap outline-none bg-gray-50 focus:bg-white transition-shadow" 
              rows="12"
            ></textarea>
          </div>

        </div>
        <div v-else class="p-8 text-center text-gray-400 mt-20 flex flex-col justify-center items-center">
          <div class="text-6xl mb-6 opacity-20 filter grayscale">📄</div>
          <p class="font-medium">请在左侧点击或拖拽模块进行属性配置</p>
          <p class="text-[12px] mt-2 opacity-80">更改将会实时呈现到右侧</p>
        </div>
      </section>

      <!-- Column 3: Real-time PDF Preview -->
      <section class="flex-1 bg-[#cfd1d6] overflow-auto flex flex-col p-8 relative isolate" id="preview-container">
        <!-- Floating hint -->
        <div class="bg-gray-800 text-white px-4 py-1.5 rounded-full shadow-lg text-[13px] font-medium mb-6 sticky top-4 z-40 opacity-90 hover:opacity-100 transition-all flex items-center gap-2 cursor-pointer shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
          <span class="w-2 h-2 rounded-full bg-blue-400 animate-pulse"></span>
          红线代表一页 A4 纸的高度，内容超出时 PDF 将自动分页
        </div>

        <!-- Replaced multiple layers with a simple, robust, natively growing wrapper container -->
        <!-- The component itself now handles the white background & pagination line repeating -->
        <div class="relative w-[794px] min-w-[794px] shrink-0 mx-auto z-10 shadow-2xl rounded-sm overflow-hidden">
             <!-- Note: passing focus mechanism via events -->
             <ResumeClassicTemplate :modules="modules" ref="resumeTemplateRef" @focus-field="handleFocusField" />
        </div>
        
        <div class="h-20 w-full shrink-0"></div> <!-- extra bottom padding -->
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import draggable from 'vuedraggable'
import BaseButton from '@/components/base/BaseButton.vue'
import ResumeClassicTemplate from '@/components/business/ResumeClassicTemplate.vue'
import html2pdf from 'html2pdf.js'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

// State 
const outlineCollapsed = ref(false)
const resumeTemplateRef = ref<any>(null)
const exporting = ref(false)
const saving = ref(false)
const saveTime = ref('')
const currentTemplate = ref('classic')

const defaultModules = [
  {
    id: 'basic', type: 'basic', title: '基础信息', visible: true,
    data: {
      name: '全民简历', nameVisible: true,
      intent: '行政专员', intentVisible: true,
      city: '上海', cityVisible: true,
      salary: '6000/月', salaryVisible: true,
      availability: '一个月内到岗', availabilityVisible: true,
      age: '33', ageVisible: true,
      gender: '男', genderVisible: true,
      currentCity: '上海', currentCityVisible: true,
      experience: '7年经验', experienceVisible: true,
      phone: '15688888886', phoneVisible: true,
      email: 'qmjianli@qq.com', emailVisible: true,
      avatar: '', avatarVisible: true
    }
  },
  {
    id: 'education', type: 'education', title: '教育背景', visible: true,
    data: {
      items: [
        {
          visible: true, schoolVisible: true, majorVisible: true, degreeVisible: true, timeVisible: true, gpaVisible: true, coursesVisible: true,
          school: '全民大学', major: '工商管理', degree: '本科', time: '2012-09 ~ 2016-07',
          gpa: 'GPA 3.66/4',
          courses: '基础会计学、组织行为学、市场营销学'
        }
      ]
    }
  },
  {
    id: 'experience', type: 'experience', title: '工作经历', visible: true,
    data: {
      items: [
        {
          visible: true, companyVisible: true, positionVisible: true, timeVisible: true,
          company: '全民简历科技有限公司', position: '行政专员', time: '2018-09 ~ 至今',
          descriptions: [
            { text: '负责本部的行政人事管理和日常事务', visible: true },
            { text: '编制公司人事管理制度，规避各项风险', visible: true }
          ]
        }
      ]
    }
  },
  {
    id: 'skills', type: 'skills', title: '技能特长', visible: true,
    data: {
      languageText: '大学英语6级证书，能够熟练进行交流。', languageTextVisible: true,
      computerText: '熟练操作各类应用软件，如Word、Excel。', computerTextVisible: true,
      teamText: '具有丰富的项目管理与协调经验。', teamTextVisible: true,
      bars: [
        { visible: true, name: '计算', level: '精通', percent: 90 }
      ]
    }
  }
]

const versions = ref<{id: string, name: string, data: any}[]>([])
const currentVersionId = ref('v1')

const normalizeData = (mods: any[]) => {
  return mods.map(m => {
    if (m.type === 'textList') {
      m.data.items = m.data.items.map((i:any) => typeof i === 'string' ? { visible: true, text: i } : i)
    }
    // Basic module missing flags & robust injection of properties (for cached instances)
    if (m.type === 'basic') {
      ['name','intent','phone','email','age','gender','city','currentCity','experience','salary','availability'].forEach(k => {
        if(m.data[`${k}Visible`] === undefined) m.data[`${k}Visible`] = true
      });
      if (m.data.avatar === undefined) m.data.avatar = '';
      if (m.data.avatarVisible === undefined) m.data.avatarVisible = true;
    }
    if (m.type === 'education') {
      m.data.items.forEach((i:any) => { 
        if (i.visible === undefined) i.visible = true;
        ['school','major','degree','time','gpa','courses'].forEach(k => {
          if (i[`${k}Visible`] === undefined) i[`${k}Visible`] = true
        })
      })
    }
    if (m.type === 'experience' || m.type === 'project') {
      m.data.items.forEach((i:any) => { 
        if (i.visible === undefined) i.visible = true;
        ['company','position','time'].forEach(k => {
          if (i[`${k}Visible`] === undefined) i[`${k}Visible`] = true
        })
        // Migrate string array to object array for fine-grained toggling
        if (Array.isArray(i.descriptions) && typeof i.descriptions[0] === 'string') {
          i.descriptions = i.descriptions.map((desc: string) => ({ text: desc, visible: true }))
        }
      })
    }
    return m
  })
}

const modules = ref<any>(normalizeData(JSON.parse(JSON.stringify(defaultModules))))

const activeModuleId = ref('basic')
const activeModule = computed(() => modules.value.find((m: any) => m.id === activeModuleId.value))
const activeModuleTitle = computed(() => activeModule.value?.title || '')

const getLabel = (key: string) => {
  const map: Record<string, string> = { 
    name:'姓名', intent:'求职意向', phone:'手机号码', email:'电子邮箱', age:'年龄', 
    gender:'性别', city:'目标城市', currentCity:'当前所在', experience:'工作经验', 
    salary:'期望薪资', availability:'到岗情况',
    school:'学校', major:'专业', degree:'学历', time:'时间段', gpa:'成绩',
    company:'公司/项目名', position:'职位/角色', project:'项目', role:'角色',
    languageText: '语言能力', computerText: 'IT 技能', teamText: '综合素质'
  }
  return map[key] || key
}

// Field focus engine! Jump from Preview -> Editor
const switchActive = (id: string) => { activeModuleId.value = id; }

const handleFocusField = (moduleId: string, inputIdIdentifier: string) => {
  switchActive(moduleId); // Route correctly to module pane
  
  if (inputIdIdentifier) {
    nextTick(() => {
      // Give Vue DOM rendering a tiny wait to catch up after mounting inputs inside new v-if pane
      setTimeout(() => {
         const el = document.getElementById(inputIdIdentifier);
         if (el) {
           el.scrollIntoView({ behavior: 'smooth', block: 'center' });
           el.focus();
           
           // Flash the background yellow to alert user precisely where to type
           const isTextarea = el.tagName.toLowerCase() === 'textarea';
           const oldTransition = el.style.transition;
           el.style.transition = 'all 0.5s ease';
           el.classList.add('ring-4', 'ring-blue-400', 'bg-yellow-50');
           setTimeout(() => {
             el.classList.remove('ring-4', 'ring-blue-400', 'bg-yellow-50');
             setTimeout(() => { el.style.transition = oldTransition; }, 500); 
           }, 1000)
         } else {
           document.querySelector('.config-panel')?.scrollTo({ top: 0, behavior: 'smooth' });
         }
      }, 50)
    })
  } else {
    document.querySelector('.config-panel')?.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

const removeItem = (arr: any[], idx: number) => arr.splice(idx, 1)
const addEdu = () => activeModule.value.data.items.push({ visible: true, school: '', major: '', degree: '本科', time: '', courses: '', gpa: '' })
const addExp = (type: string) => activeModule.value.data.items.push({ visible: true, company: '', position: '', time: '', descriptions: [{ text: '', visible: true }] })
const addSkillBar = () => activeModule.value.data.bars.push({ visible: true, name: '新技能', level: '一般', percent: 50 })
const addTextListItem = () => activeModule.value.data.items.push({ visible: true, text: ''})

const triggerAvatarUpload = () => {
  document.getElementById('avatarFileInput')?.click();
}

const uploadAvatar = (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file && activeModule.value?.type === 'basic') {
    const reader = new FileReader()
    reader.onload = (event) => { activeModule.value.data.avatar = event.target?.result as string }
    reader.readAsDataURL(file)
  }
}

// Watch data deep to debounced auto-save 
let saveTimer: any = null
watch(modules, () => {
  saving.value = true
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    const idx = versions.value.findIndex(v => v.id === currentVersionId.value)
    if (idx !== -1) {
      versions.value[idx].data = JSON.parse(JSON.stringify(modules.value))
      localStorage.setItem('resume_versions', JSON.stringify(versions.value))
      saving.value = false
      const d = new Date()
      saveTime.value = `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}:${d.getSeconds().toString().padStart(2,'0')}`
    }
  }, 1000)
}, { deep: true })

onMounted(() => {
  const saved = localStorage.getItem('resume_versions')
  if (saved) {
    versions.value = JSON.parse(saved)
    currentVersionId.value = versions.value[0].id
    modules.value = normalizeData(JSON.parse(JSON.stringify(versions.value[0].data)))
  } else {
    versions.value = [{ id: 'v1', name: '默认简历', data: JSON.parse(JSON.stringify(defaultModules)) }]
    localStorage.setItem('resume_versions', JSON.stringify(versions.value))
  }
})

const switchVersion = () => {
  const target = versions.value.find(v => v.id === currentVersionId.value)
  if (target) modules.value = normalizeData(JSON.parse(JSON.stringify(target.data)))
}
const createNewVersion = () => {
  const id = 'v' + Date.now()
  const newVer = { id, name: `新建草稿 ${versions.value.length + 1}`, data: JSON.parse(JSON.stringify(modules.value)) }
  versions.value.push(newVer)
  currentVersionId.value = id
  localStorage.setItem('resume_versions', JSON.stringify(versions.value))
}
const resetToDefault = () => {
  if(confirm('确认重置为默认数据吗？')) modules.value = normalizeData(JSON.parse(JSON.stringify(defaultModules)))
}

// === Export PDF ===
async function exportPdf() {
  if (!resumeTemplateRef.value?.resumeRef) return

  exporting.value = true
  appStore.showToast('生成中', '如果有两页，将自动识别切分...', 'info')

  const element = resumeTemplateRef.value.resumeRef
  // remove the visual pagination red dashed lines before export physically applying a print class
  element.classList.add('exporting-pdf')  
  
  var opt = {
    margin:       0,
    filename:     `简历-${new Date().getTime()}.pdf`,
    image:        { type: 'jpeg', quality: 1.0 },
    // scale determines how crisp the canvas is. high scale -> crisp
    html2canvas:  { scale: 2, useCORS: true, backgroundColor: '#ffffff' },
    // format [794, 1123] enforces strict precise A4 bounding so multipage works natively by vertical splitting!
    jsPDF:        { unit: 'px', format: [794, 1123], orientation: 'portrait', hotfixes: ["px_scaling"] }
  };

  try {
    await html2pdf().set(opt).from(element).save()
    appStore.showToast('导出完成', '多页 PDF 已合并生成下载', 'success')
  } catch (error: any) {
    appStore.showToast('导出失败', error.message || '生成错误', 'error')
  } finally {
    // replace visual pagination lines afterwards
    element.classList.remove('exporting-pdf') 
    exporting.value = false
  }
}
</script>

<style scoped lang="scss">
.config-panel {
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 4px;
  }
  &::-webkit-scrollbar-thumb:hover {
    background: #94a3b8;
  }
}

/* Custom Tailwind utility replacement logic for Ranges */
.custom-range {
  -webkit-appearance: none;
  background: transparent;
  cursor: pointer;
}
.custom-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  height: 16px;
  width: 16px;
  border-radius: 50%;
  background: #2563eb;
  border: 2px solid white;
  box-shadow: 0 1px 4px rgba(0,0,0,0.4);
  margin-top: -6px;
}
.custom-range::-webkit-slider-runnable-track {
  width: 100%;
  height: 4px;
  background: #e2e8f0;
  border-radius: 2px;
}
</style>
