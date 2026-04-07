<template>
  <div class="admin-dashboard-shell pb-8">
    <div class="mb-8 text-gray-800">
      <h1 class="text-2xl font-bold tracking-tight">违规举报处理</h1>
      <p class="text-gray-500 text-sm mt-1">处理来自社区用户的投诉，裁定举报结果并触发相应惩罚机制。</p>
    </div>

    <!-- 操作台布局 -->
    <div class="flex gap-6">
      
      <!-- 举报记录表格 -->
      <div class="flex-1 flex flex-col bg-white rounded-xl shadow-sm border min-h-[500px] overflow-hidden">
        <div class="px-5 py-3 border-b flex items-center justify-between bg-gray-50 shrink-0">
          <h3 class="font-medium text-gray-700">处理大厅</h3>
        </div>

        <!-- 内部滚动表格区域 -->
        <div class="flex-1 overflow-auto">
          <table class="w-full text-left border-collapse">
            <thead class="sticky top-0 bg-white z-10 shadow-sm border-b">
              <tr class="text-xs text-gray-500">
                <th class="py-3 px-5 font-medium">举报信息</th>
                <th class="py-3 px-4 font-medium">被举报对象</th>
                <th class="py-3 px-4 font-medium w-32">处理进度</th>
              </tr>
            </thead>
            <tbody class="divide-y text-sm">
              <tr v-for="item in reports" :key="item.id" 
                  class="hover:bg-blue-50/30 transition cursor-pointer"
                  :class="selectedReport?.id === item.id ? 'bg-blue-50/50' : ''"
                  @click="selectedReport = item">
                <td class="py-4 px-5">
                  <div class="font-medium text-gray-800 mb-1 flex items-center gap-2">
                    <span class="bg-red-100 text-red-700 text-[10px] px-1.5 py-0.5 rounded">{{ item.reasonType }}</span>
                    {{ item.reasonInfo }}
                  </div>
                  <div class="text-xs text-gray-500">举报人: {{ item.reporterName }} · {{ item.time }}</div>
                </td>
                <td class="py-4 px-4">
                  <div class="font-medium text-gray-700 mb-0.5 truncate max-w-[200px]" :title="item.targetTitle">
                    <span class="i-carbon-document text-gray-400 mr-1"></span>
                    {{ item.targetTitle }}
                  </div>
                  <div class="text-xs text-gray-400">UID: {{ item.targetUserId }} 发布</div>
                </td>
                <td class="py-4 px-4">
                  <span class="inline-flex items-center gap-1 px-2 py-1 rounded text-[11px] font-bold shrink-0"
                        :class="item.status === 'pending' ? 'bg-orange-50 text-orange-600' : 'bg-green-50 text-green-600'">
                    <span class="i-carbon-time" v-if="item.status === 'pending'"></span>
                    <span class="i-carbon-checkmark" v-else></span>
                    {{ item.status === 'pending' ? '待处理' : '已完结' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 右侧案件详情面板 -->
      <div v-if="selectedReport" class="w-[380px] bg-white rounded-xl shadow-sm border flex flex-col shrink-0 min-h-[500px]">
        <div class="px-5 py-4 border-b bg-gray-50 shrink-0">
          <h3 class="font-bold text-gray-800 flex items-center gap-2">
            <span class="i-carbon-task-view text-blue-500"></span> 案件详情
          </h3>
        </div>
        
        <!-- 详情滚动区 -->
        <div class="p-5 flex-1 overflow-y-auto">
          <div class="mb-5">
            <div class="text-xs font-semibold text-gray-400 uppercase mb-2">举报人描述</div>
            <div class="bg-gray-50 p-3 rounded-lg text-sm text-gray-700 border border-gray-100">
              "{{ selectedReport.detailDesc }}"
            </div>
          </div>
          
          <div class="mb-6">
            <div class="text-xs font-semibold text-gray-400 uppercase mb-2">涉事原贴内容快照</div>
            <div class="border rounded-lg p-4 bg-white shadow-sm relative text-gray-800">
              <span class="absolute right-2 top-2 i-carbon-launch text-gray-300 hover:text-blue-500 cursor-pointer" title="查看原贴"></span>
              <h4 class="font-bold mb-2 mt-1">{{ selectedReport.targetTitle }}</h4>
              <p class="text-sm text-gray-600 leading-relaxed">{{ selectedReport.targetContent }}</p>
            </div>
          </div>
          
          <!-- 处理动作栏 -->
          <div v-if="selectedReport.status === 'pending'" class="pt-4 border-t space-y-3">
            <div class="text-xs font-semibold text-gray-800 uppercase mb-2 text-center">判定与下发惩罚</div>
            <button @click="resolve('ban')" class="flex items-center justify-center gap-2 w-full py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm shadow outline-none transition">
              举报成立：封号并下架
            </button>
            <button @click="resolve('delete')" class="flex items-center justify-center gap-2 w-full py-2 bg-orange-100 hover:bg-orange-200 text-orange-700 font-medium rounded-lg text-sm outline-none transition border border-orange-200">
              举报成立：仅下架内容
            </button>
            <button @click="resolve('reject')" class="flex items-center justify-center gap-2 w-full py-2 bg-gray-100 hover:bg-gray-200 text-gray-600 font-medium rounded-lg text-sm outline-none transition">
              举报不成立：直接驳回
            </button>
          </div>
          <!-- 完结状态反馈 -->
          <div v-else class="pt-4 border-t text-center text-sm text-green-600 flex items-center justify-center gap-1.5 font-bold">
            <span class="i-carbon-checkmark-filled text-lg"></span> 此工单已处理完结
          </div>
        </div>
      </div>
      
      <!-- 未选中工单时的空占位 -->
      <div v-else class="w-[380px] bg-gray-50/50 rounded-xl border border-dashed border-gray-300 flex flex-col items-center justify-center text-gray-400 shrink-0 min-h-[500px]">
        <span class="i-carbon-list text-3xl mb-3 text-gray-300"></span>
        <p class="text-sm">在表格中选择一项举报查看关联详情</p>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

interface Report {
  id: number
  reasonType: string
  reasonInfo: string
  reporterName: string
  time: string
  targetTitle: string
  targetUserId: number
  targetContent: string
  detailDesc: string
  status: 'pending' | 'resolved'
}

const selectedReport = ref<Report | null>(null)

const reports = ref<Report[]>([
  {
    id: 2001,
    reasonType: '低俗色情',
    reasonInfo: '包含不良网站推广二维码',
    reporterName: '正义路人甲',
    time: '20分钟前',
    targetTitle: '免费领前端课程包',
    targetUserId: 50402,
    targetContent: '最新的全套前端视频在这里。加群获取',
    detailDesc: '配图很不雅，强烈要求封号。',
    status: 'pending'
  }
])

function resolve(action: string) {
  if (!selectedReport.value) return
  
  if (action === 'ban') {
    appStore.showToast('处理完毕', '该用户已被封禁，内容已下架。')
  } else if (action === 'delete') {
    appStore.showToast('处理完毕', '违规内容已强制下架。')
  } else {
    appStore.showToast('已驳回', '举报不成立，已通知举报人。', 'info')
  }
  
  selectedReport.value.status = 'resolved'
}
</script>
