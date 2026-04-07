<template>
  <div class="admin-dashboard-shell pb-8">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-800 tracking-tight">管理中心仪表盘</h1>
      <p class="text-gray-500 text-sm mt-1">网站数据实时概览与快捷操作入口。</p>
    </div>

    <!-- 顶层核心指标卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
      <div v-for="stat in coreStats" :key="stat.title" class="bg-white rounded-xl shadow-sm border p-5 flex items-center justify-between transition hover:-translate-y-1 hover:shadow-md cursor-default">
        <div>
          <div class="text-gray-500 text-xs font-medium mb-1 uppercase tracking-wider">{{ stat.title }}</div>
          <div class="text-2xl font-bold text-gray-800">{{ stat.value }}</div>
          <div class="mt-1 text-xs" :class="stat.trend > 0 ? 'text-green-500' : 'text-red-500'">
            <span class="inline-block" :class="stat.trend > 0 ? 'i-carbon-arrow-up' : 'i-carbon-arrow-down'"></span> 
            {{ Math.abs(stat.trend) }}% 较昨日
          </div>
        </div>
        <div class="h-10 w-10 min-w-10 rounded-full flex items-center justify-center bg-gray-50 text-gray-500 text-xl" :class="stat.icon"></div>
      </div>
    </div>

    <!-- 中间两列栏目 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      
      <!-- 待办任务流 -->
      <div class="bg-white rounded-xl shadow-sm border flex flex-col col-span-2">
        <div class="px-5 py-4 border-b flex items-center justify-between">
          <h2 class="font-bold text-gray-800 flex items-center gap-2">
            <span class="i-carbon-task text-blue-500 text-lg"></span> 关键待办
          </h2>
          <span class="text-xs bg-red-100 text-red-600 px-2 py-0.5 rounded-full font-medium">3 项紧急</span>
        </div>
        <div class="flex-1 overflow-auto divide-y">
          <div v-for="task in pendingTasks" :key="task.id" class="p-4 hover:bg-gray-50 transition flex items-start gap-4">
            <div class="mt-0.5 flex-shrink-0" :class="task.type === 'report' ? 'text-red-500 i-carbon-warning-alt' : 'text-orange-500 i-carbon-review'"></div>
            <div class="flex-1 min-w-0">
              <div class="font-medium text-sm text-gray-800 mb-0.5 flex items-center gap-2">
                {{ task.title }} 
                <span class="text-[10px] bg-gray-100 text-gray-500 px-1.5 py-0.5 rounded">{{ task.time }}</span>
              </div>
              <p class="text-xs text-gray-500 truncate">{{ task.description }}</p>
            </div>
            <button class="shrink-0 text-sm bg-white border shadow-sm px-3 py-1 rounded text-gray-600 hover:text-blue-600 hover:border-blue-200 transition">
              处理
            </button>
          </div>
        </div>
        <div class="p-3 border-t bg-gray-50 rounded-b-xl text-center">
          <button class="text-xs text-blue-600 hover:underline">查看所有待办</button>
        </div>
      </div>

      <!-- 快捷工具 & 系统状态 -->
      <div class="space-y-6">
        <!-- 快捷操作 -->
        <div class="bg-white rounded-xl shadow-sm border p-5">
          <h2 class="font-bold text-gray-800 mb-4 pb-2 border-b">快捷导航</h2>
          <div class="grid grid-cols-2 gap-3">
            <button class="flex flex-col items-center justify-center p-3 rounded-lg border bg-gray-50 hover:bg-blue-50 hover:border-blue-200 transition group">
              <div class="i-carbon-user-multiple text-xl text-gray-500 group-hover:text-blue-600 mb-1"></div>
              <span class="text-xs font-medium text-gray-600 group-hover:text-blue-700">用户管理</span>
            </button>
            <button class="flex flex-col items-center justify-center p-3 rounded-lg border bg-gray-50 hover:bg-blue-50 hover:border-blue-200 transition group">
              <div class="i-carbon-document-tasks text-xl text-gray-500 group-hover:text-blue-600 mb-1"></div>
              <span class="text-xs font-medium text-gray-600 group-hover:text-blue-700">内容审核</span>
            </button>
            <button class="flex flex-col items-center justify-center p-3 rounded-lg border bg-gray-50 hover:bg-blue-50 hover:border-blue-200 transition group">
              <div class="i-carbon-warning text-xl text-gray-500 group-hover:text-blue-600 mb-1"></div>
              <span class="text-xs font-medium text-gray-600 group-hover:text-blue-700">举报处理</span>
            </button>
            <button class="flex flex-col items-center justify-center p-3 rounded-lg border bg-gray-50 hover:bg-blue-50 hover:border-blue-200 transition group">
              <div class="i-carbon-tag-group text-xl text-gray-500 group-hover:text-blue-600 mb-1"></div>
              <span class="text-xs font-medium text-gray-600 group-hover:text-blue-700">标签分类</span>
            </button>
          </div>
        </div>

        <!-- 服务器状态 -->
        <div class="bg-white rounded-xl shadow-sm border p-5">
          <h2 class="font-bold text-gray-800 mb-4 pb-2 border-b flex items-center justify-between">
            系统状态
            <span class="flex items-center gap-1 text-xs text-green-600 bg-green-50 px-2 py-0.5 rounded-full"><span class="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></span> 运行正常</span>
          </h2>
          <div class="space-y-3">
            <div>
              <div class="flex justify-between text-xs mb-1"><span class="text-gray-500">API QPS</span><span class="font-medium">320 req/s</span></div>
              <div class="w-full bg-gray-100 rounded-full h-1.5"><div class="bg-blue-500 h-1.5 rounded-full" style="width: 45%"></div></div>
            </div>
            <div>
              <div class="flex justify-between text-xs mb-1"><span class="text-gray-500">存储使用</span><span class="font-medium">45 GB / 100 GB</span></div>
              <div class="w-full bg-gray-100 rounded-full h-1.5"><div class="bg-indigo-500 h-1.5 rounded-full" style="width: 45%"></div></div>
            </div>
            <div>
              <div class="flex justify-between text-xs mb-1"><span class="text-gray-500">内存负载</span><span class="font-medium">2.1 GB / 4 GB</span></div>
              <div class="w-full bg-gray-100 rounded-full h-1.5"><div class="bg-orange-500 h-1.5 rounded-full" style="width: 52%"></div></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const coreStats = ref([
  { title: "新增注册用户", value: "2,845", trend: 12.5, icon: "i-carbon-user-follow" },
  { title: "内容发布总数", value: "12,054", trend: 5.2, icon: "i-carbon-document-add" },
  { title: "今日活跃互动", value: "48.2k", trend: 18.1, icon: "i-carbon-chat" },
  { title: "待处理举报", value: "32", trend: -1.5, icon: "i-carbon-warning-alt" },
])

const pendingTasks = ref([
  { id: 1, type: 'report', title: '高频侵权举报', description: '有用户连续举报"2025架构师全景"资料抄袭，需介入裁定。', time: '10分钟前' },
  { id: 2, type: 'review', title: '敏感词风控拦截', description: '系统拦截了来自UID(10084)的疑似推广引流文章，请人工复核。', time: '1小时前' },
  { id: 3, type: 'review', title: '批量发帖异常', description: '检测到单一IP在短时间内发布了大量低质路线图，疑似脚本。', time: '2小时前' },
  { id: 4, type: 'report', title: '基础资料分类错误', description: '有多名用户反馈该"前端入门"资料被错误归类为"后端"。', time: '今天 09:30' },
])
</script>

<style scoped>
.admin-dashboard-shell {
  max-width: 1200px;
  margin: 0 auto;
}
</style>
