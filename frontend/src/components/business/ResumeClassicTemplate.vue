<template>
  <div class="resume-wrapper relative bg-white text-dark w-[794px] min-h-[1123px] mx-auto p-[40px] text-[14px] leading-relaxed" 
       ref="resumeRef">
    <template v-for="module in modules" :key="module.id">
      <div v-if="module.visible" 
           class="module-block mb-6 relative group cursor-pointer hover:bg-blue-50/30 transition-colors rounded p-2 -mx-2 -mt-2"
           @click="$emit('select-module', module.id)">
        
        <div class="absolute left-[-20px] top-4 hidden group-hover:block text-blue-500 opacity-50">✎</div>

        <!-- ======================= 基础信息 ======================= -->
        <div v-if="module.type === 'basic'" class="flex items-start justify-between border-b-[2px] border-[#333] pb-4 mb-2">
          <div class="flex-1 text-center pr-[120px]">
            <h1 class="text-3xl font-bold tracking-[0.2em] mb-4">{{ module.data.name || '姓名' }}</h1>
            
            <div class="text-sm text-gray-600 mb-2 flex justify-center items-center flex-wrap" style="gap: 4px 8px;">
              <template v-for="(val, key) in { intent: '求职意向：'+module.data.intent, city: module.data.city, salary: module.data.salary, availability: module.data.availability }" :key="key">
                <span v-if="module.data[key] && module.data[`${key}Visible`] !== false">{{ val }}</span>
                <span class="text-gray-300 last:hidden" v-if="module.data[key] && module.data[`${key}Visible`] !== false">|</span>
              </template>
            </div>
            
            <div class="text-sm text-gray-600 mb-2 flex justify-center items-center flex-wrap" style="gap: 4px 8px;">
              <template v-for="(val, key) in { age: module.data.age+'岁', gender: module.data.gender, currentCity: module.data.currentCity, experience: module.data.experience }" :key="key">
                <span v-if="module.data[key] && module.data[`${key}Visible`] !== false">{{ val }}</span>
                <span class="text-gray-300 last:hidden" v-if="module.data[key] && module.data[`${key}Visible`] !== false">|</span>
              </template>
            </div>
            
            <div class="text-sm text-gray-600 flex justify-center items-center flex-wrap" style="gap: 4px 8px;">
              <template v-for="(val, key) in { phone: module.data.phone, email: module.data.email }" :key="key">
                 <span v-if="module.data[key] && module.data[`${key}Visible`] !== false">{{ val }}</span>
                 <span class="text-gray-300 last:hidden" v-if="module.data[key] && module.data[`${key}Visible`] !== false">|</span>
              </template>
            </div>
          </div>
          <div v-if="module.data.avatarVisible !== false" class="w-[100px] h-[120px] bg-gray-100 overflow-hidden absolute right-[40px] top-[40px] border border-gray-200">
            <img v-if="module.data.avatar" :src="module.data.avatar" alt="Avatar" class="w-full h-full object-cover">
            <div v-else class="w-full h-full flex items-center justify-center text-gray-400 text-sm bg-[#f5f5f5]">照片</div>
          </div>
        </div>

        <!-- ======================= 教育背景 ======================= -->
        <div v-if="module.type === 'education'">
          <h2 class="text-lg font-bold border-b border-[#666] pb-1 mb-4 flex">{{ module.title }}</h2>
          <template v-for="(edu, idx) in module.data.items" :key="idx">
            <div v-if="edu.visible !== false" class="mb-3">
              <div class="flex justify-between font-bold mb-1">
                <span>{{ edu.school }}<span v-if="edu.school && edu.major"> - </span>{{ edu.major }}<span v-if="edu.major && edu.degree"> - </span>{{ edu.degree }}</span>
                <span class="text-gray-600 font-normal">{{ edu.time }}</span>
              </div>
              <div class="text-[13px] text-[#444] mb-1 leading-relaxed" v-if="edu.gpa">专业成绩：{{ edu.gpa }}</div>
              <div class="text-[13px] text-[#444] leading-relaxed" v-if="edu.courses">主修课程：{{ edu.courses }}</div>
            </div>
          </template>
        </div>

        <!-- ======================= 经历类 (工作/项目) ======================= -->
        <div v-if="module.type === 'experience' || module.type === 'project'">
          <h2 class="text-lg font-bold border-b border-[#666] pb-1 mb-4">{{ module.title }}</h2>
          <template v-for="(work, idx) in module.data.items" :key="idx">
            <div v-if="work.visible !== false" class="mb-4">
              <div class="flex justify-between font-bold mb-2">
                <span>{{ work.company }}<span v-if="work.company && work.position"> - </span>{{ work.position }}</span>
                <span class="text-gray-600 font-normal">{{ work.time }}</span>
              </div>
              <ul class="list-disc list-outside pl-4 text-[13px] text-[#444] space-y-[2px]">
                <li v-for="(desc, idx2) in work.descriptions" :key="idx2" class="pl-1">{{ desc }}</li>
              </ul>
            </div>
          </template>
        </div>

        <!-- ======================= 技能特长 ======================= -->
        <div v-if="module.type === 'skills'">
          <h2 class="text-lg font-bold border-b border-[#666] pb-1 mb-4">{{ module.title }}</h2>
          <div class="text-[13px] text-[#444] space-y-[4px] mb-6">
            <div v-if="module.data.languageTextVisible !== false && module.data.languageText"><span class="font-medium">语言能力：</span>{{ module.data.languageText }}</div>
            <div v-if="module.data.computerTextVisible !== false && module.data.computerText"><span class="font-medium">计算机：</span>{{ module.data.computerText }}</div>
            <div v-if="module.data.teamTextVisible !== false && module.data.teamText"><span class="font-medium">团队能力：</span>{{ module.data.teamText }}</div>
          </div>
          
          <!-- 技能条 -->
          <div class="flex items-end gap-16 px-6 flex-wrap" v-if="module.data.bars && module.data.bars.length > 0">
             <template v-for="(bar, idx) in module.data.bars" :key="idx">
               <div v-if="bar.visible !== false" class="flex flex-col items-center">
                   <div class="text-xs text-gray-500 mb-2">{{ bar.level || '进度' }}</div>
                   <div class="w-32 h-[2px] bg-gray-200 relative mb-3">
                     <div class="absolute inset-y-0 left-0 bg-[#333]" :style="{ width: bar.percent + '%' }">
                        <div class="absolute top-1/2 -mt-[4px] right-[-4px] w-[8px] h-[8px] rounded-full bg-white border-2 border-[#333]"></div>
                     </div>
                   </div>
                   <div class="text-[13px] font-medium">{{ bar.name }}</div>
               </div>
             </template>
          </div>
        </div>

        <!-- ======================= 纯列表型 (如荣誉证书) ======================= -->
        <div v-if="module.type === 'textList'">
          <h2 class="text-lg font-bold border-b border-[#666] pb-1 mb-4">{{ module.title }}</h2>
          <ul class="list-disc list-outside pl-4 text-[13px] text-[#444] space-y-[2px]">
            <template v-for="(txt, idx) in module.data.items" :key="idx">
              <li v-if="txt.visible !== false" class="pl-1" :class="txt.visible !== false ? '' : 'hidden'">{{ txt.text || txt }}</li>
            </template>
          </ul>
        </div>

        <!-- ======================= 纯文本型 (如自我评价) ======================= -->
        <div v-if="module.type === 'text'">
          <h2 class="text-lg font-bold border-b border-[#666] pb-1 mb-4">{{ module.title }}</h2>
          <p class="text-[13px] text-[#444] leading-relaxed whitespace-pre-wrap">{{ module.data.content }}</p>
        </div>

      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  modules: any[]
}>()

defineEmits(['select-module'])

const resumeRef = ref<HTMLElement | null>(null)

defineExpose({
  resumeRef
})
</script>

<style scoped>
.resume-wrapper {
  color: #333;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  box-sizing: border-box;
  /* A4 Paginated Visual Effect */
  background-image: repeating-linear-gradient(
    to bottom,
    white,
    white 1120px,
    #ddd 1120px,
    #ddd 1123px
  );
  background-color: white;
}

/* Hide CSS page breaks during screen view, but keep standard */
@media screen {
  .page-break { display: none; }
}

@media print {
  .resume-wrapper {
    box-shadow: none;
    margin: 0;
    padding: 0;
    width: 100%;
    min-height: auto;
    background-image: none !important;
  }
}
</style>
