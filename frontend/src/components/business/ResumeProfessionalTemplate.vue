<template>
  <div class="resume-physical-page bg-white mx-auto shadow-sm" ref="resumeRef">
    <div class="resume-wrapper">
      <div
        class="resume-content flex flex-col pointer-events-auto"
        v-for="module in sortedModules"
        :key="module.id"
      >
        <!-- Basic Info Module (Header) -->
        <div v-if="module.type === 'basic' && module.visible" class="mb-5 pb-2 flex justify-between relative resume-module-block hover:outline-dashed hover:outline-2 hover:outline-blue-400 group cursor-pointer transition-all" @click.stop="$emit('focus-field', module.id, '')">
          <div class="flex-1">
            <div class="w-full text-center flex flex-col items-center mb-4"><h1 class="text-3xl font-normal text-[#3775a6] tracking-widest mb-1">{{ module.data.name }}</h1><div class="text-xs text-gray-500 tracking-wider">努力超越自己，每天进步一点点</div></div><h1 class="hidden">
              <span v-if="module.data.nameVisible !== false">{{ module.data.name }}</span>
              <span v-if="module.data.intentVisible !== false" class="text-lg font-medium text-gray-500 tracking-normal ml-3 px-2 border-l-2 border-gray-300">{{ module.data.intent }}</span>
            </h1>
            <div class="grid grid-cols-2 gap-x-4 gap-y-2 text-sm text-gray-700 mt-3 font-medium flex-1">
               <div v-if="module.data.phoneVisible !== false" class="flex items-center hover:bg-yellow-50 px-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, 'input_basic_phone')">{{ module.data.phone }}</div>
               <div v-if="module.data.emailVisible !== false" class="flex items-center hover:bg-yellow-50 px-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, 'input_basic_email')">{{ module.data.email }}</div>
               <div v-if="module.data.ageVisible !== false" class="flex items-center hover:bg-yellow-50 px-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, 'input_basic_age')">{{ module.data.age }}岁</div>
               <div v-if="module.data.genderVisible !== false" class="flex items-center hover:bg-yellow-50 px-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, 'input_basic_gender')">{{ module.data.gender }}</div>
               <div v-if="module.data.currentCityVisible !== false" class="flex items-center hover:bg-yellow-50 px-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, 'input_basic_currentCity')">现居{{ module.data.currentCity }}</div>
               <div v-if="module.data.experienceVisible !== false" class="flex items-center hover:bg-yellow-50 px-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, 'input_basic_experience')">{{ module.data.experience }}</div>
            </div>
            <div class="flex flex-wrap gap-x-4 gap-y-1 text-sm text-gray-700 mt-1 font-medium">
               <div v-if="module.data.cityVisible !== false" class="flex items-center hover:bg-yellow-50 px-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, 'input_basic_city')">意向城市：{{ module.data.city }}</div>
               <div v-if="module.data.salaryVisible !== false" class="flex items-center hover:bg-yellow-50 px-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, 'input_basic_salary')">期望薪资：{{ module.data.salary }}</div>
               <div v-if="module.data.availabilityVisible !== false" class="flex items-center hover:bg-yellow-50 px-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, 'input_basic_availability')">到岗情况：{{ module.data.availability }}</div>
            </div>
          </div>
          <!-- Avatar rendering logic with event to focus upload zone -->
          <div v-if="module.data.avatarVisible !== false && module.data.avatar" class="w-[90px] w-min-[90px] h-[120px] bg-gray-50 border border-gray-200 overflow-hidden shrink-0 shadow-sm p-1" @click.stop="$emit('focus-field', module.id, 'avatarFileInput')">
             <img :src="module.data.avatar" class="w-full h-full object-cover" />
          </div>
        </div>

        <!-- Generic Template to handle repeatable list sections logic (Education / Experience) -->
        <div v-if="(module.type === 'education' || module.type === 'experience' || module.type === 'project') && module.visible" class="mb-5 relative resume-module-block hover:outline-dashed hover:outline-2 hover:outline-blue-400 group cursor-pointer transition-all" @click.stop="$emit('focus-field', module.id, '')">
          <!-- Section Title -->
          <div class="bg-[#e9f2f9] w-full flex items-stretch mb-4 mt-2 h-7"><h2 class="text-sm font-bold text-white bg-[#3775a6] px-4 flex items-center tracking-wide">{{ module.title }}</h2></div>
          
          <div class="space-y-4">
             <!-- VFor Items inside the section module -->
             <div v-for="(item, idx) in module.data.items" :key="idx" v-show="item.visible !== false">
                <!-- Row Headers (Company/School & Title & Time) -->
                <div class="flex justify-between items-baseline font-bold text-gray-800 text-[14px]">
                  <div class="flex items-center gap-3">
                    <span 
                      v-if="module.type === 'education' ? item.schoolVisible !== false : item.companyVisible !== false"
                      @click.stop="$emit('focus-field', module.id, `input_${module.type==='education'?'edu':'exp'}_${idx}_${module.type === 'education' ? 'school' : 'company'}`)"
                      class="hover:bg-yellow-50 px-1 rounded transition-colors cursor-[text]"
                    >
                      {{ module.type === 'education' ? item.school : item.company }}
                    </span>
                    <span v-if="module.type === 'education' && item.majorVisible !== false" class="text-gray-500 font-medium before:content-[''] before:w-[1px] before:h-[10px] before:bg-gray-300 before:mr-3 before:inline-block hover:bg-yellow-50 px-1 transition-colors rounded cursor-[text]" @click.stop="$emit('focus-field', module.id, `input_edu_${idx}_major`)">
                      {{ item.major }}
                    </span>
                    <span v-if="(module.type === 'experience' || module.type === 'project') && item.positionVisible !== false" class="text-gray-500 font-medium before:content-[''] before:w-[1px] before:h-[10px] before:bg-gray-300 before:mr-3 before:inline-block hover:bg-yellow-50 px-1 transition-colors rounded cursor-[text]" @click.stop="$emit('focus-field', module.id, `input_exp_${idx}_position`)">
                      {{ item.position }}
                    </span>
                  </div>
                  <div class="text-gray-500 font-medium text-[13px] flex items-center gap-3">
                     <span v-if="module.type === 'education' && item.degreeVisible !== false" class="hover:bg-yellow-50 px-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, `input_edu_${idx}_degree`)">{{ item.degree }}</span>
                     <span v-if="module.type === 'education' && item.gpaVisible !== false" class="hover:bg-yellow-50 px-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, `input_edu_${idx}_gpa`)">{{ item.gpa }}</span>
                     <span v-if="item.timeVisible !== false" class="hover:bg-yellow-50 px-1 rounded transition-colors tracking-wide" @click.stop="$emit('focus-field', module.id, `input_${module.type==='education'?'edu':'exp'}_${idx}_time`)">{{ item.time }}</span>
                  </div>
                </div>
                
                <!-- Expanded Descriptions / Courses underneath the header -->
                <div class="mt-1.5 text-[13px] text-gray-700 leading-relaxed text-justify space-y-1">
                   <div v-if="module.type === 'education' && item.coursesVisible !== false" class="hover:bg-yellow-50 p-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, `input_edu_${idx}_courses`)">
                     主修课程：{{ item.courses }}
                   </div>
                   
                   <template v-if="module.type === 'experience' || module.type === 'project'">
                     <ul class="list-none space-y-1 mt-1 ">
                        <template v-for="(desc, dIdx) in item.descriptions" :key="dIdx">
                          <li v-if="desc.visible !== false" class="pl-1 hover:bg-yellow-50 rounded transition-colors" @click.stop="$emit('focus-field', module.id, `input_exp_${idx}_desc_${dIdx}`)">
                             {{ desc.text }}
                          </li>
                        </template>
                     </ul>
                   </template>
                </div>
             </div>
          </div>
        </div>

        <!-- Skills Module -->
        <div v-if="module.type === 'skills' && module.visible" class="mb-5 relative resume-module-block hover:outline-dashed hover:outline-2 hover:outline-blue-400 group cursor-pointer transition-all" @click.stop="$emit('focus-field', module.id, '')">
          <div class="bg-[#e9f2f9] w-full flex items-stretch mb-4 mt-2 h-7"><h2 class="text-sm font-bold text-white bg-[#3775a6] px-4 flex items-center tracking-wide">{{ module.title }}</h2></div>
          <div class="grid grid-cols-2 gap-4 text-[13px] text-gray-700">
            <!-- text properties -->
            <div class="col-span-2 space-y-2">
               <div v-if="module.data.languageTextVisible !== false" class="flex items-start hover:bg-yellow-50 p-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, 'input_skills_languageText')"><span class="font-bold w-[70px] shrink-0 text-gray-800">语言能力：</span> <span class="flex-1">{{ module.data.languageText }}</span></div>
               <div v-if="module.data.computerTextVisible !== false" class="flex items-start hover:bg-yellow-50 p-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, 'input_skills_computerText')"><span class="font-bold w-[70px] shrink-0 text-gray-800">IT 技能：</span> <span class="flex-1">{{ module.data.computerText }}</span></div>
               <div v-if="module.data.teamTextVisible !== false" class="flex items-start hover:bg-yellow-50 p-1 rounded transition-colors" @click.stop="$emit('focus-field', module.id, 'input_skills_teamText')"><span class="font-bold w-[70px] shrink-0 text-gray-800">综合素质：</span> <span class="flex-1">{{ module.data.teamText }}</span></div>
            </div>
            
            <!-- bar properties -->
            <div 
              v-for="(bar, idx) in module.data.bars" 
              :key="idx" 
              v-show="bar.visible !== false"
              class="flex flex-col gap-1 pr-4 bg-white hover:bg-yellow-50 p-1 rounded transition-colors"
              @click.stop="$emit('focus-field', module.id, `input_skills_bar_${idx}_name`)"
            >
              <div class="flex justify-between text-xs font-bold text-gray-600">
                <span>{{ bar.name }} <span class="text-gray-400 font-normal ml-1">({{ bar.level }})</span></span>
              </div>
              <div class="w-full bg-gray-200 rounded-full h-1.5 shadow-inner">
                <div class="bg-gray-800 h-1.5 rounded-full" :style="{ width: `${bar.percent}%` }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Single Paragraph Blob -->
        <div v-if="module.type === 'text' && module.visible" class="mb-5 relative resume-module-block hover:outline-dashed hover:outline-2 hover:outline-blue-400 group cursor-pointer transition-all" @click.stop="$emit('focus-field', module.id, 'input_text_content')">
          <div class="bg-[#e9f2f9] w-full flex items-stretch mb-4 mt-2 h-7"><h2 class="text-sm font-bold text-white bg-[#3775a6] px-4 flex items-center tracking-wide">{{ module.title }}</h2></div>
          <div class="text-[13px] text-gray-700 leading-relaxed whitespace-pre-wrap hover:bg-yellow-50 p-2 rounded transition-colors">
            {{ module.data.content || '请输入正文' }}
          </div>
        </div>
        
        <!-- Text List Module -->
        <div v-if="module.type === 'textList' && module.visible" class="mb-5 relative resume-module-block hover:outline-dashed hover:outline-2 hover:outline-blue-400 group cursor-pointer transition-all" @click.stop="$emit('focus-field', module.id, '')">
          <div class="bg-[#e9f2f9] w-full flex items-stretch mb-4 mt-2 h-7"><h2 class="text-sm font-bold text-white bg-[#3775a6] px-4 flex items-center tracking-wide">{{ module.title }}</h2></div>
          <ul class="list-none text-[13px] text-gray-700 leading-relaxed text-justify space-y-1 ">
            <template v-for="(item, idx) in module.data.items" :key="idx">
              <li v-if="item.visible !== false" class="pl-1 hover:bg-yellow-50 rounded p-1 transition-colors" @click.stop="$emit('focus-field', module.id, `input_textlist_${idx}`)">
                {{ item.text || '...' }}
              </li>
            </template>
          </ul>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineEmits, ref } from 'vue'

const props = defineProps<{
  modules: any[]
}>()

const emit = defineEmits(['focus-field', 'select-module'])

const sortedModules = computed(() => {
  return [...props.modules].filter(m => m.visible)
})

const resumeRef = ref<HTMLElement | null>(null)
defineExpose({ resumeRef })
</script>



<style scoped>
.resume-physical-page {
  /* Enforce Strict A4 Aspect Ratio sizing -> html2pdf parses precise boundaries */
  width: 794px;
  min-height: 1123px;
  /* Make sure container is white to prevent weird overlap bugs */
  background-color: white; 
  /* Visual simulation of multi-page rendering markers strictly spaced to A4 */
  background-image: repeating-linear-gradient(
    to bottom,
    transparent,
    transparent 1122px,
    #ef4444 1122px, /* Tailwind Red 500 for high visibility outline */
    #ef4444 1123px
  );
}

.resume-wrapper {
  padding: 40px 48px; 
}

/* Specific class applied ONLY BEFORE PDF GENERATION to hide guidelines */
.exporting-pdf {
  min-height: auto; 
  background-image: none !important;
}
.exporting-pdf .resume-module-block:hover {
  outline: none !important;
  background-color: transparent !important;
}
</style>
